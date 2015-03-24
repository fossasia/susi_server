/**
 *  PushServlet
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.DAO;
import org.loklak.ProviderType;
import org.loklak.Tweet;
import org.loklak.User;
import org.loklak.api.RemoteAccess;


public class PushServlet extends HttpServlet {
    
    private static final long serialVersionUID = 7504310048722996407L;

    /*
     * There are the following sources for data, pushed or retrieved:
     * - twitter (scraped self)
     * - remote (pushed by a remote peer, could be faked by a user)
     * - user (pushed by a user)
     */

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(400, "your must call this with HTTP POST");
        return;
    }
    
    /*
     * call this i.e. with
     * curl -i -F callback=p -F data=@tweets.json http://localhost:9100/api/push.json
     */
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));
                
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        Map<String, String> m = RemoteAccess.getPostMap(request);
        String data = m.get("data");
        String callback = m.get("callback");
        boolean jsonp = callback != null && callback.length() > 0;
        if (data == null || data.length() == 0) {response.sendError(400, "your request does not contain a data object. The data object should contain data to be pushed. The format of the data object is JSON; it is exactly the same as the JSON search result"); return;}
        
        // parse the json data
        int recordCount = 0, newCount = 0, knownCount = 0;
        try {
            XContentParser parser = JsonXContent.jsonXContent.createParser(data);
            Map<String, Object> map = parser == null ? null : parser.map();
            Object statuses_obj = map.get("statuses");
            @SuppressWarnings("unchecked") List<Map<String, Object>> statuses = statuses_obj instanceof List<?> ? (List<Map<String, Object>>) statuses_obj : null;
            if (statuses != null) {
                for (Map<String, Object> tweet: statuses) {
                    recordCount++;
                    @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) tweet.remove("user");
                    if (user == null) continue;
                    tweet.put("provider_type", (Object) ProviderType.REMOTE.name());
                    tweet.put("provider_hash", remoteHash);
                    User u = new User(user);
                    Tweet t = new Tweet(tweet);
                    boolean newtweet = DAO.record(t, u, true);
                    if (newtweet) newCount++; else knownCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        json.endObject(); // of root

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
            
        DAO.log(request.getServletPath() + " -> records = " + recordCount + ", new = " + newCount + ", known = " + knownCount + ", from host hash " + remoteHash);
    }
}
