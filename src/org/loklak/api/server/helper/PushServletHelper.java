package org.loklak.api.server.helper;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.harvester.SourceType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushServletHelper {

    public static PushReport saveMessages(List<Map<String, Object>> messages) {
        PushReport report = new PushReport();
        for (Map<String, Object> message : messages) {
            Map<String, Object> user = (Map<String, Object>) message.remove("user");
            MessageEntry messageEntry = new MessageEntry(message);
            UserEntry userEntry = new UserEntry((user != null && user.get("screen_name") != null) ? user : new HashMap<String, Object>());
            boolean successful;
            report.incrementRecordCount();
            try {
                successful = DAO.writeMessage(messageEntry, userEntry, true, false);
            } catch (Exception e) {
                report.incrementErrorCount();
                continue;
            }
            if (successful) report.incrementNewCount();
            else report.incrementKnownCount();
        }

        return report;
    }

    public static String printResponse(String callback, PushReport pushReport) throws IOException {

        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", "ok");
        json.field("records", pushReport.getRecordCount());
        json.field("new", pushReport.getNewCount());
        json.field("known", pushReport.getKnownCount());
        json.field("error", pushReport.getErrorCount());
        json.field("message", "pushed");
        json.endObject();


        // build result
        String result = "";
        boolean jsonp = callback != null && callback.length() > 0;
        if (jsonp) result += callback + "(";
        result += json.string();
        if (jsonp) result += ");";

        return result;
    }

    public static String computeMessageId(Map<String, Object> message, Object initialId, SourceType sourceType) throws Exception {
        List<Object> location = (List<Object>) message.get("location_point");
        Object rawLon = location.get(1);
        String longitude =
                rawLon instanceof Integer ? Integer.toString((Integer) rawLon)
                : (rawLon instanceof Double ? Double.toString((Double) rawLon) : (String) rawLon);
        Object rawLat = location.get(0);
        String latitude =
                rawLat instanceof Integer ? Integer.toString((Integer) rawLat)
                : (rawLat instanceof  Double ? Double.toString((Double) rawLat) :(String) rawLat);

        // Modification time = 'mtime' value. If not found, take current time
        Object mtime = message.get("mtime");
        if (mtime == null) {
            mtime = Long.toString(System.currentTimeMillis());
            message.put("mtime", mtime);
        }
        return sourceType.name() + "_" + initialId + "_" + longitude + "_" + latitude + "_" + mtime;
    }

}
