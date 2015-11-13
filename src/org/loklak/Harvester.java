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
import java.util.HashSet;
import java.util.Set;

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

    private final static int FETCH_COUNT = 1000;
    private final static int FETCH_RANDOM = 3;
    private final static int HITS_LIMIT_4_QUERIES = 100;
    
    private static Set<String> pendingQueries = new HashSet<>();
    private static Set<String> pendingContext = new HashSet<>();
    private static Set<String> harvestedContext = new HashSet<>();

    private static int hitsOnBackend = 1000;
     
    public static class Ticket {
        public String q;
        public int count;
        public boolean synchronous;
        public Ticket(final String q, final int count, final boolean synchronous) {
            this.q = q; this.count = count; this.synchronous = synchronous;
        }
    }

    public static void checkContext(Timeline tl) {
        for (MessageEntry tweet: tl) {
            for (String user: tweet.getMentions()) checkContext("from:" + user);
            for (String hashtag: tweet.getHashtags()) checkContext(hashtag);
        }
    }
    public static void checkContext(String s) {
        if (!harvestedContext.contains(s)) pendingContext.add(s);
    }
    
    public static Ticket harvest() {
        String backend = DAO.getConfig("backend","http://loklak.org");
        
        if (pendingQueries.size() == 0 && hitsOnBackend < HITS_LIMIT_4_QUERIES && pendingContext.size() > 0) {
            // harvest using the collected keys instead using the queries
            String q = pendingContext.iterator().next();
            pendingContext.remove(q);
            harvestedContext.add(q);
            Timeline tl = TwitterScraper.search(q, true, true);
            if (tl == null || tl.size() == 0) return null;
            
            // find content query strings and store them in the context cache
            checkContext(tl);
            return new Ticket(q, tl.size(), false);
        }
        
        // load more queries if pendingQueries is empty
        if (pendingQueries.size() == 0) {
            try {
                ResultList<QueryEntry> rl = SuggestClient.suggest(backend, "", "query", FETCH_COUNT, "asc", "retrieval_next", DateParser.getTimezoneOffset(), null, "now", "retrieval_next", FETCH_RANDOM);
                for (QueryEntry qe: rl) {
                    pendingQueries.add(qe.getQuery());
                }
                hitsOnBackend = (int) rl.getHits();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        if (pendingQueries.size() == 0) return null;
        
        // take one of the pending queries or pending context and load the tweets
        String q = pendingQueries.iterator().next();
        pendingQueries.remove(q);
        Timeline tl = TwitterScraper.search(q, true, false);
        
        if (tl == null || tl.size() == 0) return null;
        
        // find content query strings and store them in the context cache
        checkContext(tl);
        
        // if we loaded a pending query, push results to backpeer right now
        tl.setQuery(q);
        boolean success = PushClient.push(new String[]{backend}, tl);
        if (success) return new Ticket(q, tl.size(), true);
        
        tl.setQuery(null);
        DAO.transmitTimeline(tl);
        return new Ticket(q, tl.size(), false);
    }
    
}
