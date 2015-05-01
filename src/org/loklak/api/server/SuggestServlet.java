/**
 *  SuggestServlet
 *  Copyright 29.04.2015 by Michael Peter Christen, @0rb1t3r
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
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.api.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;

public class SuggestServlet extends HttpServlet {
   
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
        
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean local = post.isLocalhostAccess();
        boolean minified = post.get("minified", false);
        int count = post.get("count", 100); // number of queries
        String query = post.get("q", ""); // to get a list of queries which match; to get all latest: leave q empty
        String orders = post.get("order", "asc").toUpperCase();
        SortOrder order = SortOrder.valueOf(orders);        
        String orderby = post.get("orderby", query.length() == 0 ? "query_first" : "query_count");
        int timezoneOffset = post.get("timezoneOffset", 0);
        Date since = post.get("since", (Date) null, timezoneOffset);
        Date until = post.get("until", (Date) null, timezoneOffset);
        String selectby = post.get("selectby", "retrieval_next");
        List<QueryEntry> queryList = query.length() == 0 && !local ? null : DAO.SearchLocalQueries(query, count, orderby, order, since, until, selectby);
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder();
        if (!minified) json = json.prettyPrint();
        json.startObject();
        
        json.field("search_metadata").startObject();
        json.field("count", queryList == null ? "0" : Integer.toString(queryList.size()));
        json.field("query", query);
        json.field("order", orders);
        json.field("orderby", orderby);
        if (since != null) json.field("since", since);
        if (until != null) json.field("until", until);
        if (since != null || until != null) json.field("selectby", selectby);
        json.field("client", post.getClientHost());
        json.endObject(); // of search_metadata
        
        json.field("queries").startArray();
        if (queryList != null) {
            for (QueryEntry t: queryList) {
                t.toJSON(json);
            }
        }
        json.endArray(); // of queries
        
        json.endObject(); // of root

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
