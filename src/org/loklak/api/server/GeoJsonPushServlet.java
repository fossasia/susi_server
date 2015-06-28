package org.loklak.api.server;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.ProviderType;
import org.loklak.data.UserEntry;
import org.loklak.harvester.SourceType;
import org.loklak.tools.UTF8;

import twitter4j.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GeoJsonPushServlet extends HttpServlet {

    private static final long serialVersionUID = -6348695722639858781L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(400, "your must call this with HTTP POST");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);

        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        Map<String, byte[]> m = RemoteAccess.getPostMap(request);
        String url = UTF8.String(m.get("url"));
        String mapType = UTF8.String(m.get("mapType"));
        String callback = UTF8.String(m.get("callback"));
        String sourceType = UTF8.String(m.get("sourceType"));
        boolean jsonp = callback != null && callback.length() > 0;
        if (url == null || url.length() == 0) {response.sendError(400, "your request does not contain an url to your data object"); return;}

        // parse json retrieved from url
        final List<Map<String, Object>> features;
        try {
            String jsonText = readJsonFromUrl(url);
            XContentParser parser = JsonXContent.jsonXContent.createParser(jsonText);
            Map<String, Object> map = parser == null ? null : parser.map();
            Object features_obj = map.get("features");
            features = features_obj instanceof List<?> ? (List<Map<String, Object>>) features_obj : null;
        } catch (Exception e) {
            response.sendError(400, "error reading json file from url");
            return;
        }
        
        // parse maptype
        Map<String, String> mapRules = new HashMap<>();
        if (!"".equals(mapType)) {
            try {
                String[] mapRulesArray = mapType.split(",");
                for (String rule : mapRulesArray) {
                    String[] splitted = rule.split(":", 2);
                    if (splitted.length != 2) {
                        throw new Exception("Invalid format");
                    }
                    mapRules.put(splitted[0], splitted[1]);
                }
            } catch (Exception e) {
                response.sendError(400, "error parsing maptype : " + mapType + ". Please check its format" + e.getMessage());
                return;
            }
        }

        // save results
        if (features != null) {
            for (Map<String, Object> feature : features) {
                Object properties_obj = feature.get("properties");
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = properties_obj instanceof Map<?, ?> ? (Map<String, Object>) properties_obj : null;
                for (String propertyKey : properties.keySet()) {
                    if (mapRules.containsKey(propertyKey)) {
                        Object obj = properties.remove(propertyKey);
                        properties.put(mapRules.get(propertyKey), obj);
                    }
                }

                properties.put("provider_type", ProviderType.GEOJSON.name());
                properties.put("source_type", ("".equals(sourceType) ? SourceType.IMPORT.name() : sourceType));

                // avoid error text not found. TODO: a better strategy, e.g. require text as a mandatory field
                if (properties.get("text") == null) {
                    properties.put("text", "") ;
                }
                // compute unique message id among geojson messages
                try {
                    //response.getWriter().println((String) properties.get("name") + " : " + computeGeoJsonId((feature)));
                    properties.put("id_str", computeGeoJsonId(feature));
                } catch (Exception e) {
                    response.sendError(400, "Error computing id : " + e.getMessage());
                    return;
                }
                MessageEntry msg = new MessageEntry(properties);
                DAO.writeMessage(msg, new UserEntry(new HashMap<String, Object>()), true, true);
            }
        }
    }

    private static String computeGeoJsonId(Map<String, Object> feature) throws Exception {
        Object properties_obj = feature.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = properties_obj instanceof Map<?, ?> ? (Map<String, Object>) properties_obj : null;
        Object geometry_obj = feature.get("geometry");
        @SuppressWarnings("unchecked")
        Map<String, Object> geometry = geometry_obj instanceof Map<?, ?> ? (Map<String, Object>) geometry_obj : null;
        String geometryType = (String) geometry.get("type");
        if (!"Point".equals(geometryType)) {
            throw new Exception("Geometry object unsupported : " + geometryType);
        }
        Object mtime_obj = properties.get("mtime");
        if (mtime_obj == null) {
            throw new Exception("Member with name 'mtime' required in feature properties");
        }
        DateTime mtime = new DateTime((String) mtime_obj);

        List<?> coords = (List<?>) geometry.get("coordinates");

        Double longitude = coords.get(0) instanceof Integer ? ((Integer) coords.get(0)).doubleValue() : (Double) coords.get(0);
        Double latitude = coords.get(1) instanceof Integer ? ((Integer) coords.get(1)).doubleValue() : (Double) coords.get(1);

        // longitude and latitude are added to id to a precision of 3 digits after comma
        Long id = (long) Math.floor(1000*longitude) + (long) Math.floor(1000*latitude) + mtime.getMillis();
        System.out.println((long) Math.floor(1000*longitude) + ", " + Math.floor(1000 * latitude) + ", " + mtime.getMillis() + ", " + id);
        return id.toString();
    }

    private static String readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            return jsonText;
        } finally {
            is.close();
        }
    }
}