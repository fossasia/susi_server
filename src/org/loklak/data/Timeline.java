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

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jetty.util.log.Log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * A timeline is a structure which holds tweet for the purpose of presentation
 * There is no tweet retrieval method here, just an iterator which returns the tweets in reverse appearing order
 */
public class Timeline implements Iterable<MessageEntry> {

    private TreeMap<String, MessageEntry> tweets; // the key is the date plus id of the tweet
    private Map<String, UserEntry> users;
    
    public Timeline() {
        this.tweets = new TreeMap<String, MessageEntry>();
        this.users = new HashMap<String, UserEntry>();
    }
    
    public int size() {
        return this.tweets.size();
    }
    
    public void addUser(UserEntry user) {
        assert user != null;
        if (user != null) this.users.put(user.getScreenName(), user);
    }
    
    public void addTweet(MessageEntry tweet) {
        this.tweets.put(Long.toHexString(tweet.getCreatedAt().getTime()) + "_" + tweet.getIdStr(), tweet);
    }

    protected UserEntry getUser(String user_screen_name) {
        return this.users.get(user_screen_name);
    }
    
    public UserEntry getUser(MessageEntry fromTweet) {
        return this.users.get(fromTweet.getScreenName());
    }
    
    public void putAll(Timeline other) {
        for (MessageEntry t: other) this.addTweet(t);
        for (Map.Entry<String, UserEntry> u: other.users.entrySet()) {
            if (!this.users.containsKey(u.getKey()) || u.getValue().containsProfileImage()) {
                this.users.put(u.getKey(), u.getValue());
            }
        }
    }
    
    public MessageEntry getOldestTweet() {
        return this.tweets.firstEntry().getValue();
    }
    
    public MessageEntry getLatestTweet() {
        return this.tweets.lastEntry().getValue();
    }
    
    public String toString() {
        return toJSON(true);
    }
    
    public String toJSON(boolean withEnrichedData) {
        // generate json
        try {
            final StringWriter s = new StringWriter();
            JsonGenerator json = DAO.jsonFactory.createGenerator(s);
            json.setPrettyPrinter(new DefaultPrettyPrinter());
            json.writeStartObject(); // start root object
            json.writeObjectFieldStart("search_metadata");
            json.writeObjectField("count", Integer.toString(this.tweets.size()));
            json.writeEndObject(); // of search_metadata
            json.writeArrayFieldStart("statuses");
            for (MessageEntry t: this) {
                UserEntry u = this.users.get(t.getScreenName());
                t.toJSON(json, u, withEnrichedData);
            }
            json.writeEndArray();
            json.writeEndObject(); // of root
            json.close(); // finish writing and close StringWriter
            
            // write json
            return s.toString();
        } catch (IOException e) {
            Log.getLog().warn(e);
            return null;
        }
    }
    
    /**
     * the tweet iterator returns tweets in descending appearance order
     */
    @Override
    public Iterator<MessageEntry> iterator() {
        return this.tweets.descendingMap().values().iterator();
    }

    public long period() {
        if (this.size() < 1) return Long.MAX_VALUE;
        
        // first we try to calculate the period time based on the current time.
        // That may fail if the current time is not set correctly!
        long timeInterval = System.currentTimeMillis() - this.getOldestTweet().created_at.getTime();
        long p = 1 + timeInterval / this.size();
        if (p >= 10000) return p;

        if (this.size() < 2) return Long.MAX_VALUE;
        timeInterval = this.getLatestTweet().created_at.getTime() - this.getOldestTweet().created_at.getTime();
        return 1 + timeInterval / (this.size() - 1);
    }    
    
}
