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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.Crawler;
import org.loklak.DAO;
import org.loklak.api.RemoteAccess;
import org.loklak.api.ServletHelper;

public class CrawlerServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String clientHost = request.getRemoteHost();
        String XRealIP = request.getHeader("X-Real-IP"); if (XRealIP != null && XRealIP.length() > 0) clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
        boolean localhost = RemoteAccess.isLocalhost(clientHost);
        
        Map<String, String> qm = ServletHelper.getQueryMap(request.getQueryString());
        String callback = qm == null ? request.getParameter("callback") : qm.get("callback");
        boolean jsonp = callback != null && callback.length() > 0;

        String incubationParam =  qm == null ? request.getParameter("start") : qm.get("start");
        String depthParam =  qm == null ? request.getParameter("depth") : qm.get("depth");
        String hashtagsParam =  qm == null ? request.getParameter("hashtags") : qm.get("hashtags");
        String usersParam =  qm == null ? request.getParameter("users") : qm.get("users");
        
        String[] incubation = incubationParam == null ? new String[0] : incubationParam.split(",");
        int depth = depthParam == null ? 0 : Math.min(localhost ? 8 : 1, Integer.parseInt(depthParam));
        boolean hashtags = hashtagsParam == null ? true : hashtagsParam.equals("true");
        boolean users = usersParam == null ? true : usersParam.equals("true");
        
        for (String query: incubation) Crawler.stack(query, depth, hashtags, users, true);
        
        long now = System.currentTimeMillis();
        response.setDateHeader("Last-Modified", now);
        response.setDateHeader("Expires", now);
        response.setContentType("application/javascript");
        response.setHeader("X-Robots-Tag",  "noindex,noarchive,nofollow,nosnippet");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        if (incubation == null || incubation.length == 0) json.field("_hint", "start a crawl: start=<terms, comma-separated>, depth=<crawl depth> (dflt: 0), hashtags=<true|false> (dflt: true), users=<true|false> (dflt: true)");
        if (!localhost) json.field("_hint", "you are connecting from a non-localhost client " + clientHost + " , depth is limited to 1");
        json.field("index_sizes");
        json.startObject();
        json.field("messages", DAO.countLocalMessages());
        json.field("users", DAO.countLocalUsers());
        json.endObject();
        json.field("crawler_status"); Crawler.toJSON(json);
        json.endObject();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
