package org.loklak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.loklak.data.DAO;
import org.loklak.data.Timeline;
import org.loklak.data.MessageEntry;
import org.loklak.tools.DateParser;

public class Crawler {

    private static class Term {
        public String query;
        public int depth;
        public boolean followHashtags;
        public boolean followUsers;
        
        public Term(String query, int depth, boolean followHashtags, boolean followUsers) {
            this.query = query;
            this.depth = depth;
            this.followHashtags = followHashtags;
            this.followUsers = followUsers;
        }
    }
    
    private static BlockingDeque<Term> pending = new LinkedBlockingDeque<Term>();
    private static Map<String, Long> stacked = new ConcurrentHashMap<String, Long>();
    
    public static boolean stack(String query, int depth, boolean followHashtags, boolean followUsers, boolean atfront) {
        // remove old queries
        if (pending.size() == 0) {
            // remove old entries
            Iterator<Map.Entry<String, Long>> i = stacked.entrySet().iterator();
            long timeout = System.currentTimeMillis() - DateParser.HOUR_MILLIS; // 1 hour: a user rarely posts more than 20 tweets an hour, so this should be sufficient
            while (i.hasNext()) {
                if (i.next().getValue().longValue() < timeout) i.remove();
            }
        }
        if (stacked.containsKey(query)) return false;
        stacked.put(query, System.currentTimeMillis());
        Term nextTerm = new Term(query, Math.max(0, Math.min(4, depth)), followHashtags, followUsers);
        if (atfront) pending.addFirst(nextTerm); else pending.addLast(nextTerm);
        return true;
    }
    
    public static int process() {
        // take a term from the stack
        if (pending.size() == 0) return 0;
        Term term;
        try {
            term = pending.take();
        } catch (InterruptedException e) {
            return 0;
        }
        
        // execute the query
        Timeline tl = DAO.scrapeTwitter(term.query, Timeline.Order.CREATED_AT, 0, false)[1]; // we use only the new tweets, not old/known
        
        // if depth of query was 0, terminate
        if (term.depth == 0) return 0;
        
        // take hashtags and users from result
        Set<String> newqueries = new HashSet<String>();
        for (MessageEntry t: tl) {
            // follow users and hashtags which appear in the tweet
            if (term.followUsers) for (String user: t.getMentions()) if (user.length() >= 2) newqueries.add(user);
            if (term.followHashtags) for (String hashtag: t.getHashtags()) if (hashtag.length() >= 2) newqueries.add(hashtag);
            
            // we always follow the users which are the authors of the tweets
            newqueries.add(t.getScreenName());
        }
        
        // put the hashtags and users on the stack with reduced depth
        int count = 0;
        for (String query: newqueries) {
            if (stack(query, term.depth - 1, term.followHashtags, term.followUsers, false)) count++;
        }
        
        // return the number of new terms on the crawl stack
        return count;
    }

    public static void toJSON(XContentBuilder m) {
        ArrayList<String> pendingQueries = new ArrayList<String>();
        Set<String> processedQueries = new HashSet<String>(); processedQueries.addAll(stacked.keySet());
        for (Term t: pending) {pendingQueries.add(t.query); processedQueries.remove(t.query);}
        
        try {
            m.startObject();
            m.field("pending_size", pending.size());
            m.field("stacked_size", stacked.size());
            m.field("processed_size", processedQueries.size());
            
            m.field("pending", pendingQueries.toArray(new String[pendingQueries.size()]));
            m.field("processed", processedQueries.toArray(new String[processedQueries.size()]));
            m.endObject();
        } catch (IOException e) {
        }
    }
}
