package org.loklak.api.server.helper;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;

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
}
