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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.loklak.LoklakServer;
import org.loklak.data.AbstractIndexEntry;
import org.loklak.data.AccountEntry;
import org.loklak.data.DAO;
import org.loklak.data.UserEntry;
import org.loklak.geo.GeoMark;
import org.loklak.tools.DateParser;
import org.loklak.tools.storage.JsonDataset;
import org.loklak.tools.storage.JsonFactory;
import org.loklak.tools.storage.JsonMinifier;
import org.loklak.tools.storage.JsonDataset.JsonFactoryIndex;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import twitter4j.IDs;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

@SuppressWarnings("unused")
public class TwitterAPI {
    
    private final static String RATE_ACCOUNT_LOGIN_VERIFICATION_ENROLLMENT = "/account/login_verification_enrollment"; // limit = 15
    private final static String RATE_ACCOUNT_SETTINGS = "/account/settings"; // limit = 15
    private final static String RATE_ACCOUNT_UPDATE_PROFILE = "/account/update_profile"; // limit = 15
    private final static String RATE_ACCOUNT_VERIFY_CREDENTIALS = "/account/verify_credentials"; // limit = 15
    private final static String RATE_APPLICATION_RATE_LIMIT_STATUS = "/application/rate_limit_status"; // limit = 180
    private final static String RATE_BLOCKS_IDS = "/blocks/ids"; // limit = 15
    private final static String RATE_BLOCKS_LIST = "/blocks/list"; // limit = 15
    private final static String RATE_COLLECTIONS_ENTRIES = "/collections/entries"; // limit = 1000
    private final static String RATE_COLLECTIONS_LIST = "/collections/list"; // limit = 1000
    private final static String RATE_COLLECTIONS_SHOW = "/collections/show"; // limit = 1000
    private final static String RATE_CONTACTS_ADDRESSBOOK = "/contacts/addressbook"; // limit = 300
    private final static String RATE_CONTACTS_DELETE_STATUS = "/contacts/delete/status"; // limit = 300
    private final static String RATE_CONTACTS_UPLOADED_BY = "/contacts/uploaded_by"; // limit = 300
    private final static String RATE_CONTACTS_USERS = "/contacts/users"; // limit = 300
    private final static String RATE_CONTACTS_USERS_AND_UPLOADED_BY = "/contacts/users_and_uploaded_by"; // limit = 300
    private final static String RATE_DEVICE_TOKEN = "/device/token"; // limit = 15
    private final static String RATE_DIRECT_MESSAGES = "/direct_messages"; // limit = 15
    private final static String RATE_DIRECT_MESSAGES_SENT = "/direct_messages/sent"; // limit = 15
    private final static String RATE_DIRECT_MESSAGES_SENT_AND_RECEIVED = "/direct_messages/sent_and_received"; // limit = 15
    private final static String RATE_DIRECT_MESSAGES_SHOW = "/direct_messages/show"; // limit = 15
    private final static String RATE_FAVORITES_LIST = "/favorites/list"; // limit = 15
    private final static String RATE_FOLLOWERS_IDS = "/followers/ids"; // limit = 15
    private final static String RATE_FOLLOWERS_LIST = "/followers/list"; // limit = 15
    private final static String RATE_FRIENDS_FOLLOWING_IDS = "/friends/following/ids"; // limit = 15
    private final static String RATE_FRIENDS_FOLLOWING_LIST = "/friends/following/list"; // limit = 15
    private final static String RATE_FRIENDS_IDS = "/friends/ids"; // limit = 15
    private final static String RATE_FRIENDS_LIST = "/friends/list"; // limit = 15
    private final static String RATE_FRIENDSHIPS_INCOMING = "/friendships/incoming"; // limit = 15
    private final static String RATE_FRIENDSHIPS_LOOKUP = "/friendships/lookup"; // limit = 15
    private final static String RATE_FRIENDSHIPS_NO_RETWEETS_IDS = "/friendships/no_retweets/ids"; // limit = 15
    private final static String RATE_FRIENDSHIPS_OUTGOING = "/friendships/outgoing"; // limit = 15
    private final static String RATE_FRIENDSHIPS_SHOW = "/friendships/show"; // limit = 180
    private final static String RATE_GEO_ID_PLACE_ID = "/geo/id/:place_id"; // limit = 15
    private final static String RATE_GEO_REVERSE_GEOCODE = "/geo/reverse_geocode"; // limit = 15
    private final static String RATE_GEO_SEARCH = "/geo/search"; // limit = 15
    private final static String RATE_GEO_SIMILAR_PLACES = "/geo/similar_places"; // limit = 15
    private final static String RATE_HELP_CONFIGURATION = "/help/configuration"; // limit = 15
    private final static String RATE_HELP_LANGUAGES = "/help/languages"; // limit = 15
    private final static String RATE_HELP_PRIVACY = "/help/privacy"; // limit = 15
    private final static String RATE_HELP_SETTINGS = "/help/settings"; // limit = 15
    private final static String RATE_HELP_TOS = "/help/tos"; // limit = 15
    private final static String RATE_LISTS_LIST = "/lists/list"; // limit = 15
    private final static String RATE_LISTS_MEMBERS = "/lists/members"; // limit = 180
    private final static String RATE_LISTS_MEMBERS_SHOW = "/lists/members/show"; // limit = 15
    private final static String RATE_LISTS_MEMBERSHIPS = "/lists/memberships"; // limit = 15
    private final static String RATE_LISTS_OWNERSHIPS = "/lists/ownerships"; // limit = 15
    private final static String RATE_LISTS_SHOW = "/lists/show"; // limit = 15
    private final static String RATE_LISTS_STATUSES = "/lists/statuses"; // limit = 180
    private final static String RATE_LISTS_SUBSCRIBERS = "/lists/subscribers"; // limit = 180
    private final static String RATE_LISTS_SUBSCRIBERS_SHOW = "/lists/subscribers/show"; // limit = 15
    private final static String RATE_LISTS_SUBSCRIPTIONS = "/lists/subscriptions"; // limit = 15
    private final static String RATE_MUTES_USERS_IDS = "/mutes/users/ids"; // limit = 15
    private final static String RATE_MUTES_USERS_LIST = "/mutes/users/list"; // limit = 15
    private final static String RATE_SAVED_SEARCHES_DESTROY_ID = "/saved_searches/destroy/:id"; // limit = 15
    private final static String RATE_SAVED_SEARCHES_LIST = "/saved_searches/list"; // limit = 15
    private final static String RATE_SAVED_SEARCHES_SHOW_ID = "/saved_searches/show/:id"; // limit = 15
    private final static String RATE_SEARCH_TWEETS = "/search/tweets"; // limit = 180
    private final static String RATE_STATUSES_FRIENDS = "/statuses/friends"; // limit = 15
    private final static String RATE_STATUSES_HOME_TIMELINE = "/statuses/home_timeline"; // limit = 15
    private final static String RATE_STATUSES_LOOKUP = "/statuses/lookup"; // limit = 180
    private final static String RATE_STATUSES_MENTIONS_TIMELINE = "/statuses/mentions_timeline"; // limit = 15
    private final static String RATE_STATUSES_OEMBED = "/statuses/oembed"; // limit = 180
    private final static String RATE_STATUSES_RETWEETERS_IDS = "/statuses/retweeters/ids"; // limit = 15
    private final static String RATE_STATUSES_RETWEETS_ID = "/statuses/retweets/:id"; // limit = 60
    private final static String RATE_STATUSES_RETWEETS_OF_ME = "/statuses/retweets_of_me"; // limit = 15
    private final static String RATE_STATUSES_SHOW_ID = "/statuses/show/:id"; // limit = 180
    private final static String RATE_STATUSES_USER_TIMELINE = "/statuses/user_timeline"; // limit = 180
    private final static String RATE_TRENDS_AVAILABLE = "/trends/available"; // limit = 15
    private final static String RATE_TRENDS_CLOSEST = "/trends/closest"; // limit = 15
    private final static String RATE_TRENDS_PLACE = "/trends/place"; // limit = 15
    private final static String RATE_USERS_DERIVED_INFO = "/users/derived_info"; // limit = 15
    private final static String RATE_USERS_LOOKUP = "/users/lookup"; // limit = 180
    private final static String RATE_USERS_PROFILE_BANNER = "/users/profile_banner"; // limit = 180
    private final static String RATE_USERS_REPORT_SPAM = "/users/report_spam"; // limit = 15
    private final static String RATE_USERS_SEARCH = "/users/search"; // limit = 180
    private final static String RATE_USERS_SHOW_ID = "/users/show/:id"; // limit = 180
    private final static String RATE_USERS_SUGGESTIONS = "/users/suggestions"; // limit = 15
    private final static String RATE_USERS_SUGGESTIONS_SLUG = "/users/suggestions/:slug"; // limit = 15
    private final static String RATE_USERS_SUGGESTIONS_SLUG_MEMBERS = "/users/suggestions/:slug/members"; // limit = 15

    private static TwitterFactory appFactory = null;
    private static Map<String, TwitterFactory> userFactory = new HashMap<>();
    
    public static TwitterFactory getAppTwitterFactory() {
        String twitterAccessToken = DAO.getConfig("twitterAccessToken", "");
        String twitterAccessTokenSecret = DAO.getConfig("twitterAccessTokenSecret", "");
        if (twitterAccessToken.length() == 0 || twitterAccessTokenSecret.length() == 0) return null;
        if (appFactory == null) appFactory = getUserTwitterFactory(twitterAccessToken, twitterAccessTokenSecret);
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

    public static RateLimitStatus getRateLimitStatus(final String rate_type) throws TwitterException {
        return getAppTwitterFactory().getInstance().getRateLimitStatus().get(rate_type);
    }

    private static final int getUserLimit = 180;
    private static int getUserRemaining = getUserLimit;
    private static long getUserResetTime = 0;
    public static int getUserRemaining() {return System.currentTimeMillis() > getUserResetTime ? getUserLimit : getUserRemaining;}
    public static Map<String, Object> getUser(String screen_name, boolean forceReload) throws TwitterException, IOException {
        if (!forceReload) {
            JsonFactory mapcapsule = DAO.user_dump.get("screen_name",screen_name);
            if (mapcapsule == null) mapcapsule = DAO.user_dump.get("id_str", screen_name);
            if (mapcapsule != null) {
                Map<String, Object> map = mapcapsule.getJson();
                if (map.size() > 0) {
                    // check if the entry is maybe outdated, i.e. if it is empty or too old
                    try {
                        Date d = DAO.user_dump.parseDate(map);
                        if (d.getTime() + DateParser.DAY_MILLIS > System.currentTimeMillis()) return map;
                    } catch (ParseException e) {
                        return map;
                    }
                }
            }
        }
        TwitterFactory tf = getUserTwitterFactory(screen_name);
        if (tf == null) tf = getAppTwitterFactory();
        if (tf == null) return new HashMap<>();
        Twitter twitter = tf.getInstance();
        User user = twitter.showUser(screen_name);
        RateLimitStatus rateLimitStatus = user.getRateLimitStatus();
        getUserResetTime = System.currentTimeMillis() + rateLimitStatus.getSecondsUntilReset() * 1000;
        getUserRemaining = rateLimitStatus.getRemaining();
        return enrich(user);
    }
    
    public static Map<String, Object> enrich(User user) throws IOException {
        String json = TwitterObjectFactory.getRawJSON(user);
        Map<String, Object> map = DAO.jsonMapper.readValue(json, DAO.jsonTypeRef);
        map.put("retrieval_date", AbstractIndexEntry.utcFormatter.print(System.currentTimeMillis()));
        Object status = map.remove("status"); // we don't need to store the latest status update in the user dump
        // TODO: store the latest status in our message database
        
        // enrich the user data with geocoding information
        String created_at = (String) map.get("created_at");
        String location = (String) map.get("location");
        if (created_at != null && location != null) {
            GeoMark loc = DAO.geoNames.analyse(location, null, 5, created_at.hashCode());
            if (loc != null) {
                map.put("location_country", DAO.geoNames.getCountryName(loc.getISO3166cc()));
                map.put("location_country_code", loc.getISO3166cc());
                map.put("location_point", new double[]{loc.lon(), loc.lat()}); //[longitude, latitude]
                map.put("location_mark", new double[]{loc.mlon(), loc.mlat()}); //[longitude, latitude]
            }
        }
        DAO.user_dump.putUnique(map);
        return map;
    }
    
    public static Map<String, Object> getNetwork(String screen_name, int maxFollowers, int maxFollowing) throws IOException, TwitterException {
        Map<String, Object> map = new HashMap<>();
        // we clone the maps because we modify it
        map.putAll(getNetworkerNames(screen_name, maxFollowers, Networker.FOLLOWERS));
        map.putAll(getNetworkerNames(screen_name, maxFollowing, Networker.FOLLOWING));
        map.remove("screen_name");

        for (String setname : new String[]{"followers","unfollowers","following","unfollowing"}) {
            List<Map<String, Object>> users = new ArrayList<>(); 
            Map<String, Number> names = (Map<String, Number>) map.remove(setname + "_names");
            if (names != null) for (String sn: names.keySet()) {
                JsonFactory user = DAO.user_dump.get("screen_name", sn);
                if (user != null) users.add(user.getJson());
            }
            map.put(setname + "_count", users.size());
            map.put(setname, users);
        }
        
        return map;
    }
    
    private static enum Networker {
        FOLLOWERS, FOLLOWING;
    }

    private static final int getFollowerIdLimit = 180, getFollowingIdLimit = 180;
    private static int getFollowerIdRemaining = getFollowerIdLimit, getFollowingIdRemaining = getFollowingIdLimit;
    private static long getFollowerIdResetTime = 0, getFollowingIdResetTime = 0;
    public static int getFollowerIdRemaining() {return System.currentTimeMillis() > getFollowerIdResetTime ? getFollowerIdLimit : getFollowerIdRemaining;}
    public static int getFollowingIdRemaining() {return System.currentTimeMillis() > getFollowingIdResetTime ? getFollowingIdLimit : getFollowingIdRemaining;}
    public static Map<String, Object> getFollowersNames(final String screen_name, final int max_count) throws IOException, TwitterException {
        return getNetworkerNames(screen_name, max_count, Networker.FOLLOWERS);
    }
    public static Map<String, Object> getFollowingNames(final String screen_name, final int max_count) throws IOException, TwitterException {
        return getNetworkerNames(screen_name, max_count, Networker.FOLLOWING);
    }
    public static Map<String, Object> getNetworkerNames(final String screen_name, final int max_count, final Networker networkRelation) throws IOException, TwitterException {
        if (max_count == 0) return new HashMap<>();
        boolean complete = true;
        Set<Number> networkingIDs = new LinkedHashSet<>();
        Set<Number> unnetworkingIDs = new LinkedHashSet<>();
        JsonFactory mapcapsule = (networkRelation == Networker.FOLLOWERS ? DAO.followers_dump : DAO.following_dump).get("screen_name", screen_name);
        if (mapcapsule == null) {
            JsonDataset ds = networkRelation == Networker.FOLLOWERS ? DAO.followers_dump : DAO.following_dump;
            mapcapsule = ds.get("screen_name", screen_name);
        }

        if (mapcapsule != null) {
            // check if the map is complete
            Map<String, Object> map = mapcapsule.getJson();
            complete = (Boolean) map.get("complete");
            if (complete) return map; // TODO: check date
            List<Object> fro = (List<Object>) map.get(networkRelation == Networker.FOLLOWERS ? "follower" : "following");
            for (Object f: fro) {
                networkingIDs.add((Number) f);
            }
        }
        TwitterFactory tf = getUserTwitterFactory(screen_name);
        if (tf == null) tf = getAppTwitterFactory();
        if (tf == null) return new HashMap<String, Object>();
        Twitter twitter = tf.getInstance();
        long cursor = -1;
        collect: while (cursor != 0) {
            try {
                IDs ids = networkRelation == Networker.FOLLOWERS ? twitter.getFollowersIDs(screen_name, cursor) : twitter.getFriendsIDs(screen_name, cursor);
                RateLimitStatus rateStatus = ids.getRateLimitStatus();
                if (networkRelation == Networker.FOLLOWERS) {
                    getFollowerIdRemaining = rateStatus.getRemaining();
                    getFollowerIdResetTime = System.currentTimeMillis() + rateStatus.getSecondsUntilReset() * 1000;
                } else {
                    getFollowingIdRemaining = rateStatus.getRemaining();
                    getFollowingIdResetTime = System.currentTimeMillis() + rateStatus.getSecondsUntilReset() * 1000;
                }
                //System.out.println("got: " + ids.getIDs().length + " ids");
                //System.out.println("Rate Status: " + rateStatus.toString() + "; time=" + System.currentTimeMillis());
                boolean dd = false;
                for (long id: ids.getIDs()) {
                    if (networkingIDs.contains(id)) dd = true; // don't break loop here
                    networkingIDs.add(id);
                }
                if (dd) break collect; // this is complete!
                if (rateStatus.getRemaining() == 0) {
                    complete = false;
                    break collect;
                }
                if (networkingIDs.size() >= Math.min(10000, max_count >= 0 ? max_count : 10000)) {
                    complete = false;
                    break collect;
                }
                cursor = ids.getNextCursor();
            } catch (TwitterException e) {
                complete = false;
                break collect;
            }
        }
        // create result
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("screen_name", screen_name);
        map.put("retrieval_date", AbstractIndexEntry.utcFormatter.print(System.currentTimeMillis()));
        map.put("complete", complete);
        Map<String, Number> networking = getScreenName(networkingIDs, max_count, true);
        Map<String, Number> unnetworking = getScreenName(unnetworkingIDs, max_count, true);
        if (networkRelation == Networker.FOLLOWERS) {
            map.put("followers_count", networking.size());
            map.put("unfollowers_count", unnetworking.size());
            map.put("followers_names", networking);
            map.put("unfollowers_names", unnetworking);
            if (complete) DAO.followers_dump.putUnique(map); // currently we write only complete data sets. In the future the update of datasets shall be supported
        } else {
            map.put("following_count", networking.size());
            map.put("unfollowing_count", unnetworking.size());
            map.put("following_names", networking);
            map.put("unfollowing_names", unnetworking);
            if (complete) DAO.following_dump.putUnique(map);
        }
        return map;
    }
    
    /**
     * search for twitter user names by a given set of user id's
     * @param id_strs
     * @param lookupLocalUsersByUserId if this is true and successful, the resulting names may contain users without user info in the user dump
     * @return
     * @throws IOException
     * @throws TwitterException 
     */
    public static Map<String, Number> getScreenName(Set<Number> id_strs, final int maxFollowers, boolean lookupLocalUsersByUserId) throws IOException, TwitterException {
        // we have several sources to get this mapping:
        // - 1st / fastest: mapping from DAO.twitter_user_dump
        // - 2nd / fast   : mapping from DAO.searchLocalUserByUserId(user_id)
        // - 3rd / slow   : from twitter API with twitter.lookupUsers(String[] user_id)
        // first we check all fast solutions until trying the twitter api
        Map<String, Number> r = new HashMap<>();
        Set<Number> id4api = new HashSet<>();
        for (Number id_str: id_strs) {
            if (r.size() >= maxFollowers) break;
            JsonFactory mapcapsule = DAO.user_dump.get("id_str", id_str.toString());
            if (mapcapsule != null) {
                Map<String, Object> map = mapcapsule.getJson();
                String screen_name = (String) map.get("screen_name");
                if (screen_name != null) {
                    r.put(screen_name, id_str);
                    continue;
                }
            }
            if (lookupLocalUsersByUserId) {
                UserEntry ue = DAO.searchLocalUserByUserId(id_str.toString());
                if (ue != null) {
                    String screen_name = ue.getScreenName();
                    if (screen_name != null) {
                        r.put(screen_name, id_str);
                        continue;
                    }
                }
            }
            id4api.add(id_str);
        }

        while (id4api.size() > 100 && id4api.size() + r.size() > maxFollowers) id4api.remove(id4api.iterator().next());
        
        // resolve the remaining user_ids from the twitter api
        if (r.size() < maxFollowers && id4api.size() > 0) {
            TwitterFactory tf = getAppTwitterFactory();
            if (tf == null) return new HashMap<>();
            Twitter twitter = tf.getInstance();
            collect: while (id4api.size() > 0) {
                // construct a query term with at most 100 id's
                int chunksize = Math.min(100, id4api.size());
                long[] u = new long[chunksize];
                Iterator<Number> ni = id4api.iterator();
                for (int i = 0; i < chunksize; i++) {
                    u[i] = ni.next().longValue();
                }
                try {
                    ResponseList<User> users = twitter.lookupUsers(u);
                    for (User usr: users) {
                        enrich(usr);
                        r.put(usr.getScreenName(), usr.getId());
                        id4api.remove(usr.getId());
                    }
                } catch (TwitterException e) {
                    if (r.size() == 0) throw e;
                    break collect;
                }
            }
        }
        return r;
    }
    
    
    public static void main(String[] args) {
        try {
            Path data = FileSystems.getDefault().getPath("data");
            DAO.init(LoklakServer.readConfig(data), data);
            try {
                System.out.println(getRateLimitStatus(RATE_FOLLOWERS_IDS));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            try {
                System.out.println(getFollowersNames("loklak_app", 10000));
            } catch (IOException | TwitterException e) {
                e.printStackTrace();
            }
            try {
                System.out.println(getFollowingNames("loklak_app", 10000));
            } catch (IOException | TwitterException e) {
                e.printStackTrace();
            }
            DAO.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        System.exit(0);
    }
    
}
