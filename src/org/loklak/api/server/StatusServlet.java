/**
 *  CrawlerServlet
 *  Copyright 27.02.2015 by Michael Peter Christen, @0rb1t3r
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
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.DAO;
import org.loklak.api.ServletHelper;

public class StatusServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String clientHost = request.getRemoteHost();
        String XRealIP = request.getHeader("X-Real-IP"); if (XRealIP != null && XRealIP.length() > 0) clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
        
        Map<String, String> qm = ServletHelper.getQueryMap(request.getQueryString());
        String callback = qm == null ? request.getParameter("callback") : qm.get("callback");
        boolean jsonp = callback != null && callback.length() > 0;

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
        json.field("index_sizes");
        json.startObject();
        json.field("messages", DAO.countLocalMessages());
        json.field("users", DAO.countLocalUsers());
        json.endObject();
        json.field("client_info");
        json.startObject();
        json.field("RemoteHost", clientHost);
        Enumeration<String> he = request.getHeaderNames();
        while (he.hasMoreElements()) {
            String h = he.nextElement();
            json.field(h, request.getHeader(h));
        }
        json.endObject();
        json.endObject();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
