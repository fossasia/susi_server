/**
 *  TwitterAPI
 *  Copyright 16.07.2015 by Michael Peter Christen, @0rb1t3r
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


package org.loklak.harvester;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.data.AbstractIndexEntry;
import org.loklak.data.AccountEntry;
import org.loklak.data.DAO;
import org.loklak.geo.GeoMark;

import twitter4j.IDs;
import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterAPI {

    private static TwitterFactory appFactory = null;
    private static Map<String, TwitterFactory> userFactory = new HashMap<>();
    
    public static TwitterFactory getAppTwitterFactory() {
        if (appFactory == null) appFactory = getUserTwitterFactory(DAO.getConfig("twitterAccessToken", ""), DAO.getConfig("twitterAccessTokenSecret", ""));
        return appFactory;
    }

    public static TwitterFactory getUserTwitterFactory(String screen_name) {
        TwitterFactory uf = userFactory.get(screen_name);
        if (uf != null) return uf;
        AccountEntry accountEntry = DAO.searchLocalAccount(screen_name);
        if (accountEntry == null) return null;
        uf = getUserTwitterFactory(accountEntry.getOauthToken(), accountEntry.getOauthTokenSecret());
        if (uf != null) userFactory.put(screen_name, uf);
        return uf;
    }
    
    private static TwitterFactory getUserTwitterFactory(String accessToken, String accessTokenSecret) {
        if (accessToken == null || accessToken.length() == 0 || accessTokenSecret == null || accessTokenSecret.length() == 0) return null;
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setOAuthConsumerKey(DAO.getConfig("client.twitterConsumerKey", ""))
                .setOAuthConsumerSecret(DAO.getConfig("client.twitterConsumerSecret", ""))
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);
        cb.setJSONStoreEnabled(true);
        return new TwitterFactory(cb.build());
    }

    public static Map<String, Object> getUser(String screen_name) throws TwitterException, IOException {
        Map<String, Object> map = DAO.twitter_user_dump.getIndex("screen_name").get(screen_name);
        if (map != null) return map;
        TwitterFactory tf = getUserTwitterFactory(screen_name);
        if (tf == null) tf = getAppTwitterFactory();
        if (tf == null) return null;
        User user = tf.getInstance().showUser(screen_name);
        String json = TwitterObjectFactory.getRawJSON(user);
        XContentParser parser = JsonXContent.jsonXContent.createParser(json);
        map = parser == null ? null : parser.map();
        map.put("retrieval_date", AbstractIndexEntry.utcFormatter.print(System.currentTimeMillis()));
        Object status = map.remove("status"); // we don't need to store the latest status update in the user dump
        // TODO: store the latest status in our message database
        
        // enrich the user data with geocoding information
        String created_at = (String) map.get("created_at");
        String location = (String) map.get("location");
        if (created_at != null && location != null) {
            GeoMark loc = DAO.geoNames.analyse(location, null, 5, created_at.hashCode());
            if (loc != null) {
                map.put("location_point", new double[]{loc.lon(), loc.lat()}); //[longitude, latitude]
                map.put("location_mark", new double[]{loc.mlon(), loc.mlat()}); //[longitude, latitude]
            }
        }
        DAO.twitter_user_dump.putUnique(map);
        return map;
    }
    
    public static Set<Long> getFollowerIDs(Twitter twitter, String user_name) {
        long cursor = -1;
        Set<Long> allIDs = new HashSet<>();
        boolean complete = true;
        while (cursor != 0) {
            System.out.println("cursor:" + cursor);
            try {
                IDs ids = twitter.getFollowersIDs(user_name, cursor);
                RateLimitStatus rateStatus = ids.getRateLimitStatus();
                System.out.println("got: " + ids.getIDs().length + " ids");
                System.out.println("Rate Status: " + rateStatus.toString() + "; time=" + System.currentTimeMillis());
                for (long id: ids.getIDs()) allIDs.add(id);
                /*
                PagableResponseList<User> followers = twitter.getFollowersList(user_name, cursor);
                for (User follower: followers) {
                    System.out.println(follower.getName());
                }
                */
                if (rateStatus.getRemaining() == 5) {
                    complete = false;
                    break;
                }
                cursor = ids.getNextCursor();
            } catch (TwitterException e) {
                complete = false;
            }
        }
        return allIDs;
    }

}
