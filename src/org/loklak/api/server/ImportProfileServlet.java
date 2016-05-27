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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.ImportProfileEntry;
import org.loklak.objects.SourceType;
import org.loklak.objects.Timeline;
import org.loklak.server.Query;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
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
        Query post = RemoteAccess.evaluate(request);

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

    private void doUpdate(Query post, HttpServletResponse response) throws IOException {
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
            JSONObject json = null;
            try {json = new JSONObject(data);} catch (JSONException e) {}
            if (json == null) {
                throw new IOException("cannot parse json");
            }
            if (!json.has("id_str")) {
                throw new IOException("id_str field missing");
            }
            if (!json.has("source_type")) {
                throw new IOException("source_type field missing");
            }
            ImportProfileEntry i = DAO.SearchLocalImportProfiles(json.getString("id_str"));
            if (i == null) {
                throw new IOException("import profile id_str field '" + json.getString("id_str") + "' not found");
            }
            ImportProfileEntry importProfileEntry;
            try {
                importProfileEntry = new ImportProfileEntry(json);
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
        JSONObject json = new JSONObject(true);
        json.put("status", "ok");
        json.put("records", success ? 1 : 0);
        json.put("message", "updated");
        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString(2));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }

    private void doDelete(Query post, HttpServletResponse response) throws IOException {

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
        JSONObject json = new JSONObject(true);
        json.put("status", "ok");
        json.put("records", sharerExists && successful ? 1 : 0);
        json.put("message", "deleted");
        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString());
        if (jsonp) sos.println(");");
        sos.println();
    }


    private void doSearch(Query post, HttpServletResponse response) throws IOException {
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
        JSONArray entries_to_map = new JSONArray();
        for (ImportProfileEntry entry : entries) {
            JSONObject entry_to_map = entry.toJSON();
            if ("true".equals(detailed)) {
                String query = "";
                for (String msgId : entry.getImported()) {
                    query += "id:" + msgId + " ";
                }
                DAO.SearchLocalMessages search = new DAO.SearchLocalMessages(query, Timeline.Order.CREATED_AT, 0, 1000, 0);
                entry_to_map.put("imported", search.timeline.toJSON(false).get("statuses"));
            }
            entries_to_map.put(entry_to_map);
        }
        post.setResponse(response, "application/javascript");

        JSONObject m = new JSONObject(true);
        JSONObject metadata = new JSONObject();
        metadata.put("count", entries.size());
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);
        m.put("profiles", entries_to_map);

        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(minified ? m.toString() : m.toString(2));
        if (jsonp) sos.println(");");
        sos.println();
    }
}
