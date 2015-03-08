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

package org.loklak;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * A timeline is a structure which holds tweet for the purpose of presentation
 * There is no tweet retrieval method here, just an iterator which returns the tweets in reverse appearing order
 */
public class Timeline implements Iterable<Tweet> {

    private TreeMap<String, Tweet> tweets; // the key is the date plus id of the tweet
    private Map<String, User> users;
    
    public Timeline() {
        this.tweets = new TreeMap<String, Tweet>();
        this.users = new HashMap<String, User>();
    }
    
    public int size() {
        return this.tweets.size();
    }
    
    public void addUser(User user) {
        assert user != null;
        if (user != null) this.users.put(user.getScreenName(), user);
    }
    
    public void addTweet(Tweet tweet) {
        this.tweets.put(Long.toHexString(tweet.getCreatedAtDate().getTime()) + "_" + tweet.getIdStr(), tweet);
    }

    protected User getUser(String user_screen_name) {
        return this.users.get(user_screen_name);
    }
    
    public User getUser(Tweet fromTweet) {
        return this.users.get(fromTweet.getUserScreenName());
    }
    
    public void putAll(Timeline other) {
        for (Tweet t: other) this.addTweet(t);
        this.users.putAll(other.users);
    }
    
    public String toJSON(boolean withEnrichedData) {
        // generate json
        XContentBuilder json;
        try {
            json = XContentFactory.jsonBuilder();
            json.startObject();
            json.field("search_metadata").startObject();
            json.field("count", Integer.toString(this.tweets.size()));
            json.endObject(); // of search_metadata
            json.field("statuses").startArray();
            for (Tweet t: this) {
                User u = this.users.get(t.getUserScreenName());
                t.toJSON(json, u, withEnrichedData);
            }
            json.endArray();
            json.endObject(); // of root

            // write json
            return json.string();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * the tweet iterator returns tweets in descending appearance order
     */
    @Override
    public Iterator<Tweet> iterator() {
        return this.tweets.descendingMap().values().iterator();
    }

}
