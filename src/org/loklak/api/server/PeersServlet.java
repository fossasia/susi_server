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

package org.loklak.api.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.http.RemoteAccess;

public class PeersServlet extends HttpServlet {

    private static final long serialVersionUID = -2577184683745091648L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        
        // manage DoS
        String path = request.getServletPath();
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
        String[] classes = post.get("classes", new String[0], ",");
        if (classes.length == 0) classes = new String[]{"HelloServlet","SuggestServlet"};
        Set<String> classcheck = new HashSet<>();
        for (String c: classes) classcheck.add(c);
        
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        // String pingback = qm == null ? request.getParameter("pingback") : qm.get("pingback");
        // pingback may be either filled with nothing, the term 'now' or the term 'later'

        post.setResponse(response, "application/javascript");
        
        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        int count = 0;
        json.field("peers").startArray();
        for (Map.Entry<String, Map<String, RemoteAccess>> hmap: RemoteAccess.history.entrySet()) {
            if (classcheck.contains(hmap.getKey())) {
                for (Map.Entry<String, RemoteAccess> peer: hmap.getValue().entrySet()) {
                    json.startObject();
                    json.field("class", hmap.getKey());
                    json.field("host", peer.getKey());
                    RemoteAccess remoteAccess = peer.getValue();
                    json.field("port.http", remoteAccess.getLocalHTTPPort());
                    json.field("port.https", remoteAccess.getLocalHTTPSPort());
                    json.field("lastSeen", remoteAccess.getAccessTime());
                    json.field("lastPath", remoteAccess.getLocalPath());
                    json.field("peername", remoteAccess.getPeername());
                    json.endObject();
                    count++;
                }
            }
        }
        json.endArray();
        json.field("count", count);
        json.endObject(); // of root

        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();

        post.finalize();
    }
    
}
