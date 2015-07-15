package org.loklak.api.server;

import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.helper.PushReport;
import org.loklak.api.server.helper.PushServletHelper;
import org.loklak.data.DAO;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FreifunkNodePushServlet extends HttpServlet {

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

        JsonValidator validator = new JsonValidator();
        ProcessingReport report = validator.validate(new String(jsonText), JsonValidator.JsonSchemaEnum.FREIFUNK_NODE);
        if (report.getLogLevel() == LogLevel.ERROR || report.getLogLevel() == LogLevel.FATAL) {
            response.sendError(400, "json does not conform to Freifunk node schema " + report);
            return;
        }

        // push nodes
        JsonFieldConverter converter = new JsonFieldConverter();
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) map.get("nodes");
        nodes = converter.convert(nodes, JsonFieldConverter.JsonConversionSchemaEnum.FREIFUNK_NODE);

        for (Map<String, Object> node : nodes) {
            try {
                node.put("id_str", computeNodeId(node));
            } catch (Exception e) {
                DAO.log("Problem computing id" + node);
                continue;
            }
            node.put("source_type", SourceType.FREIFUNK_NODE.name());
        }
        PushReport nodePushReport = PushServletHelper.saveMessages(nodes);

        String res = PushServletHelper.printResponse(post.get("callback", ""), nodePushReport);
        response.getOutputStream().println(res);
        DAO.log(request.getServletPath()
                + " -> records = " + nodePushReport.getRecordCount()
                + ", new = " + nodePushReport.getNewCount() + ", known = " + nodePushReport.getKnownCount() + ", from host hash " + remoteHash);
    }

    private static String computeNodeId(Map<String, Object> node) throws Exception {
        String id = (String) node.get("id");
        boolean hasId = id != null && id.equals("");

        List<Object> location = (List<Object>) node.get("location_point");
        Object rawLon = location.get(1);
        String longitude = rawLon instanceof Integer ? Integer.toString((Integer) rawLon) : Double.toString((Double) rawLon);
        Object rawLat = location.get(0);
        String latitude = rawLat instanceof Integer ? Integer.toString((Integer) rawLat) : Double.toString((Double) rawLat);
        // Modification time = current time
        String mtime = Long.toString(System.currentTimeMillis());
        return SourceType.FREIFUNK_NODE.name() + (hasId ? "_" + id : "") + "_" + longitude + "_" + latitude + "_" + mtime;
    }
}
