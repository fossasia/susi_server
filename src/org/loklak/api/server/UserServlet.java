/**
 *  UserServlet
 *  Copyright 27.05.2015 by Michael Peter Christen, @0rb1t3r
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

public class UserServlet extends HttpServlet {
   
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
                if (twitterUserEntry != null) {
                    TwitterAPI.enrichLocation(twitterUserEntry);
                    twitterUserEntries.add(twitterUserEntry);
                }
            } catch (TwitterException e) {}
        }
        Map<String, Object> topology = null;
        try {topology = TwitterAPI.getNetwork(screen_names[0], maxFollowers, maxFollowing);} catch (TwitterException e) {}
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);

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
