/**
 *  Campaigns
 *  Copyright 09.04.2015 by Dang Hai An, @zyzo
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.server;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.client.ClientConnection;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.ProviderType;
import org.loklak.data.UserEntry;
import org.loklak.data.ImportProfileEntry;
import org.loklak.geo.LocationSource;
import org.loklak.geo.PlaceContext;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

/*
 * Test URLs:
 * http://localhost:9000/api/push/geojson.json?url=http://www.paris-streetart.com/test/map.geojson
 * http://localhost:9000/api/push/geojson.json?url=http://api.fossasia.net/map/ffGeoJsonp.php
 * http://localhost:9000/api/push/geojson.json?url=http://cmap-fossasia-api.herokuapp.com/ffGeoJsonp.php&map_type=shortname:screen_name,shortname:user.screen_name&source_type=FOSSASIA_API
 */

public class GeoJsonPushServlet extends HttpServlet {

    private static final long serialVersionUID = -6348695722639858781L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));

        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        String url = post.get("url", "");
        String mapType = post.get("map_type", "");
        String sourceType = post.get("source_type", "");
        if ("".equals(sourceType) || !SourceType.hasValue(sourceType)) {
            DAO.log("invalid or missing source_type value : " + sourceType);
            sourceType = SourceType.IMPORT.name();
        }
        String screenName = post.get("screen_name", "");
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;

        if (url == null || url.length() == 0) {response.sendError(400, "your request does not contain an url to your data object"); return;}

        // parse json retrieved from url
        final List<Map<String, Object>> features;
        try {
            byte[] jsonText = ClientConnection.download(url);
            XContentParser parser = JsonXContent.jsonXContent.createParser(jsonText);
            Map<String, Object> map = parser == null ? null : parser.map();
            Object features_obj = map.get("features");
            features = features_obj instanceof List<?> ? (List<Map<String, Object>>) features_obj : null;
        } catch (Exception e) {
            response.sendError(400, "error reading json file from url");
            return;
        }
        if (features == null) {
            response.sendError(400, "geojson format error : member 'features' missing.");
            return;
        }

        // parse maptype
        Map<String, List<String>> mapRules = new HashMap<>();
        if (!"".equals(mapType)) {
            try {
                String[] mapRulesArray = mapType.split(",");
                for (String rule : mapRulesArray) {
                    String[] splitted = rule.split(":", 2);
                    if (splitted.length != 2) {
                        throw new Exception("Invalid format");
                    }
                    List<String> valuesList = mapRules.get(splitted[0]);
                    if (valuesList == null) {
                        valuesList = new ArrayList<String>();
                        mapRules.put(splitted[0], valuesList);
                    }
                    valuesList.add(splitted[1]);
                }
            } catch (Exception e) {
                response.sendError(400, "error parsing map_type : " + mapType + ". Please check its format");
                return;
            }
        }

        int recordCount = 0, newCount = 0, knownCount = 0;
        List<String> importedMsgIds = new ArrayList<>();
        for (Map<String, Object> feature : features) {
            Object properties_obj = feature.get("properties");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = properties_obj instanceof Map<?, ?> ? (Map<String, Object>) properties_obj : null;
            Object geometry_obj = feature.get("geometry");
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = geometry_obj instanceof Map<?, ?> ? (Map<String, Object>) geometry_obj : null;

            if (properties == null) {
                properties = new HashMap<>();
            }
            if (geometry == null) {
                geometry = new HashMap<>();
            }

            // add mapped properties
            Map<String, Object> mappedProperties = convertMapRulesProperties(mapRules, properties);
            properties.putAll(mappedProperties);

            properties.put("source_type", sourceType);
            properties.put("provider_type", ProviderType.GEOJSON.name());
            properties.put("provider_hash", remoteHash);
            properties.put("location_point", geometry.get("coordinates"));
            properties.put("location_source", LocationSource.REPORT.name());
            properties.put("place_context", PlaceContext.FROM.name());

            // avoid error text not found. TODO: a better strategy, e.g. require text as a mandatory field
            if (properties.get("text") == null) {
                properties.put("text", "");
            }

            // compute unique message id among all messages
            String id_str;
            try {
                id_str = computeGeoJsonId(properties, geometry);
                properties.put("id_str", id_str);
                // response.getWriter().println(properties.get("shortname") + ", " + properties.get("screen_name") + ", " + properties.get("name") + " : " + computeGeoJsonId((feature)));
            } catch (Exception e) {
                response.sendError(400, "Error computing id : " + e.getMessage());
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) properties.remove("user");
            MessageEntry messageEntry = new MessageEntry(properties);
            // uncomment this causes NoShardAvailableException
            UserEntry userEntry = new UserEntry((user != null && user.get("screen_name") != null) ? user : new HashMap<String, Object>());
            boolean successful = DAO.writeMessage(messageEntry, userEntry, true, false);
            if (successful) {
                newCount++;
                importedMsgIds.add(id_str);
            } else {
                knownCount++;
            }
            recordCount++;
        }

        ImportProfileEntry importProfileEntry = null;
        if (newCount > 0 ) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("client_host", post.getClientHost());
            profile.put("imported", importedMsgIds);
            if (!"".equals(screenName)) {
                profile.put("screen_name", screenName);
            }
            profile.put("source_url", url);
            profile.put("source_type", sourceType);
            // placholders
            profile.put("harvesting_freq", Integer.MAX_VALUE);
            profile.put("lifetime", Integer.MAX_VALUE);
            importProfileEntry = new ImportProfileEntry(profile);
            DAO.writeImportProfile(importProfileEntry, true);
        }

        post.setResponse(response, "application/javascript");

        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", "ok");
        json.field("records", recordCount);
        json.field("new", newCount);
        json.field("known", knownCount);
        json.field("message", "pushed");
        if (importProfileEntry != null)
            json.field("importProfile", importProfileEntry.toMap());
        json.endObject(); // of root

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();

        DAO.log(request.getServletPath() + " -> records = " + recordCount + ", new = " + newCount + ", known = " + knownCount + ", from host hash " + remoteHash);

    }

    /**
     * For each member m in properties, if it exists in mapRules, perform these conversions :
     *   - m:c -> keep value, change key m to c
     *   - m:c.d -> insert/update json object of key c with a value {d : value}
     * @param mapRules
     * @param properties
     * @return mappedProperties
     */
    private Map<String, Object> convertMapRulesProperties(Map<String, List<String>> mapRules, Map<String, Object> properties) {
        Map<String, Object> root = new HashMap<>();
        Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
            String key = (String) pair.getKey();
            if (mapRules.containsKey(key)) {
                for (String newField : mapRules.get(key)) {
                    if (newField.contains(".")) {
                        String[] deepFields = newField.split(Pattern.quote("."));
                        Map<String, Object> currentLevel = root;
                        for (int lvl = 0; lvl < deepFields.length; lvl++) {
                            if (lvl == deepFields.length - 1) {
                                currentLevel.put(deepFields[lvl], pair.getValue());
                            } else {
                                if (currentLevel.get(deepFields[lvl]) == null) {
                                    Map<String, Object> tmp = new HashMap<>();
                                    currentLevel.put(deepFields[lvl], tmp);
                                }
                                currentLevel = (Map<String, Object>) currentLevel.get(deepFields[lvl]);
                            }
                        }
                    } else {
                        root.put(newField, pair.getValue());
                    }
                }
            }
        }
        return root;
    }

    private static String computeGeoJsonId(Map<String, Object> properties, Map<String, Object> geometry) throws Exception {
        String geometryType = (String) geometry.get("type");
        if (!"Point".equals(geometryType)) {
            throw new Exception("Geometry object unsupported : " + geometryType);
        }
        Object mtime_obj = properties.get("mtime");
        if (mtime_obj == null) {
            throw new Exception("geojson format error : member 'mtime' required in feature properties");
        }
        DateTime mtime = new DateTime((String) mtime_obj);

        List<?> coords = (List<?>) geometry.get("coordinates");

        Double longitude = coords.get(0) instanceof Integer ? ((Integer) coords.get(0)).doubleValue() : (Double) coords.get(0);
        Double latitude = coords.get(1) instanceof Integer ? ((Integer) coords.get(1)).doubleValue() : (Double) coords.get(1);

        // longitude and latitude are added to id to a precision of 3 digits after comma
        Long id = (long) Math.floor(1000*longitude) + (long) Math.floor(1000*latitude) + mtime.getMillis();
        return id.toString();
    }
}
