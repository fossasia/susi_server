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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.loklak.api.client.PushClient;
import org.loklak.api.client.SuggestClient;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.QueryEntry;
import org.loklak.data.ResultList;
import org.loklak.data.Timeline;
import org.loklak.harvester.TwitterScraper;
import org.loklak.tools.DateParser;

public class Harvester {

    private final static int FETCH_RANDOM = 3;
    private final static int HITS_LIMIT_4_QUERIES = 100;
    private final static Random random = new Random(System.currentTimeMillis());
    public final static ExecutorService executor = Executors.newFixedThreadPool(2);
    
    private static Set<String> pendingQueries = new HashSet<>();
    private static List<String> pendingContext = new ArrayList<>();
    private static Set<String> harvestedContext = new HashSet<>();
    
    private static int hitsOnBackend = 1000;

    public static void checkContext(Timeline tl) {
        for (MessageEntry tweet: tl) {
            for (String user: tweet.getMentions()) checkContext("from:" + user);
            for (String hashtag: tweet.getHashtags()) checkContext(hashtag);
        }
    }
    public static void checkContext(String s) {
        if (!harvestedContext.contains(s) && !pendingContext.contains(s)) pendingContext.add(s);
    }
    
    public static int harvest() {
        String backend = DAO.getConfig("backend","http://loklak.org");
        
        if (random.nextInt(20) != 0 && hitsOnBackend < HITS_LIMIT_4_QUERIES && pendingQueries.size() == 0 && pendingContext.size() > 0) {
            // harvest using the collected keys instead using the queries
            int r = random.nextInt(pendingContext.size());
            String q = pendingContext.remove(r);
            pendingContext.remove(q);
            harvestedContext.add(q);
            Timeline tl = TwitterScraper.search(q, true, true);
            if (tl == null || tl.size() == 0) return -1;
            
            // find content query strings and store them in the context cache
            checkContext(tl);
            DAO.log("retrieval of " + tl.size() + " new messages for q = " + q + ", scheduled push");
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        if (pendingQueries.size() == 0) return -1;
        
        // take one of the pending queries or pending context and load the tweets
        String q = pendingQueries.iterator().next();
        pendingQueries.remove(q);
        Timeline tl = TwitterScraper.search(q, true, false);
        
        if (tl == null || tl.size() == 0) return -1;
        
        // find content query strings and store them in the context cache
        checkContext(tl);
        
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
                        DAO.log("retrieval of " + tl.size() + " new messages for q = " + tl.getQuery() + ", pushed to backend synchronously in " + (System.currentTimeMillis() - start) + " ms");
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
            Caretaker.transmitTimeline(tl);
            DAO.log("retrieval of " + tl.size() + " new messages for q = " + q + ", scheduled push");
        }
        
    }
    
}
