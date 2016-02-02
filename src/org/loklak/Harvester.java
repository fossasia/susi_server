/**
 *  Harvester
 *  Copyright 13.11.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.loklak.api.client.PushClient;
import org.loklak.api.client.SearchClient;
import org.loklak.api.client.SuggestClient;
import org.loklak.data.DAO;
import org.loklak.harvester.TwitterScraper;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.ResultList;
import org.loklak.objects.Timeline;
import org.loklak.objects.Timeline.Order;
import org.loklak.tools.DateParser;

public class Harvester {

    private final static int FETCH_RANDOM = 3;
    private final static int HITS_LIMIT_4_QUERIES = 100;
    private final static int MAX_PENDING = 200; // this could be much larger but we don't want to cache too many of these
    private final static int MAX_HARVESTED = 10000; // just to prevent a memory leak with possible OOM after a long time we flush that cache after a while
    private final static Random random = new Random(System.currentTimeMillis());
    public final static ExecutorService executor = Executors.newFixedThreadPool(1);
    
    private static LinkedHashSet<String> pendingQueries = new LinkedHashSet<>();
    private static ArrayList<String> pendingContext = new ArrayList<>();
    private static Set<String> harvestedContext = new HashSet<>();
    
    private static int hitsOnBackend = 1000;

    public static void checkContext(Timeline tl, boolean front) {
        for (MessageEntry tweet: tl) {
            for (String user: tweet.getMentions()) checkContext("from:" + user, front);
            for (String hashtag: tweet.getHashtags()) checkContext(hashtag, front);
        }
    }
    public static void checkContext(String s, boolean front) {
        if (!front && pendingContext.size() > MAX_PENDING) return; // queue is full
        if (!harvestedContext.contains(s) && !pendingContext.contains(s)) {
            if (front) pendingContext.add(0, s); else pendingContext.add(s);
        }
        while (pendingContext.size() > MAX_PENDING) pendingContext.remove(pendingContext.size() - 1);
        if (harvestedContext.size() > MAX_HARVESTED) harvestedContext.clear();
    }
    
    public static int harvest() {
        String backend = DAO.getConfig("backend","http://loklak.org");
        
        if (random.nextInt(100) != 0 && hitsOnBackend < HITS_LIMIT_4_QUERIES && pendingQueries.size() == 0 && pendingContext.size() > 0) {
            // harvest using the collected keys instead using the queries
            int r = random.nextInt((pendingContext.size() / 2) + 1);
            String q = pendingContext.remove(r);
            harvestedContext.add(q);
            Timeline tl = TwitterScraper.search(q, Timeline.Order.CREATED_AT, true, true, 400);
            if (tl == null || tl.size() == 0) return -1;
            
            // find content query strings and store them in the context cache
            checkContext(tl, false);
            DAO.log("retrieval of " + tl.size() + " new messages for q = " + q + ", scheduled push; pendingQueries = " + pendingQueries.size() + ", pendingContext = " + pendingContext.size() + ", harvestedContext = " + harvestedContext.size());
            return tl.size();
        }
        
        // load more queries if pendingQueries is empty
        if (pendingQueries.size() == 0) {
            try {
                ResultList<QueryEntry> rl = SuggestClient.suggest(backend, "", "query", Math.max(FETCH_RANDOM * 30, hitsOnBackend / 10), "asc", "retrieval_next", DateParser.getTimezoneOffset(), null, "now", "retrieval_next", FETCH_RANDOM);
                for (QueryEntry qe: rl) {
                    pendingQueries.add(qe.getQuery());
                }
                hitsOnBackend = (int) rl.getHits();
                if (hitsOnBackend == 0) {
                    // the backend does not have any new query words for this time.
                    if (pendingContext.size() == 0) {
                        // try to fill the pendingContext using a matchall-query from the cache
                        // http://loklak.org/api/search.json?source=cache&q=
                        Timeline tl = SearchClient.search(backend, "", Timeline.Order.CREATED_AT, "cache", 100, 0, SearchClient.backend_hash, 60000);
                        checkContext(tl, false);
                    }
                    // if we still don't have any context, we are a bit helpless and hope that this situation
                    // will be better in the future. To prevent that this is called excessively fast, do a pause.
                    if (pendingContext.size() == 0) try {Thread.sleep(10000);} catch (InterruptedException e) {}
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        if (pendingQueries.size() == 0) return -1;
        
        // take one of the pending queries or pending context and load the tweets
        String q = pendingQueries.iterator().next();
        pendingQueries.remove(q);
        pendingContext.remove(q);
        harvestedContext.add(q);
        Timeline tl = TwitterScraper.search(q, Timeline.Order.CREATED_AT, true, false, 400);
        
        if (tl == null || tl.size() == 0) {
            // even if the result is empty, we must push this to the backend to make it possible that the query gets an update
            if (tl == null) tl = new Timeline(Order.CREATED_AT);
            tl.setQuery(q);
            PushThread pushThread = new PushThread(backend, tl);
            executor.execute(pushThread);
            return -1;
        }
        
        // find content query strings and store them in the context cache
        checkContext(tl, true);
        
        // if we loaded a pending query, push results to backpeer right now
        tl.setQuery(q);
        PushThread pushThread = new PushThread(backend, tl);
        executor.execute(pushThread);
        return tl.size();
    }
    
    private static class PushThread implements Runnable {
        private String peer;
        private Timeline tl;
        public PushThread(String peer, Timeline tl) {
            this.peer = peer;
            this.tl = tl;
        }
        @Override
        public void run() {
            boolean success = false;
            for (int i = 0; i < 5; i++) {
                try {
                    long start = System.currentTimeMillis();
                    success = PushClient.push(new String[]{peer}, tl);
                    if (success) {
                        DAO.log("retrieval of " + tl.size() + " new messages for q = " + tl.getQuery() + ", pushed to backend synchronously in " + (System.currentTimeMillis() - start) + " ms; pendingQueries = " + pendingQueries.size() + ", pendingContext = " + pendingContext.size() + ", harvestedContext = " + harvestedContext.size());
                        return;
                    }
                } catch (Throwable e) {
                    //e.printStackTrace();
                    DAO.log("failed synchronous push to backend, attempt " + i);
                    try {Thread.sleep((i + 1) * 3000);} catch (InterruptedException e1) {}
                }
            }
            String q = tl.getQuery();
            tl.setQuery(null);
            Caretaker.transmitTimelineToBackend(tl);
            DAO.log("retrieval of " + tl.size() + " new messages for q = " + q + ", scheduled push");
        }
        
    }
    
}
