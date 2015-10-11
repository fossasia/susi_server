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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.search.sort.SortOrder;
import org.loklak.data.AbstractIndexEntry;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;
import org.loklak.harvester.SourceType;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        int count = post.get("count", 10); // number of queries
        String query = post.get("q", ""); // to get a list of queries which match; to get all latest: leave q empty
        String source = post.get("source", "all"); // values: all,query,geo
        String orders = post.get("order", query.length() == 0 ? "desc" : "asc").toUpperCase();
        SortOrder order = SortOrder.valueOf(orders);        
        String orderby = post.get("orderby", query.length() == 0 ? "retrieval_next" : "query_count");
        int timezoneOffset = post.get("timezoneOffset", 0);
        Date since = post.get("since", (Date) null, timezoneOffset);
        Date until = post.get("until", (Date) null, timezoneOffset);
        String selectby = post.get("selectby", "retrieval_next");
        List<QueryEntry> queryList = new ArrayList<>();

        if ((source.equals("all") || source.equals("query")) && query.length() >= 0) {
            queryList.addAll(DAO.SearchLocalQueries(query, count, orderby, order, since, until, selectby));
        }
        
        if (delete && local && queryList.size() > 0) {
            for (QueryEntry qe: queryList) DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
            queryList.clear();
            queryList.addAll(DAO.SearchLocalQueries(query, count, orderby, order, since, until, selectby));
        }
        
        if (source.equals("all") || source.equals("geo")) {
            LinkedHashSet<String> suggestions = DAO.geoNames.suggest(query, count, 0);
            if (suggestions.size() < count && query.length() > 2) suggestions.addAll(DAO.geoNames.suggest(query, count, 1));
            if (suggestions.size() < count && query.length() > 5) suggestions.addAll(DAO.geoNames.suggest(query, count, 2));
            for (String s: suggestions) {
                QueryEntry qe = new QueryEntry(s, 0, Long.MAX_VALUE, SourceType.IMPORT, false);
                queryList.add(qe);
            }
        }
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("count", queryList == null ? "0" : Integer.toString(queryList.size()));
        metadata.put("query", query);
        metadata.put("order", orders);
        metadata.put("orderby", orderby);
        if (since != null) metadata.put("since", AbstractIndexEntry.utcFormatter.print(since.getTime()));
        if (until != null) metadata.put("until", AbstractIndexEntry.utcFormatter.print(until.getTime()));
        if (since != null || until != null) metadata.put("selectby", selectby);
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);
        
        List<Object> queries = new ArrayList<>();
        if (queryList != null) {
            for (QueryEntry t: queryList) {
                queries.add(t.toMap());
            }
        }
        m.put("queries", queries);
        
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(minified ? new ObjectMapper().writer().writeValueAsString(m) : new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(m));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
