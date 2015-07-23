package org.loklak.api.server;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        boolean jsonp = callback != null && callback.length() > 0;
        String source_type = post.get("source_type", "");
        if ("".equals(source_type) || !SourceType.hasValue(source_type)) {
            response.sendError(400, "your request does not contain a valid source_type parameter.");
            return;
        }

        List<ImportProfileEntry> entries = DAO.SearchLocalImportProfiles(source_type);
        List<Map<String, Object>>  entries_to_map = new ArrayList<>();
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
