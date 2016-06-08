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

package org.loklak.api.search;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.harvester.TwitterScraper;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.rss.RSSFeed;
import org.loklak.rss.RSSMessage;
import org.loklak.server.Query;
import org.loklak.tools.CharacterCoding;
import org.loklak.tools.UTF8;

/**
 * The search servlet. we provide opensearch/rss and twitter-like JSON as result.
 */
public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 563533152152063908L;

    private final static int SEARCH_LOW_COUNT = 10;
    private final static int SEARCH_DEFAULT_COUNT = 100;
    private final static int SEARCH_MAX_PUBLIC_COUNT = 1000;
    private final static int SEARCH_MAX_LOCALHOST_COUNT = 10000;

    private final static int SEARCH_CACHE_THREASHOLD_TIME = 3000;
    
    private final static AtomicLong last_cache_search_time = new AtomicLong(10L);

    public final static String backend_hash = Integer.toHexString(Integer.MAX_VALUE);
    public final static String frontpeer_hash = Integer.toHexString(Integer.MAX_VALUE - 1);

    // possible values: cache, twitter, all
    public static Timeline search(final String protocolhostportstub, final String query, final Timeline.Order order, final String source, final int count, final int timezoneOffset, final String provider_hash, final long timeout) throws IOException {
        Timeline tl = new Timeline(order);
        String urlstring = "";
        try {
            urlstring = protocolhostportstub + "/api/search.json?q=" + URLEncoder.encode(query.replace(' ', '+'), "UTF-8") + "&timezoneOffset=" + timezoneOffset + "&maximumRecords=" + count + "&source=" + (source == null ? "all" : source) + "&minified=true&timeout=" + timeout;
            byte[] jsonb = ClientConnection.downloadPeer(urlstring);
            if (jsonb == null || jsonb.length == 0) throw new IOException("empty content from " + protocolhostportstub);
            String jsons = UTF8.String(jsonb);
            JSONObject json = new JSONObject(jsons);
            if (json == null || json.length() == 0) return tl;
            JSONArray statuses = json.getJSONArray("statuses");
            if (statuses != null) {
                for (int i = 0; i < statuses.length(); i++) {
                    JSONObject tweet = statuses.getJSONObject(i);
                    JSONObject user = tweet.getJSONObject("user");
                    if (user == null) continue;
                    tweet.remove("user");
                    UserEntry u = new UserEntry(user);
                    MessageEntry t = new MessageEntry(tweet);
                    tl.add(t, u);
                }
            }
            JSONObject metadata = json.getJSONObject("search_metadata");
            if (metadata != null) {
                Integer hits = metadata.has("hits") ? (Integer) metadata.get("hits") : null;
                if (hits != null) tl.setHits(hits.intValue());
                String scraperInfo = metadata.has("scraperInfo") ? (String) metadata.get("scraperInfo") : null;
                if (scraperInfo != null) tl.setScraperInfo(scraperInfo);
            }
        } catch (Throwable e) {
        	Log.getLog().warn(e);
            throw new IOException(e.getMessage());
        }
        //System.out.println(parser.text());
        return tl;
    }
    
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final Query post = RemoteAccess.evaluate(request);
        try {
        
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your (" + post.getClientHost() + ") request frequency is too high"); return;}
        
        // check call type
        boolean jsonExt = request.getServletPath().endsWith(".json");
        boolean rssExt = request.getServletPath().endsWith(".rss");
        boolean txtExt = request.getServletPath().endsWith(".txt");

        // evaluate get parameter
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        String query = post.get("q", "");
        if (query == null || query.length() == 0) query = post.get("query", "");
        query = CharacterCoding.html2unicode(query).replaceAll("\\+", " ");
        final long timeout = (long) post.get("timeout", DAO.getConfig("search.timeout", 2000));
        final int count = post.isDoS_servicereduction() ? SEARCH_LOW_COUNT : Math.min(post.get("count", post.get("maximumRecords", SEARCH_DEFAULT_COUNT)), post.isLocalhostAccess() ? SEARCH_MAX_LOCALHOST_COUNT : SEARCH_MAX_PUBLIC_COUNT);
        String source = post.isDoS_servicereduction() ? "cache" : post.get("source", "all"); // possible values: cache, backend, twitter, all
        int agregation_limit = post.get("limit", 100);
        String[] fields = post.get("fields", new String[0], ",");
        int timezoneOffset = post.get("timezoneOffset", 0);
        if (query.indexOf("id:") >= 0 && ("all".equals(source) || "twitter".equals(source))) source = "cache"; // id's cannot be retrieved from twitter with the scrape-api (yet), only from the cache
        final String ordername = post.get("order", Timeline.Order.CREATED_AT.getMessageFieldName());
        final Timeline.Order order = Timeline.parseOrder(ordername);
        
        // create tweet timeline
        final Timeline tl = new Timeline(order);
        Map<String, List<Map.Entry<String, Long>>> aggregations = null;
        final QueryEntry.Tokens tokens = new QueryEntry.Tokens(query);
        
        final AtomicInteger cache_hits = new AtomicInteger(0), count_backend = new AtomicInteger(0), count_twitter_all = new AtomicInteger(0), count_twitter_new = new AtomicInteger(0);
        final boolean backend_push = DAO.getConfig("backend.push.enabled", false);

        if ("all".equals(source)) {
            // start all targets for search concurrently
            final int timezoneOffsetf = timezoneOffset;
            final String queryf = query;
            final long start = System.currentTimeMillis();
            
            // start a scraper
            Thread scraperThread = tokens.raw.length() == 0 ? null : new Thread() {
                public void run() {
                    final String scraper_query = tokens.translate4scraper();
                    DAO.log(request.getServletPath() + " scraping with query: " + scraper_query);
                    Timeline twitterTl = DAO.scrapeTwitter(post, scraper_query, order, timezoneOffsetf, true, timeout, true);
                    count_twitter_new.set(twitterTl.size());
                    tl.putAll(QueryEntry.applyConstraint(twitterTl, tokens, false)); // pre-localized results are not filtered with location constraint any more 
                    tl.setScraperInfo(twitterTl.getScraperInfo());
                    post.recordEvent("twitterscraper_time", System.currentTimeMillis() - start);
                }
            };
            if (scraperThread != null) scraperThread.start();

            // start a local search
            Thread localThread = queryf == null || queryf.length() == 0 ? null : new Thread() {
                public void run() {
                    DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(queryf, order, timezoneOffsetf, last_cache_search_time.get() > SEARCH_CACHE_THREASHOLD_TIME ? SEARCH_LOW_COUNT : count, 0);
                    long time = System.currentTimeMillis() - start;
                    last_cache_search_time.set(time);
                    post.recordEvent("cache_time", time);
                    cache_hits.set(localSearchResult.timeline.getHits());
                    tl.putAll(localSearchResult.timeline);
                }
            };
            if (localThread != null) localThread.start();
            
            // start a backend search, but only if backend_push == true or result from scraper is too bad
            boolean start_backend_thread = false;
            if (backend_push) start_backend_thread = true; else {
                // wait now for termination of scraper thread and local search
                // to evaluate how many results are available
                if (scraperThread != null) try {scraperThread.join(Math.max(10000, timeout - System.currentTimeMillis() + start));} catch (InterruptedException e) {}
                if (localThread != null)  try {localThread.join(Math.max(100, timeout - System.currentTimeMillis() + start));} catch (InterruptedException e) {}
                localThread = null; scraperThread = null;
                if (tl.size() < count) start_backend_thread = true;
            }
            Thread backendThread = tokens.original.length() == 0 || !start_backend_thread ? null : new Thread() {
                public void run() {
                    Timeline backendTl = DAO.searchBackend(tokens.original, order, count, timezoneOffsetf, "cache", timeout);
                    if (backendTl != null) {
                        tl.putAll(QueryEntry.applyConstraint(backendTl, tokens, true));
                        count_backend.set(tl.size());
                        // TODO: read and aggregate aggregations from backend as well
                    }
                    post.recordEvent("backend_time", System.currentTimeMillis() - start);
                }
            };
            if (backendThread != null) backendThread.start();
            
            // wait for termination of all threads
            if (scraperThread != null) try {scraperThread.join(Math.max(10000, timeout - System.currentTimeMillis() + start));} catch (InterruptedException e) {}
            if (localThread != null)  try {localThread.join(Math.max(100, timeout - System.currentTimeMillis() + start));} catch (InterruptedException e) {}
            if (backendThread != null) try {backendThread.join(Math.max(100, timeout - System.currentTimeMillis() + start));} catch (InterruptedException e) {}
            
        } else if ("twitter".equals(source) && tokens.raw.length() > 0) {
            final long start = System.currentTimeMillis();
            final String scraper_query = tokens.translate4scraper();
            DAO.log(request.getServletPath() + " scraping with query: " + scraper_query);
            Timeline twitterTl = DAO.scrapeTwitter(post, scraper_query, order, timezoneOffset, true, timeout, true);
            count_twitter_new.set(twitterTl.size());
            tl.putAll(QueryEntry.applyConstraint(twitterTl, tokens, false)); // pre-localized results are not filtered with location constraint any more 
            tl.setScraperInfo(twitterTl.getScraperInfo());
            post.recordEvent("twitterscraper_time", System.currentTimeMillis() - start);
            // in this case we use all tweets, not only the latest one because it may happen that there are no new and that is not what the user expects
        
        } else if ("cache".equals(source)) {
            final long start = System.currentTimeMillis();
            DAO.SearchLocalMessages localSearchResult = new DAO.SearchLocalMessages(query, order, timezoneOffset, last_cache_search_time.get() > SEARCH_CACHE_THREASHOLD_TIME ? SEARCH_LOW_COUNT : count, agregation_limit, fields);
            cache_hits.set(localSearchResult.timeline.getHits());
            tl.putAll(localSearchResult.timeline);
            aggregations = localSearchResult.aggregations;
            long time = System.currentTimeMillis() - start;
            last_cache_search_time.set(time);
            post.recordEvent("cache_time", time);
            
        } else if ("backend".equals(source) && query.length() > 0) {
            final long start = System.currentTimeMillis();
            Timeline backendTl = DAO.searchBackend(query, order, count, timezoneOffset, "cache", timeout);
            if (backendTl != null) {
                tl.putAll(QueryEntry.applyConstraint(backendTl, tokens, true));
                tl.setScraperInfo(backendTl.getScraperInfo());
                // TODO: read and aggregate aggregations from backend as well
                count_backend.set(tl.size());
            }
            post.recordEvent("backend_time", System.currentTimeMillis() - start);
       
        }

        final long start = System.currentTimeMillis();
        // check the latest user_ids
        DAO.announceNewUserId(tl);
        
        // reduce the list to the wanted number of results if we have more
        tl.reduceToMaxsize(count);
        
        // create json or xml according to path extension
        int shortlink_iflinkexceedslength = (int) DAO.getConfig("shortlink.iflinkexceedslength", 500L);
        String shortlink_urlstub = DAO.getConfig("shortlink.urlstub", "http://localhost:9000");
        if (jsonExt) {
            post.setResponse(response, jsonp ? "application/javascript": "application/json");
            // generate json
            JSONObject m = new JSONObject(true);
            JSONObject metadata = new JSONObject(true);
            if (!minified) {
                m.put("readme_0", "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!");
                m.put("readme_1", "loklak.org is the framework for a message search system, not the portal, read: http://loklak.org/about.html#notasearchportal");
                m.put("readme_2", "This is supposed to be the back-end of a search portal. For the api, see http://loklak.org/api.html");
                m.put("readme_3", "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)");
            }
            metadata.put("itemsPerPage", Integer.toString(count));
            metadata.put("count", Integer.toString(tl.size()));
            metadata.put("count_twitter_all", count_twitter_all.get());
            metadata.put("count_twitter_new", count_twitter_new.get());
            metadata.put("count_backend", count_backend.get());
            metadata.put("count_cache", cache_hits.get());
            metadata.put("hits", Math.max(cache_hits.get(), tl.size()));
            if (order == Timeline.Order.CREATED_AT) metadata.put("period", tl.period());
            metadata.put("query", query);
            metadata.put("client", post.getClientHost());
            metadata.put("time", System.currentTimeMillis() - post.getAccessTime());
            metadata.put("servicereduction", post.isDoS_servicereduction() ? "true" : "false");
            if (tl.getScraperInfo().length() > 0) metadata.put("scraperInfo", tl.getScraperInfo());
            m.put("search_metadata", metadata);
            JSONArray statuses = new JSONArray();
            try {
                for (MessageEntry t: tl) {
                    UserEntry u = tl.getUser(t);
                    if (DAO.getConfig("flag.fixunshorten", false)) t.setText(TwitterScraper.unshorten(t.getText(shortlink_iflinkexceedslength, shortlink_urlstub)));
                    statuses.put(t.toJSON(u, true, shortlink_iflinkexceedslength, shortlink_urlstub));
                }
            } catch (ConcurrentModificationException e) {
                // late incoming messages from concurrent peer retrieval may cause this
                // we silently do nothing here and return what we listed so far
            }
            m.put("statuses", statuses);
            
            // aggregations
            JSONObject agg = new JSONObject(true);
            if (aggregations != null) {
                for (Map.Entry<String, List<Map.Entry<String, Long>>> aggregation: aggregations.entrySet()) {
                    JSONObject facet = new JSONObject(true);
                    for (Map.Entry<String, Long> a: aggregation.getValue()) {
                        if (a.getValue().equals(query)) continue; // we omit obvious terms that cannot be used for faceting, like search for "#abc" -> most hashtag is "#abc"
                        facet.put(a.getKey(), a.getValue());
                    }
                    agg.put(aggregation.getKey(), facet);
                }
            }
            m.put("aggregations", agg);
            
            // write json
            response.setCharacterEncoding("UTF-8");
            PrintWriter sos = response.getWriter();
            if (jsonp) sos.print(callback + "(");
            sos.print(m.toString(minified ? 0 : 2));
            if (jsonp) sos.println(");");
            sos.println();
        } else if (rssExt) {
            response.setCharacterEncoding("UTF-8");
            post.setResponse(response, "application/rss+xml;charset=utf-8");
            // generate xml
            RSSMessage channel = new RSSMessage();
            channel.setPubDate(new Date());
            channel.setTitle("RSS feed for Twitter search for " + query);
            channel.setDescription("");
            channel.setLink("");
            RSSFeed feed = new RSSFeed(tl.size());
            feed.setChannel(channel);
            try {
                for (MessageEntry t: tl) {
                    UserEntry u = tl.getUser(t);
                    RSSMessage m = new RSSMessage();
                    m.setLink(t.getStatusIdUrl().toExternalForm());
                    m.setAuthor(u.getName() + " @" + u.getScreenName());
                    m.setTitle(u.getName() + " @" + u.getScreenName());
                    m.setDescription(t.getText(shortlink_iflinkexceedslength, shortlink_urlstub));
                    m.setPubDate(t.getCreatedAt());
                    m.setGuid(t.getIdStr());
                    feed.addMessage(m);
                }
            } catch (ConcurrentModificationException e) {
                // late incoming messages from concurrent peer retrieval may cause this
                // we silently do nothing here and return what we listed so far
            }
            String rss = feed.toString();
            //System.out.println("feed has " + feed.size() + " entries");
            
            // write xml
            response.getOutputStream().write(UTF8.getBytes(rss));
        } else if (txtExt) {
            post.setResponse(response, "text/plain");
            final StringBuilder buffer = new StringBuilder(1000);
            try {
                for (MessageEntry t: tl) {
                    UserEntry u = tl.getUser(t);
                    buffer.append(t.getCreatedAt()).append(" ").append(u.getScreenName()).append(": ").append(t.getText(shortlink_iflinkexceedslength, shortlink_urlstub)).append('\n');
                }
            } catch (ConcurrentModificationException e) {
                // late incoming messages from concurrent peer retrieval may cause this
                // we silently do nothing here and return what we listed so far
            }
            response.getOutputStream().write(UTF8.getBytes(buffer.toString()));
        }
        post.recordEvent("result_count", tl.size());
        post.recordEvent("postprocessing_time", System.currentTimeMillis() - start);
        JSONObject hits = new JSONObject(true);
        hits.put("count_twitter_all", count_twitter_all.get());
        hits.put("count_twitter_new", count_twitter_new.get());
        hits.put("count_backend", count_backend.get());
        hits.put("cache_hits", cache_hits.get());
        post.recordEvent("hits", hits);
        DAO.log(request.getServletPath() + "?" + request.getQueryString() + " -> " + tl.size() + " records returned, " +  count_twitter_new.get() + " new");
        post.finalize();
        } catch (Throwable e) {
            Log.getLog().warn(e.getMessage(), e);
            //Log.getLog().warn(e);
        }
    }

    public static void main(String[] args) {
        try {
            Timeline tl = search("http://loklak.org", "beer", Timeline.Order.CREATED_AT, "cache", 20, -120, backend_hash, 10000);
            System.out.println(tl.toJSON(false).toString(2));
        } catch (IOException e) {
        	Log.getLog().warn(e);
        }
    }
}
