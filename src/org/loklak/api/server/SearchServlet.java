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
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

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
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
        
        // check call type
        boolean jsonExt = request.getServletPath().endsWith(".json");

        // evaluate get parameter
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        String query = post.get("q", "");
        if (query == null || query.length() == 0) query = post.get("query", "");
        query = CharacterCoding.html2unicode(query).replaceAll("\\+", " ");
        int count = post.get("count", post.get("maximumRecords", 100));
        String source = post.isDoS_servicereduction() ? "cache" : post.get("source", "all"); // possible values: cache, backend, twitter, all
        int limit = post.get("limit", 100);
        String[] fields = post.get("fields", new String[0], ",");
        int timezoneOffset = post.get("timezoneOffset", 0);
        if (query.indexOf("id:") >= 0 && ("all".equals(source) || "twitter".equals(source))) source = "cache"; // id's cannot be retrieved from twitter with the scrape-api (yet), only from the cache

        // create tweet timeline
        final Timeline tl = new Timeline();
        Map<String, List<Map.Entry<String, Long>>> aggregations = null;
        long hits = 0;
        if (query.length() > 0) {
            final String noConstraintsQuery = QueryEntry.removeConstraints(query);
            if ("all".equals(source)) {
                // start all targets for search concurrently
                final String queryf = query;
                final int timezoneOffsetf = timezoneOffset;
                Thread scraperThread = new Thread() {
                    public void run() {
                        Timeline[] twitterTl = DAO.scrapeTwitter(noConstraintsQuery, timezoneOffsetf, true);
                        tl.putAll(noConstraintsQuery.equals(queryf) ? twitterTl[1] : QueryEntry.applyConstraint(twitterTl[1], queryf));
                    }
                };
                scraperThread.start();
                Thread backendThread = new Thread() {
                    public void run() {
                        Timeline backendTl = DAO.searchBackend(queryf, 100, timezoneOffsetf, "cache");
                        tl.putAll(backendTl);
                    }
                };
                backendThread.start();
                DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(query, timezoneOffset, count, 0);
                hits = localSearchResult.hits;
                tl.putAll(localSearchResult.timeline);
                try {backendThread.join(5000);} catch (InterruptedException e) {}
                try {scraperThread.join(8000);} catch (InterruptedException e) {}
            } else {
                if ("twitter".equals(source)) {
                    Timeline[] twitterTl = DAO.scrapeTwitter(noConstraintsQuery, timezoneOffset, true);
                    tl.putAll(noConstraintsQuery.equals(query) ? twitterTl[0] : QueryEntry.applyConstraint(twitterTl[0], query));
                    // in this case we use all tweets, not only the latest one because it may happen that there are no new and that is not what the user expects
                }
    
                // replace the timeline with one from the own index which now includes the remote result
                if ("backend".equals(source)) {
                    tl.putAll(DAO.searchBackend(query, count, timezoneOffset, "cache"));
                }
    
                // replace the timeline with one from the own index which now includes the remote result
                if ("cache".equals(source)) {
                    DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(query, timezoneOffset, count, limit, fields);
                    hits = localSearchResult.hits;
                    tl.putAll(localSearchResult.timeline);
                    aggregations = localSearchResult.aggregations;
                }
            }
        }
        hits = Math.max(hits, tl.size());

        if (post.isDoS_servicereduction() && !RemoteAccess.isSleepingForClient(post.getClientHost())) {
            RemoteAccess.sleep(post.getClientHost(), 2000);
        }
        post.setResponse(response, jsonExt ? (jsonp ? "application/javascript": "application/json") : "application/rss+xml;charset=utf-8");
        
        // create json or xml according to path extension
        if (jsonExt) {
            // generate json

            final StringWriter s = new StringWriter();
            JsonGenerator json = DAO.jsonFactory.createGenerator(s);
            json.setPrettyPrinter(minified ? new MinimalPrettyPrinter() : new DefaultPrettyPrinter());

            json.writeStartObject();
            if (!minified) {
                json.writeObjectField("readme_0", "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!");
                json.writeObjectField("readme_1", "loklak.org is the framework for a message search system, not the portal, read: http://loklak.org/about.html#notasearchportal");
                json.writeObjectField("readme_2", "This is supposed to be the back-end of a search portal. For the api, see http://loklak.org/api.html");
                json.writeObjectField("readme_3", "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)");
            }
            json.writeObjectFieldStart("search_metadata");
            json.writeObjectField("itemsPerPage", Integer.toString(count));
            json.writeObjectField("count", Integer.toString(tl.size()));
            json.writeObjectField("hits", hits);
            json.writeObjectField("period", tl.period());
            json.writeObjectField("query", query);
            json.writeObjectField("client", post.getClientHost());
            json.writeObjectField("servicereduction", post.isDoS_servicereduction() ? "true" : "false");
            json.writeEndObject(); // of search_metadata
            json.writeArrayFieldStart("statuses");
            for (MessageEntry t: tl) {
                UserEntry u = tl.getUser(t);
                t.toJSON(json, u, true);
            }
            json.writeEndArray();
            
            // aggregations
            json.writeObjectFieldStart("aggregations");
            if (aggregations != null) {
                for (Map.Entry<String, List<Map.Entry<String, Long>>> aggregation: aggregations.entrySet()) {
                    json.writeObjectFieldStart(aggregation.getKey());
                    for (Map.Entry<String, Long> a: aggregation.getValue()) {
                        if (a.getValue().equals(query)) continue; // we omit obvious terms that cannot be used for faceting, like search for "#abc" -> most hashtag is "#abc"
                        json.writeObjectField(a.getKey(), a.getValue());
                    }
                    json.writeEndObject(); // of aggregation field
                }
            }
            json.writeEndObject(); // of aggregations
            json.writeEndObject(); // of root
            json.close();
            
            // write json
            ServletOutputStream sos = response.getOutputStream();
            if (jsonp) sos.print(callback + "(");
            sos.print(s.toString());
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
                m.setPubDate(t.getCreatedAtDate());
                m.setGuid(t.getIdStr());
                feed.addMessage(m);
            }
            String rss = feed.toString();
            //System.out.println("feed has " + feed.size() + " entries");
            
            // write xml
            response.getOutputStream().write(UTF8.getBytes(rss));
        }
        DAO.log(request.getServletPath() + "?" + request.getQueryString() + " -> " + tl.size() + " records returned");
        } catch (Throwable e) {
            Log.getLog().warn(e.getMessage(), e);
            //e.printStackTrace();
        }
    }
}
