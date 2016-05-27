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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.QueuedIndexing;
import org.loklak.data.DAO;
import org.loklak.data.IndexEntry;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.ProviderType;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.objects.Timeline.Order;
import org.loklak.server.Query;
import org.loklak.tools.UTF8;


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
        doPost(request, response);
        //response.sendError(400, "your must call this with HTTP POST");
        //return;
    }
    
    /*
     * call this i.e. with
     * curl -i -F callback=p -F data=@tweets.json http://localhost:9000/api/push.json
     */
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        long timeStart = System.currentTimeMillis();
        
        Query post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));
        boolean remoteHashFromPeerId = false;
                
        // manage DoS
        if (post.isDoS_blackout()) {
            response.sendError(503, "your request frequency is too high");
            return;
        }

        Map<String, byte[]> m = RemoteAccess.getPostMap(request);
        byte[] data = m.get("data");
        String callback = UTF8.String(m.get("callback"));
        boolean jsonp = callback != null && callback.length() > 0;
        if (data == null || data.length == 0) {response.sendError(400, "your request does not contain a data object. The data object should contain data to be pushed. The format of the data object is JSON; it is exactly the same as the JSON search result"); return;}
        
        // parse the json data
        int recordCount = 0;//, newCount = 0, knownCount = 0;
        String query = null;
        long timeParsing = 0, timeTimelineStorage = 0, timeQueryStorage = 0;
        String jsonString = new String(data, StandardCharsets.UTF_8);
        JSONObject map = new JSONObject(jsonString);

        timeParsing = System.currentTimeMillis();
        
        // read metadata
        JSONObject metadata = map.has("search_metadata") ? (JSONObject) map.get("search_metadata") : null;
        // read peer id if they submitted one
        if (metadata != null) {
            String peerid = metadata.has("peerid") ? (String) metadata.get("peerid") : null;
            if (peerid != null && peerid.length() > 3 && peerid.charAt(2) == '_') {
                remoteHash = peerid;
                remoteHashFromPeerId = true;
            }
        }
        
        // read statuses
        JSONArray statuses = map.has("statuses") ? map.getJSONArray("statuses") : null;
        if (statuses != null) {
            Timeline tl = new Timeline(Order.CREATED_AT);
            for (Object tweet_obj: statuses) {
                JSONObject tweet = (JSONObject) tweet_obj;
                recordCount++;
                JSONObject user = (JSONObject) tweet.remove("user");
                if (user == null) continue;
                tweet.put("provider_type", ProviderType.REMOTE.name());
                tweet.put("provider_hash", remoteHash);
                UserEntry u = new UserEntry(user);
                MessageEntry t = new MessageEntry(tweet);
                tl.add(t, u);
                //boolean newtweet = DAO.writeMessage(t, u, true, true, true);
                //if (newtweet) newCount++; else knownCount++;
            }
            QueuedIndexing.addScheduler(tl, true);
            //try {DAO.users.bulkCacheFlush();} catch (IOException e) {}
            //try {DAO.messages.bulkCacheFlush();} catch (IOException e) {}

            timeTimelineStorage = System.currentTimeMillis();
            
            // update query database if query was given in the result list
            if (metadata != null) {
                query = metadata.has("query") ? (String) metadata.get("query") : null;
                if (query != null) {
                    // update query database
                    QueryEntry qe = null;
                    try {
                        qe = DAO.queries.read(query);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (qe != null) {
                        // existing queries are updated
                        qe.update(tl.period(), false);
                        try {
                            DAO.queries.writeEntry(new IndexEntry<QueryEntry>(query, qe.getSourceType().name(), qe));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            timeQueryStorage = System.currentTimeMillis();
        }
        
        // in case that a peer submitted their peer id, we return also some statistics for that peer
        long messages_from_client = remoteHashFromPeerId ? DAO.countLocalMessages(remoteHash) : -1;

        post.setResponse(response, "application/javascript");
        
        // generate json
        JSONObject json = new JSONObject(true);
        json.put("status", "ok");
        json.put("records", recordCount);
        if (remoteHashFromPeerId) json.put("contribution_message_count", messages_from_client);
        //json.field("new", newCount);
        //json.field("known", knownCount);
        json.put("message", "pushed");

        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString(2));
        if (jsonp) sos.println(");");
        sos.println();

        long timeResponse = System.currentTimeMillis();
        
        DAO.log(
                request.getServletPath() + " -> records = " + recordCount +
                //", new = " + newCount +
                //", known = " + knownCount +
                ", from host hash " + remoteHash +
                (query == null ? "" : " for query=" + query) +
                ", timeParsing = " + (timeParsing - timeStart) +
                ", timeTimelineStorage = " + (timeTimelineStorage - timeParsing) +
                ", timeQueryStorage = " + (timeQueryStorage - timeTimelineStorage) +
                ", timeResponse = " + (timeResponse - timeQueryStorage) +
                ", total time = " + (timeResponse - timeStart)
                );

        response.addHeader("Access-Control-Allow-Origin", "*");
        post.finalize();
    }
}
