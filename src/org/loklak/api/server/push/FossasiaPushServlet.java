/**
 * FossasiaPushServlet
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

import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class FossasiaPushServlet extends HttpServlet {

    @SuppressWarnings("unchecked")
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
        ProcessingReport report = validator.validate(new String(jsonText), JsonValidator.JsonSchemaEnum.FOSSASIA);
        if (report.getLogLevel() == LogLevel.ERROR || report.getLogLevel() == LogLevel.FATAL) {
            response.sendError(400, "json does not conform to FOSSIA API schema https://github.com/fossasia/api.fossasia.net/blob/master/specs/1.0.1.json");
            return;
        }

        String id;
        try {
            id = computeId(map);
        } catch (Exception e) {
            response.sendError(400, "Error computing id : " + e.getMessage());
            return;
        }

        // conversion phase
        JsonFieldConverter converter = new JsonFieldConverter();
        map = converter.convert(map, JsonFieldConverter.JsonConversionSchemaEnum.FOSSASIA);

        // save to elastic
        int recordCount = 0, newCount = 0, knownCount = 0;
        map.put("id_str", id);
        map.put("text", "");
        MessageEntry messageEntry = new MessageEntry(map);
        Map<String, Object> user = (Map<String, Object>) map.remove("user");
        UserEntry userEntry = new UserEntry((user != null && user.get("screen_name") != null) ? user : new HashMap<String, Object>());
        boolean successful = DAO.writeMessage(messageEntry, userEntry, true, false);
        if (successful) newCount++;
        else knownCount++;
        recordCount++;

        post.setResponse(response, "application/javascript");

        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", "ok");
        json.field("records", recordCount);
        json.field("new", newCount);
        json.field("known", knownCount);
        json.field("message", "pushed");
        json.endObject(); // of root

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
        sos.print(messageEntry.toString());
        sos.print(userEntry.toString());
        DAO.log(request.getServletPath() + " -> records = " + recordCount + ", new = " + newCount + ", known = " + knownCount + ", from host hash " + remoteHash);

    }

    private static String computeId(Map<String, Object> object) throws Exception {
        /*
        Object mtime_obj = object.get("mtime");
        if (mtime_obj == null) {
            throw new Exception("fossasia api format error : member 'mtime' required");
        }
        DateTime mtime = new DateTime((String) mtime_obj);
        */
        Map<String, Object> location = (Map) object.get("location");

        Double longitude = location.get("lat") instanceof Integer ? ((Integer) location.get("lat")).doubleValue() : (Double) location.get("lat");
        Double latitude = location.get("lon") instanceof Integer ? ((Integer) location.get("lon")).doubleValue() : (Double) location.get("lon");

        // longitude and latitude are added to id to a precision of 3 digits after comma
        Long id = (long) Math.floor(1000 * longitude) + (long) Math.floor(1000 * latitude) /*+ mtime.getMillis()*/;
        return id.toString();
    }
}