/**
 *  PeersServlet
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

package org.loklak.api.p2p;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.ProviderType;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.server.Query;

public class PeersServlet extends HttpServlet {

    private static final long serialVersionUID = -2577184683745091648L;

    public static Timeline peers(final String protocolhostportstub, final String query, final Timeline.Order order, final String source, final int count, final int timezoneOffset, final String provider_hash) throws IOException {
        Timeline tl = new Timeline(order);
        String urlstring = "";
        urlstring = protocolhostportstub + "/api/peers.json";
        byte[] response = ClientConnection.download(urlstring);
        if (response == null || response.length == 0) return tl;
        JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
        JSONArray statuses = json.has("statuses") ? json.getJSONArray("statuses") : null;
        if (statuses != null) {
            for (Object tweet_obj: statuses) {
                JSONObject tweet = (JSONObject) tweet_obj;
                JSONObject user = tweet.has("user") ? (JSONObject) tweet.remove("user") : null;
                if (user == null) continue;
                tweet.put("provider_type", (Object) ProviderType.REMOTE.name());
                tweet.put("provider_hash", provider_hash);
                UserEntry u = new UserEntry(user);
                MessageEntry t = new MessageEntry(tweet);
                tl.add(t, u);
            }
        }
        //System.out.println(parser.text());
        return tl;
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        
        // manage DoS
        String path = request.getServletPath();
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        // Check call type
        boolean jsonExt = request.getServletPath().endsWith(".json");
        boolean csvExt = request.getServletPath().endsWith(".csv");

        String[] classes = post.get("classes", new String[0], ",");
        if (classes.length == 0) classes = new String[]{"HelloServlet","SuggestServlet"};
        Set<String> classcheck = new HashSet<>();
        for (String c: classes) classcheck.add(c);
        
        // Evaluating the get Parameter
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        // String pingback = qm == null ? request.getParameter("pingback") : qm.get("pingback");
        // pingback may be either filled with nothing, the term 'now' or the term 'later'

        if (jsonExt) {
            post.setResponse(response, "application/javascript");
            // generate json
            JSONObject json = new JSONObject(true);
            JSONArray peers = new JSONArray();
            json.put("peers", peers);
            int count = 0;
            for (Map.Entry<String, Map<String, RemoteAccess>> hmap: RemoteAccess.history.entrySet()) {
                if (classcheck.contains(hmap.getKey())) {
                    JSONObject p = new JSONObject(true);
                    for (Map.Entry<String, RemoteAccess> peer: hmap.getValue().entrySet()) {
                        p.put("class", hmap.getKey());
                        p.put("host", peer.getKey());
                        RemoteAccess remoteAccess = peer.getValue();
                        p.put("port.http", remoteAccess.getLocalHTTPPort());
                        p.put("port.https", remoteAccess.getLocalHTTPSPort());
                        p.put("lastSeen", remoteAccess.getAccessTime());
                        p.put("lastPath", remoteAccess.getLocalPath());
                        p.put("peername", remoteAccess.getPeername());
                        peers.put(p);
                        count++;
                    }
                }
            }
            json.put("count", count);
    
            // write json
            response.setCharacterEncoding("UTF-8");
            PrintWriter sos = response.getWriter();
            if (jsonp) sos.print(callback + "(");
            sos.print(json.toString(2));
            if (jsonp) sos.println(");");
            sos.println();
        } else if (csvExt) {
            post.setResponse(response, "text/plain");
            final StringBuilder buffer = new StringBuilder(1000);
            for (Map.Entry<String, Map<String, RemoteAccess>> hmap: RemoteAccess.history.entrySet()) {
                if (classcheck.contains(hmap.getKey())) {
                    for (Map.Entry<String, RemoteAccess> peer: hmap.getValue().entrySet()) {
                        String peerLine = "";
                        peerLine.concat(hmap.getKey());
                        peerLine.concat(",");
                        peerLine.concat(peer.getKey());
                        peerLine.concat(",");
                        RemoteAccess remoteAccess = peer.getValue();
                        peerLine.concat(String.valueOf(remoteAccess.getLocalHTTPPort()));
                        peerLine.concat(",");
                        peerLine.concat(String.valueOf(remoteAccess.getLocalHTTPSPort()));
                        peerLine.concat(",");
                        peerLine.concat(String.valueOf(remoteAccess.getAccessTime()));
                        peerLine.concat(",");
                        peerLine.concat(String.valueOf(remoteAccess.getLocalPath()));
                        peerLine.concat(",");
                        peerLine.concat(String.valueOf(remoteAccess.getPeername()));
                        peerLine.concat("\n");
                        buffer.append(peerLine);
                    }
                }
            }
            response.setCharacterEncoding("UTF-8");
            PrintWriter sos = response.getWriter();
            sos.print(buffer.toString());
            sos.println();
        }

        post.finalize();
    }
    
}
