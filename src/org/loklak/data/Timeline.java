/**
 *  Timeline
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

package org.loklak.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A timeline is a structure which holds tweet for the purpose of presentation
 * There is no tweet retrieval method here, just an iterator which returns the tweets in reverse appearing order
 */
public class Timeline implements Iterable<MessageEntry> {

    public static enum Order {
        CREATED_AT,
        RETWEET_COUNT,
        FAVOURITES_COUNT;
            
        public String getMessageFieldName() {
            return this.name().toLowerCase();
        }
    }
    
    private TreeMap<String, MessageEntry> tweets; // the key is the date plus id of the tweet
    private Map<String, UserEntry> users;
    final private Order order;
    
    public Timeline(Order order) {
        this.tweets = new TreeMap<String, MessageEntry>();
        this.users = new HashMap<String, UserEntry>();
        this.order = order;
    }
    
    public static Order parseOrder(String order) {
        try {
            return Order.valueOf(order.toUpperCase());
        } catch (Throwable e) {
            return Order.CREATED_AT;
        }
    }
    
    public Order getOrder() {
        return this.order;
    }
    
    public int size() {
        return this.tweets.size();
    }
    
    public void reduceToMaxsize(final int maxsize) {
        if (maxsize < 0) return;
        while (this.tweets.size() > maxsize) this.tweets.remove(this.tweets.firstEntry().getKey());
    }
    
    public void addUser(UserEntry user) {
        assert user != null;
        if (user != null) this.users.put(user.getScreenName(), user);
    }
    
    public void addTweet(MessageEntry tweet) {
        String key = "";
        if (this.order == Order.RETWEET_COUNT) {
            key = Long.toHexString(tweet.getRetweetCount());
            while (key.length() < 16) key = "0" + key;
            key = key + "_" + tweet.getIdStr();
        } else if (this.order == Order.FAVOURITES_COUNT) {
            key = Long.toHexString(tweet.getFavouritesCount());
            while (key.length() < 16) key = "0" + key;
            key = key + "_" + tweet.getIdStr();
        } else {
            key = Long.toHexString(tweet.getCreatedAt().getTime()) + "_" + tweet.getIdStr();
        }
        this.tweets.put(key, tweet);
    }

    protected UserEntry getUser(String user_screen_name) {
        return this.users.get(user_screen_name);
    }
    
    public UserEntry getUser(MessageEntry fromTweet) {
        return this.users.get(fromTweet.getScreenName());
    }
    
    public void putAll(Timeline other) {
        assert this.order.equals(other.order);
        for (MessageEntry t: other) this.addTweet(t);
        for (Map.Entry<String, UserEntry> u: other.users.entrySet()) {
            UserEntry t = this.users.get(u.getKey());
            if (t == null || !t.containsProfileImage()) {
                this.users.put(u.getKey(), u.getValue());
            }
        }
    }
    
    public MessageEntry getBottomTweet() {
        return this.tweets.firstEntry().getValue();
    }
    
    public MessageEntry getTopTweet() {
        return this.tweets.lastEntry().getValue();
    }
    
    public String toString() {
        try {
            return new ObjectMapper().writer().writeValueAsString(toMap(true));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public Map<String, Object> toMap(boolean withEnrichedData) {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("count", Integer.toString(this.tweets.size()));
        m.put("search_metadata", metadata);
        List<Object> statuses = new ArrayList<>();
        for (MessageEntry t: this) {
            UserEntry u = this.users.get(t.getScreenName());
            statuses.add(t.toMap(u, withEnrichedData));
        }
        m.put("statuses", statuses);
        return m;
    }
    
    /**
     * the tweet iterator returns tweets in descending appearance order (top first)
     */
    @Override
    public Iterator<MessageEntry> iterator() {
        return this.tweets.descendingMap().values().iterator();
    }

    public long period() {
        if (this.size() < 1) return Long.MAX_VALUE;
        if (this.size() < 2) {
            // try to calculate the period time based on the current time.
            // That may fail if the current time is not set correctly!
            long timeInterval = System.currentTimeMillis() - this.getBottomTweet().created_at.getTime();
            long p = 1 + timeInterval / this.size();
            return p < 4000 ? p / 4 + 3000 : p;
        }
        
        // calculate the time based on the latest 20 tweets (or less)
        long first = 0;
        long last = 0;
        int count = 0;
        for (MessageEntry messageEntry: this) {
            if (first == 0) {first = messageEntry.created_at.getTime(); continue;}
            last = messageEntry.created_at.getTime();
            count++;
            if (count >= 19) break;
        }

        if (count == 0) return 60000;
        long timeInterval = first - last;
        long p = 1 + timeInterval / count;
        return p < 4000 ? p / 4 + 3000 : p;
    }    
    
}
