/**
 * OpenWifiMapPushServlet
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

package org.loklak.api.server;

import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.client.ClientConnection;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenWifiMapPushServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));

        // manage DoS
        if (post.isDoS_blackout()) {
            response.sendError(503, "your request frequency is too high");
            return;
        }

        String url = post.get("url", "");
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        if (url == null || url.length() == 0) {
            response.sendError(400, "your request does not contain an url to your data object");
            return;
        }

        Map<String, Object> map;
        byte[] jsonText;
        try {
            jsonText = ClientConnection.download(url);
            XContentParser parser = JsonXContent.jsonXContent.createParser(jsonText);
            map = parser.map();
        } catch (Exception e) {
            response.sendError(400, "error reading json file from url");
            return;
        }

        // validation phase
        JsonValidator validator = new JsonValidator();
        ProcessingReport report = validator.validate(new String(jsonText), JsonValidator.JsonSchemaEnum.OPENWIFIMAP);
        if (report.getLogLevel() == LogLevel.ERROR || report.getLogLevel() == LogLevel.FATAL) {
            response.sendError(400, "json does not conform to OpenWifiMap API schema https://github.com/freifunk/openwifimap-api#api");
            return;
        }

        // conversion phase
        JsonFieldConverter converter = new JsonFieldConverter();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) map.get("rows");
        rows = converter.convert(rows, JsonFieldConverter.JsonConversionSchemaEnum.OPENWIFIMAP);

        // save to elastic
        int recordCount = 0, newCount = 0, knownCount = 0;
        for (Map<String, Object> row : rows) {
            String id;
            try {
                id = computeId(row);
            } catch (Exception e) {
                response.sendError(400, "Error computing id : " + e.getMessage());
                return;
            }
            row.put("id_str", id);
            row.put("text", "");
            row.put("source_type", SourceType.OPENWIFIMAP.name());
            Map<String, Object> user = (Map<String, Object>) row.remove("user");
            MessageEntry messageEntry = new MessageEntry(row);
            UserEntry userEntry = new UserEntry((user != null && user.get("screen_name") != null) ? user : new HashMap<String, Object>());
            boolean successful = DAO.writeMessage(messageEntry, userEntry, true, false);
            if (successful) newCount++;
            else knownCount++;
            recordCount++;
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
        json.endObject();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
        DAO.log(request.getServletPath() + " -> records = " + recordCount + ", new = " + newCount + ", known = " + knownCount + ", from host hash " + remoteHash);
    }

    private static String computeId(Map<String, Object> object) throws Exception {
        String id = (String) object.get("id");
        boolean hasId = id != null && id.equals("");

        List<Object> location = (List<Object>) object.get("latlng");
        String mtime = (String) object.get("mtime");
        Object rawLon = location.get(1);
        String longitude = rawLon instanceof Integer ? Integer.toString((Integer) rawLon) : Double.toString((Double) rawLon);
        Object rawLat = location.get(0);
        String latitude = rawLat instanceof Integer ? Integer.toString((Integer) rawLat) : Double.toString((Double) rawLat);

        return SourceType.OPENWIFIMAP.name() + (hasId ? "_" + id : "") + "_" + longitude + "_" + latitude + "_" + mtime;
    }
}
