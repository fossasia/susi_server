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
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.search.sort.SortOrder;
import org.loklak.data.AbstractIndexEntry;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

// test suggestions with http://localhost:9000/api/suggest.json?q=beer&orderby=query_count&order=desc

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
        boolean delete = post.get("delete", false);
        int count = post.get("count", 100); // number of queries
        String query = post.get("q", ""); // to get a list of queries which match; to get all latest: leave q empty
        String orders = post.get("order", "asc").toUpperCase();
        SortOrder order = SortOrder.valueOf(orders);        
        String orderby = post.get("orderby", query.length() == 0 ? "retrieval_next" : "query_count");
        int timezoneOffset = post.get("timezoneOffset", 0);
        Date since = post.get("since", (Date) null, timezoneOffset);
        Date until = post.get("until", (Date) null, timezoneOffset);
        String selectby = post.get("selectby", "retrieval_next");
        List<QueryEntry> queryList = query.length() == 0 && !local ? null : DAO.SearchLocalQueries(query, count, orderby, order, since, until, selectby);
        
        if (delete && queryList != null) {
            for (QueryEntry qe: queryList) DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
            queryList = query.length() == 0 && !local ? null : DAO.SearchLocalQueries(query, count, orderby, order, since, until, selectby);
        }
        
        post.setResponse(response, "application/javascript");
        
        // generate json

        final StringWriter s = new StringWriter();
        JsonGenerator json = DAO.jsonFactory.createGenerator(s);
        json.setPrettyPrinter(minified ? new MinimalPrettyPrinter() : new DefaultPrettyPrinter());

        json.writeStartObject();
        
        json.writeObjectFieldStart("search_metadata");
        json.writeObjectField("count", queryList == null ? "0" : Integer.toString(queryList.size()));
        json.writeObjectField("query", query);
        json.writeObjectField("order", orders);
        json.writeObjectField("orderby", orderby);
        if (since != null) AbstractIndexEntry.writeDate(json, "since", since.getTime());
        if (until != null) AbstractIndexEntry.writeDate(json, "until", until.getTime());
        if (since != null || until != null) json.writeObjectField("selectby", selectby);
        json.writeObjectField("client", post.getClientHost());
        json.writeEndObject(); // of search_metadata
        
        json.writeArrayFieldStart("queries");
        if (queryList != null) {
            for (QueryEntry t: queryList) {
                t.toJSON(json);
            }
        }
        json.writeEndArray(); // of queries
        
        json.writeEndObject(); // of root
        json.close();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(s.toString());
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
