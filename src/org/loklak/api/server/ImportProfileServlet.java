/**
 *  ImportProfileServlet
 *  Copyright 24.07.2015 by Dang Hai An, @zyzo
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.data.DAO;
import org.loklak.data.ImportProfileEntry;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class ImportProfileServlet extends HttpServlet {

    private static final long serialVersionUID = -2577184683765091648L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);

        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        // parameters
        String callback = post.get("callback", "");
        boolean minified = post.get("minified", false);
        boolean update = "update".equals(post.get("action", ""));
        boolean jsonp = callback != null && callback.length() > 0;

        if (update) {
            String data = post.get("data", "");
            if (data == null || data.length() == 0) {
                response.sendError(400, "your request does not contain a data object.");
                return;
            }

            // parse the json data
            boolean success;

            try {
                XContentParser parser = JsonXContent.jsonXContent.createParser(data);
                Map<String, Object> map = parser.map();
                if (map.get("id_str") == null) {
                    throw new IOException("id_str field missing");
                }
                ImportProfileEntry i = DAO.SearchLocalImportProfiles((String) map.get("id_str"));
                if (i == null) {
                    throw new IOException("import profile id_str field '" + map.get("id_str") + "' not found");
                }
                ImportProfileEntry importProfileEntry = new ImportProfileEntry(map);
                importProfileEntry.setLastModified(new Date());
                success = DAO.writeImportProfile(importProfileEntry, true);
            } catch (IOException | NullPointerException e) {
                response.sendError(400, "submitted data is not well-formed: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            ServletOutputStream sos = response.getOutputStream();
            if (success) {
                sos.println("Update successfully");
                return;
            } else {
                response.sendError(400, "Unable to update");
                return;
            }
        }

        String source_type = post.get("source_type", "");
        // source_type either has to be null a a valid SourceType value
        if (!"".equals(source_type) && !SourceType.hasValue(source_type)) {
            response.sendError(400, "your request does not contain a valid source_type parameter.");
            return;
        }

        List<ImportProfileEntry> entries = DAO.SearchLocalImportProfilesBySourceType(source_type);
        List<Map<String, Object>> entries_to_map = new ArrayList<>();
        for (ImportProfileEntry entry : entries) {
            entries_to_map.add(entry.toMap());
        }
        post.setResponse(response, "application/javascript");

        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("count", entries.size());
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);
        m.put("profiles", entries_to_map);

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print((minified ? new ObjectMapper().writer() : new ObjectMapper().writerWithDefaultPrettyPrinter()).writeValueAsString(m));
        if (jsonp) sos.println(");");
        sos.println();
    }
}
