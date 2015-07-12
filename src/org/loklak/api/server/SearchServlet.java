/**
 *  SearchServlet
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;
import org.loklak.data.Timeline;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.rss.RSSFeed;
import org.loklak.rss.RSSMessage;
import org.loklak.tools.CharacterCoding;
import org.loklak.tools.UTF8;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The search servlet. we provide opensearch/rss and twitter-like JSON as result.
 */
public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 563533152152063908L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your (" + post.getClientHost() + ") request frequency is too high"); return;}
        
        // check call type
        boolean jsonExt = request.getServletPath().endsWith(".json");

        // evaluate get parameter
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        String query = post.get("q", "");
        if (query == null || query.length() == 0) query = post.get("query", "");
        query = CharacterCoding.html2unicode(query).replaceAll("\\+", " ");
        final int count = post.isDoS_servicereduction() ? 10 : Math.min(post.get("count", post.get("maximumRecords", 100)), post.isLocalhostAccess() ? 10000 : 1000);
        String source = post.isDoS_servicereduction() ? "cache" : post.get("source", "all"); // possible values: cache, backend, twitter, all
        int limit = post.get("limit", 100);
        String[] fields = post.get("fields", new String[0], ",");
        int timezoneOffset = post.get("timezoneOffset", 0);
        if (query.indexOf("id:") >= 0 && ("all".equals(source) || "twitter".equals(source))) source = "cache"; // id's cannot be retrieved from twitter with the scrape-api (yet), only from the cache
        final String ordername = post.get("order", Timeline.Order.CREATED_AT.getMessageFieldName());
        final Timeline.Order order = Timeline.parseOrder(ordername);
        
        // create tweet timeline
        final Timeline tl = new Timeline(order);
        Map<String, List<Map.Entry<String, Long>>> aggregations = null;
        long hits = 0;
        final AtomicInteger newrecords = new AtomicInteger(0);
        if (query.length() > 0) {
            final String noConstraintsQuery = QueryEntry.removeConstraints(query);
            if ("all".equals(source)) {
                // start all targets for search concurrently
                final String queryf = query;
                final int timezoneOffsetf = timezoneOffset;
                Thread scraperThread = noConstraintsQuery.length() == 0 ? null : new Thread() {
                    public void run() {
                        Timeline[] twitterTl = DAO.scrapeTwitter(noConstraintsQuery, order, timezoneOffsetf, true);
                        newrecords.set(twitterTl[1].size());
                        tl.putAll(noConstraintsQuery.equals(queryf) ? twitterTl[1] : QueryEntry.applyConstraint(twitterTl[1], queryf));
                    }
                };
                if (scraperThread != null) scraperThread.start();
                Thread backendThread = new Thread() {
                    public void run() {
                        Timeline backendTl = DAO.searchBackend(queryf, order, count, timezoneOffsetf, "cache");
                        tl.putAll(noConstraintsQuery.equals(queryf) ? backendTl : QueryEntry.applyConstraint(backendTl, queryf));
                    }
                };
                backendThread.start();
                DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(query, order, timezoneOffset, count, 0);
                hits = localSearchResult.hits;
                tl.putAll(localSearchResult.timeline);
                try {backendThread.join(5000);} catch (InterruptedException e) {}
                if (scraperThread != null) try {scraperThread.join(8000);} catch (InterruptedException e) {}
            } else {
                if ("twitter".equals(source) && noConstraintsQuery.length() > 0) {
                    Timeline[] twitterTl = DAO.scrapeTwitter(noConstraintsQuery, order, timezoneOffset, true);
                    newrecords.set(twitterTl[1].size());
                    tl.putAll(noConstraintsQuery.equals(query) ? twitterTl[0] : QueryEntry.applyConstraint(twitterTl[0], query));
                    // in this case we use all tweets, not only the latest one because it may happen that there are no new and that is not what the user expects
                }
    
                // replace the timeline with one from the own index which now includes the remote result
                if ("backend".equals(source)) {
                    Timeline backendTl = DAO.searchBackend(query, order, count, timezoneOffset, "cache");
                    tl.putAll(noConstraintsQuery.equals(query) ? backendTl : QueryEntry.applyConstraint(backendTl, query));
                }
    
                // replace the timeline with one from the own index which now includes the remote result
                if ("cache".equals(source)) {
                    DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(query, order, timezoneOffset, count, limit, fields);
                    hits = localSearchResult.hits;
                    tl.putAll(localSearchResult.timeline);
                    aggregations = localSearchResult.aggregations;
                }
            }
        }
        hits = Math.max(hits, tl.size());
        // reduce the list to the wanted number of results if we have more
        tl.reduceToMaxsize(count);
        

        if (post.isDoS_servicereduction() && !RemoteAccess.isSleepingForClient(post.getClientHost())) {
            RemoteAccess.sleep(post.getClientHost(), 2000);
        }
        post.setResponse(response, jsonExt ? (jsonp ? "application/javascript": "application/json") : "application/rss+xml;charset=utf-8");
        
        // create json or xml according to path extension
        if (jsonExt) {
            // generate json
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            if (!minified) {
                m.put("readme_0", "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!");
                m.put("readme_1", "loklak.org is the framework for a message search system, not the portal, read: http://loklak.org/about.html#notasearchportal");
                m.put("readme_2", "This is supposed to be the back-end of a search portal. For the api, see http://loklak.org/api.html");
                m.put("readme_3", "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)");
            }
            metadata.put("itemsPerPage", Integer.toString(count));
            metadata.put("count", Integer.toString(tl.size()));
            metadata.put("hits", hits);
            if (order == Timeline.Order.CREATED_AT) metadata.put("period", tl.period());
            metadata.put("query", query);
            metadata.put("client", post.getClientHost());
            metadata.put("servicereduction", post.isDoS_servicereduction() ? "true" : "false");
            m.put("search_metadata", metadata);
            List<Object> statuses = new ArrayList<>();
            for (MessageEntry t: tl) {
                UserEntry u = tl.getUser(t);
                statuses.add(t.toMap(u, true));
            }
            m.put("statuses", statuses);
            
            // aggregations
            Map<String, Object> agg = new LinkedHashMap<String, Object>();
            if (aggregations != null) {
                for (Map.Entry<String, List<Map.Entry<String, Long>>> aggregation: aggregations.entrySet()) {
                    Map<String, Object> facet = new LinkedHashMap<>();
                    for (Map.Entry<String, Long> a: aggregation.getValue()) {
                        if (a.getValue().equals(query)) continue; // we omit obvious terms that cannot be used for faceting, like search for "#abc" -> most hashtag is "#abc"
                        facet.put(a.getKey(), a.getValue());
                    }
                    agg.put(aggregation.getKey(), facet);
                }
            }
            m.put("aggregations", agg);
            
            // write json
            ServletOutputStream sos = response.getOutputStream();
            if (jsonp) sos.print(callback + "(");
            sos.print(minified ? new ObjectMapper().writer().writeValueAsString(m) : new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(m));
            if (jsonp) sos.println(");");
            sos.println();
        } else {
            // generate xml
            RSSMessage channel = new RSSMessage();
            channel.setPubDate(new Date());
            channel.setTitle("RSS feed for Twitter search for " + query);
            channel.setDescription("");
            channel.setLink("");
            RSSFeed feed = new RSSFeed(tl.size());
            feed.setChannel(channel);
            for (MessageEntry t: tl) {
                UserEntry u = tl.getUser(t);
                RSSMessage m = new RSSMessage();
                m.setLink(t.getStatusIdUrl().toExternalForm());
                m.setAuthor(u.getName() + " @" + u.getScreenName());
                m.setTitle(u.getName() + " @" + u.getScreenName());
                m.setDescription(t.getText());
                m.setPubDate(t.getCreatedAt());
                m.setGuid(t.getIdStr());
                feed.addMessage(m);
            }
            String rss = feed.toString();
            //System.out.println("feed has " + feed.size() + " entries");
            
            // write xml
            response.getOutputStream().write(UTF8.getBytes(rss));
        }
        DAO.log(request.getServletPath() + "?" + request.getQueryString() + " -> " + tl.size() + " records returned, " +  newrecords.get() + " new");
        } catch (Throwable e) {
            Log.getLog().warn(e.getMessage(), e);
            //e.printStackTrace();
        }
    }
}
