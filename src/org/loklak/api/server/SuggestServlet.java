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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.harvester.SourceType;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.AbstractIndexEntry;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.ResultList;

/*
 * - test suggestions -
 * 
 * most common queries
 * http://localhost:9000/api/suggest.json?q=beer&orderby=query_count&order=desc
 *
 * random list of re-load queries
 * http://localhost:9000/api/suggest.json?source=query&orderby=retrieval_next&order=asc&count=1000&until=now&random=3
 */

public class SuggestServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    public static Map<Integer, JSONObject> cache = new ConcurrentHashMap<>();
    
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
        boolean minified = post.get("minified", false);
        
        int requestkey = post.hashCode();
        JSONObject m = post.isDoS_servicereduction() ? cache.get(requestkey) : null;
        if (m == null) {
            boolean local = post.isLocalhostAccess();
            boolean delete = post.get("delete", false);
            int count = post.get("count", 10); // number of queries
            String query = post.get("q", ""); // to get a list of queries which match; to get all latest: leave q empty
            String source = post.get("source", "all"); // values: all,query,geo
            String orders = post.get("order", query.length() == 0 ? "desc" : "asc").toUpperCase();
            SortOrder order = SortOrder.valueOf(orders);
            String orderby = post.get("orderby", query.length() == 0 ? "retrieval_next" : "query_count");
            int timezoneOffset = post.get("timezoneOffset", 0);
            Date since = post.get("since",  "").equals("now") ? new Date() : post.get("since", (Date) null, timezoneOffset);
            Date until = post.get("until",  "").equals("now") ? new Date() : post.get("until", (Date) null, timezoneOffset);
            String selectby = post.get("selectby", "retrieval_next");
            ResultList<QueryEntry> queryList = new ResultList<>();
    
            if ((source.equals("all") || source.equals("query")) && query.length() >= 0) {
                long start = System.currentTimeMillis();
                queryList = DAO.SearchLocalQueries(query, count, orderby, "long", order, since, until, selectby);
                post.recordEvent("localqueries_time", System.currentTimeMillis() - start);
            }
            
            if (delete && local && queryList.size() > 0) {
                long start = System.currentTimeMillis();
                for (QueryEntry qe: queryList) DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
                queryList.clear();
                queryList = DAO.SearchLocalQueries(query, count, orderby, "long", order, since, until, selectby);
                post.recordEvent("localquerydelete_time", System.currentTimeMillis() - start);
            }
            
            if (source.equals("all") || source.equals("geo")) {
                long start = System.currentTimeMillis();
                LinkedHashSet<String> suggestions = DAO.geoNames.suggest(query, count, 0);
                if (suggestions.size() < count && query.length() > 2) suggestions.addAll(DAO.geoNames.suggest(query, count, 1));
                if (suggestions.size() < count && query.length() > 5) suggestions.addAll(DAO.geoNames.suggest(query, count, 2));
                for (String s: suggestions) {
                    QueryEntry qe = new QueryEntry(s, 0, Long.MAX_VALUE, SourceType.IMPORT, false);
                    queryList.add(qe);
                }
                post.recordEvent("suggestionsquery_time", System.currentTimeMillis() - start);
            }

            long start = System.currentTimeMillis();        
            post.setResponse(response, "application/javascript");

            List<Object> queries = new ArrayList<>();
            if (queryList != null) for (QueryEntry t: queryList) queries.add(t.toJSON().toMap());

            int random = post.get("random", -1);
            if (random > 0 && random < queries.size()) {
                // take the given number from the result list and use random to choose
                List<Object> random_queries = new ArrayList<>();
                Random r = new Random(System.currentTimeMillis());
                while (random-- > 0) {
                    random_queries.add(queries.remove(r.nextInt(queries.size())));
                    int shrink = Math.max(queries.size() / 2, random * 10);
                    while (queries.size() > shrink) queries.remove(queries.size() - 1); // prefer from top
                }
                queries = random_queries;
            }
            
            // generate json
            m = new JSONObject(true);
            JSONObject metadata = new JSONObject(true);
            metadata.put("count", queryList == null ? "0" : Integer.toString(queries.size()));
            metadata.put("hits", queryList.getHits());
            metadata.put("query", query);
            metadata.put("order", orders);
            metadata.put("orderby", orderby);
            if (since != null) metadata.put("since", AbstractIndexEntry.utcFormatter.print(since.getTime()));
            if (until != null) metadata.put("until", AbstractIndexEntry.utcFormatter.print(until.getTime()));
            if (since != null || until != null) metadata.put("selectby", selectby);
            metadata.put("client", post.getClientHost());
            m.put("search_metadata", metadata);
            
            m.put("queries", queries);
            post.recordEvent("postprocessing_time", System.currentTimeMillis() - start);
        }
        
        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(m.toString(minified ? 0 : 2));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
