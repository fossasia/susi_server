/**
 *  GraphServlet
 *  Copyright 14.10.2015 by Michael Peter Christen, @0rb1t3r
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.harvester.TwitterAPI;

import twitter4j.TwitterException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphServlet extends HttpServlet {
    
    private static final long serialVersionUID = 8578478303032749879L;

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
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        String[] screen_names = post.get("screen_name", "").split(",");
        String followers = screen_names.length == 1 ? post.get("followers", "0") : "0";
        String following = screen_names.length == 1 ? post.get("following", "0") : "0";
        int maxFollowers = Integer.parseInt(followers);
        int maxFollowing = Integer.parseInt(following);
        
        List<Map<String, Object>> twitterUserEntries = new ArrayList<>();
        for (String screen_name: screen_names) {
            try {
                Map<String, Object> twitterUserEntry = TwitterAPI.getUser(screen_name, false);
                if (twitterUserEntry != null) twitterUserEntries.add(twitterUserEntry);
            } catch (TwitterException e) {}
        }
        Map<String, Object> topology = null;
        try {topology = TwitterAPI.getNetwork(screen_names[0], maxFollowers, maxFollowing);} catch (TwitterException e) {}
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("edges", post.getClientHost());

        if (twitterUserEntries.size() == 1) m.put("user", twitterUserEntries.iterator().next());
        if (twitterUserEntries.size() > 1) m.put("users", twitterUserEntries);
        if (topology != null) m.put("topology", topology);
        
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print((minified ? new ObjectMapper().writer() : new ObjectMapper().writerWithDefaultPrettyPrinter()).writeValueAsString(m));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
}

/*
 * produce something like that:
{
"edges" : "76",
"maxdepth" : "2",
"graph" : [{"source":"/en/index.html", "target":"http://fsfe.org/about/basics/freesoftware.en.html", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://en.wikipedia.org/wiki/Peer-to-peer", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://www.gnu.org/licenses/gpl-2.0.html", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://localhost:8090/", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"https://www.youtube.com/watch?v=CFwebavBU0s", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://openjdk.java.net/install/", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"https://www.youtube.com/watch?v=iqJuf_EA1UE", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://www.yacy-websuche.de/wiki/index.php/En:DebianInstall", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"https://www.youtube.com/watch?v=XDoVNzOMoIo", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://suma-ev.de/", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"http://loklak.org/", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"https://shop.spreadshirt.net/geekstuff/", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"},
{"source":"/en/index.html", "target":"https://twitter.com/share", "type":"Outbound", "depthSource":"-1", "depthTarget":"-1"}
]
}
*/