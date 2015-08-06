/**
 * GeoJsonPushServlet
 * Copyright 09.04.2015 by Dang Hai An, @zyzo
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.server.push;

import org.elasticsearch.common.joda.time.DateTime;
import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.data.ProviderType;
import org.loklak.geo.LocationSource;
import org.loklak.geo.PlaceContext;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
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
        String map_type = post.get("map_type", "");
        String source_type_str = post.get("source_type", "");
        if ("".equals(source_type_str) || !SourceType.hasValue(source_type_str)) {
            DAO.log("invalid or missing source_type value : " + source_type_str);
            source_type_str = SourceType.IMPORT.name();
        }
        SourceType sourceType = SourceType.valueOf(source_type_str);

        if (url == null || url.length() == 0) {response.sendError(400, "your request does not contain an url to your data object"); return;}

        // parse json retrieved from url
        final List<Map<String, Object>> features;
        byte[] jsonText;
        try {
            jsonText = ClientConnection.download(url);
            Map<String, Object> map = DAO.jsonMapper.readValue(jsonText, DAO.jsonTypeRef);
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
        if (!"".equals(map_type)) {
            try {
                String[] mapRulesArray = map_type.split(",");
                for (String rule : mapRulesArray) {
                    String[] splitted = rule.split(":", 2);
                    if (splitted.length != 2) {
                        throw new Exception("Invalid format");
                    }
                    List<String> valuesList = mapRules.get(splitted[0]);
                    if (valuesList == null) {
                        valuesList = new ArrayList<>();
                        mapRules.put(splitted[0], valuesList);
                    }
                    valuesList.add(splitted[1]);
                }
            } catch (Exception e) {
                response.sendError(400, "error parsing map_type : " + map_type + ". Please check its format");
                return;
            }
        }

        List<Map<String, Object>> rawMessages = new ArrayList<>();

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

            if (!"".equals(sourceType)) {
                properties.put("source_type", sourceType.name());
            } else {
                properties.put("source_type", SourceType.IMPORT);
            }
            properties.put("provider_type", ProviderType.GEOJSON.name());
            properties.put("provider_hash", remoteHash);
            properties.put("location_point", geometry.get("coordinates"));
            properties.put("location_mark", geometry.get("coordinates"));
            properties.put("location_source", LocationSource.USER.name());
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
            rawMessages.add(properties);
        }

        PushReport report = PushServletHelper.saveMessagesAndImportProfile(rawMessages, Arrays.hashCode(jsonText), post, sourceType);

        String res = PushServletHelper.buildJSONResponse(post.get("callback", ""), report);
        post.setResponse(response, "application/javascript");
        response.getOutputStream().println(res);
        DAO.log(request.getServletPath()
                + " -> records = " + report.getRecordCount()
                + ", new = " + report.getNewCount()
                + ", known = " + report.getKnownCount()
                + ", error = " + report.getErrorCount()
                + ", from host hash " + remoteHash);
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
            Map.Entry<String, Object> pair = it.next();
            String key = pair.getKey();
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
        DateTime mtime = new DateTime(mtime_obj);

        List<?> coords = (List<?>) geometry.get("coordinates");

        Double longitude = coords.get(0) instanceof Integer ? ((Integer) coords.get(0)).doubleValue() : (Double) coords.get(0);
        Double latitude = coords.get(1) instanceof Integer ? ((Integer) coords.get(1)).doubleValue() : (Double) coords.get(1);

        // longitude and latitude are added to id to a precision of 3 digits after comma
        Long id = (long) Math.floor(1000*longitude) + (long) Math.floor(1000*latitude) + mtime.getMillis();
        return id.toString();
    }
}
