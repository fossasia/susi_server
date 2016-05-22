/**
 *  CrawlerServlet
 *  Copyright 24.02.2015 by Michael Peter Christen, @0rb1t3r
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

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.loklak.Crawler;
import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;

public class CrawlerServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        
        boolean localhost = post.isLocalhostAccess();
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;

        String[] incubation =  post.get("start", new String[0], ",");
        int depth = Math.min(localhost ? 8 : 1, post.get("depth", 0));
        boolean hashtags = post.get("hashtags", true);
        boolean users = post.get("users", true);
        
        for (String query: incubation) Crawler.stack(query, depth, hashtags, users, true);
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        JSONObject json = new JSONObject(true);
        if (incubation == null || incubation.length == 0) json.put("_hint", "start a crawl: start=<terms, comma-separated>, depth=<crawl depth> (dflt: 0), hashtags=<true|false> (dflt: true), users=<true|false> (dflt: true)");
        if (!localhost) json.put("_hint", "you are connecting from a non-localhost client " + post.getClientHost() + " , depth is limited to 1");
        JSONObject index_sizes = new JSONObject(true);
        json.put("index_sizes", index_sizes);
        index_sizes.put("messages", DAO.countLocalMessages(-1));
        index_sizes.put("users", DAO.countLocalUsers());
        json.put("crawler_status", Crawler.toJSON());

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString(2));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
