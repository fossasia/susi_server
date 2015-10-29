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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.data.DAO;
import org.loklak.data.Timeline;
import org.loklak.data.ImportProfileEntry;
import org.loklak.harvester.SourceType;
import org.loklak.http.RemoteAccess;

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
import java.util.Collection;
import java.util.HashMap;

public class ImportProfileServlet extends HttpServlet {

    private static final long serialVersionUID = -2577184683765091648L;

    private static final String UPDATE_ACTION = "update";
    private static final String DELETE_ACTION = "delete";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);

        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        String action = post.get("action", "");

        switch (action) {
        case UPDATE_ACTION:
            doUpdate(post, response);
            return;
        case DELETE_ACTION:
            doDelete(post, response);
            return;
        case "":
            doSearch(post, response);
            return;
        default:
            response.sendError(400, "invalid 'action' value : " + action);
            return;
        }
    }

    private void doUpdate(RemoteAccess.Post post, HttpServletResponse response) throws IOException {
        String callback = post.get("callback", null);
        boolean jsonp = callback != null && callback.length() > 0;
        String data = post.get("data", "");
        if (data == null || data.length() == 0) {
            response.sendError(400, "your request must contain a data object.");
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
            if (map.get("source_type") == null) {
                throw new IOException("source_type field missing");
            }
            ImportProfileEntry i = DAO.SearchLocalImportProfiles((String) map.get("id_str"));
            if (i == null) {
                throw new IOException("import profile id_str field '" + map.get("id_str") + "' not found");
            }
            ImportProfileEntry importProfileEntry;
            try {
                importProfileEntry = new ImportProfileEntry(map);
            } catch (IllegalArgumentException e) {
                response.sendError(400, "Error updating import profile : " + e.getMessage());
                return;
            }
            importProfileEntry.setLastModified(new Date());
            success = DAO.writeImportProfile(importProfileEntry, true);
        } catch (IOException | NullPointerException e) {
            response.sendError(400, "submitted data is invalid : " + e.getMessage());
            e.printStackTrace();
            return;
        }

        post.setResponse(response, "application/javascript");
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", "ok");
        json.field("records", success ? 1 : 0);
        json.field("message", "updated");
        json.endObject();
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }

    private void doDelete(RemoteAccess.Post post, HttpServletResponse response) throws IOException {

        String callback = post.get("callback", null);
        boolean jsonp = callback != null && callback.length() > 0;

        String id = post.get("id_str", "");
        if ("".equals(id)) {
            response.sendError(400, "your request must contain a `id_str` parameter.");
            return;
        }

        String screen_name = post.get("screen_name", "");
        if ("".equals(screen_name)) {
            response.sendError(400, "your request must contain a `screen_name` parameter.");
            return;
        }

        ImportProfileEntry entry = DAO.SearchLocalImportProfiles(id);
        List<String> sharers = entry.getSharers();
        boolean sharerExists = sharers.remove(screen_name);
        entry.setSharers(sharers);
        boolean successful = false;
        if (sharerExists && DAO.writeImportProfile(entry, true)) {
            successful = true;
        } else {
            throw new IOException("Unable to delete import profile : " + entry.getId());
        }
        post.setResponse(response, "application/javascript");
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", "ok");
        json.field("records", sharerExists && successful ? 1 : 0);
        json.field("message", "deleted");
        json.endObject();
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
    }


    private void doSearch(RemoteAccess.Post post, HttpServletResponse response) throws IOException {
        String callback = post.get("callback", "");
        boolean minified = post.get("minified", false);
        boolean jsonp = callback != null && callback.length() > 0;
        String source_type = post.get("source_type", "");
        String screen_name = post.get("screen_name", "");
        String msg_id = post.get("msg_id", "");
        String detailed = post.get("detailed", "");
        // source_type either has to be null a a valid SourceType value
        if (!"".equals(source_type) && !SourceType.hasValue(source_type)) {
            response.sendError(400, "your request must contain a valid source_type parameter.");
            return;
        }
        Map<String, String> searchConstraints = new HashMap<>();
        if (!"".equals(source_type)) {
            searchConstraints.put("source_type", source_type);
        }
        if (!"".equals(screen_name)) {
            searchConstraints.put("sharers", screen_name);
        }
        if (!"".equals(msg_id)) {
            searchConstraints.put("imported", msg_id);
        }
        Collection<ImportProfileEntry> entries = DAO.SearchLocalImportProfilesWithConstraints(searchConstraints, true);
        List<Map<String, Object>> entries_to_map = new ArrayList<>();
        for (ImportProfileEntry entry : entries) {
            Map<String, Object> entry_to_map = entry.toMap();
            if ("true".equals(detailed)) {
                String query = "";
                for (String msgId : entry.getImported()) {
                    query += "id:" + msgId + " ";
                }
                DAO.SearchLocalMessages search = new DAO.SearchLocalMessages(query, Timeline.Order.CREATED_AT, 0, 1000, 0);
                entry_to_map.put("imported", search.timeline.toMap(false).get("statuses"));
            }
            entries_to_map.add(entry_to_map);
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
