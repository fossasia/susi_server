/**
 *  DAO
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.api.client.SearchClient;
import org.loklak.scraper.TwitterScraper;
import org.loklak.tools.Cache;
import org.loklak.tools.UTF8;

/**
 * The Data Access Object for the message project.
 * This provides only static methods because the class methods shall be available for
 * all other classes.
 */
public class DAO {

    public final static String MESSAGE_DUMP_FILE_PREFIX = "messages_";
    public final static String MESSAGES_INDEX_NAME = "messages";
    public final static String USERS_INDEX_NAME = "users";
    public final static int CACHE_MAXSIZE = 10000;
    
    public  static File conf_dir;
    private static File message_dump_dir, message_dump_dir_own, message_dump_dir_import, message_dump_dir_imported;
    private static File settings_dir, customized_config;
    private static RandomAccessFile messagelog;
    private static Node elasticsearch_node;
    private static Client elasticsearch_client;
    private static Cache<String, Tweet> tweetCache = new Cache<String, Tweet>(CACHE_MAXSIZE);
    private static Cache<String, User> userCache = new Cache<String, User>(CACHE_MAXSIZE);
    private static BlockingQueue<Timeline> newTweetTimelines = new LinkedBlockingQueue<Timeline>();
    private static Properties config = new Properties();
    
    /**
     * initialize the DAO
     * @param datadir the path to the data directory
     */
    public static void init(File datadir) {
        try {
            // create and document the data dump dir
            message_dump_dir = new File(datadir, "dump");
            message_dump_dir_own = new File(message_dump_dir, "own");
            message_dump_dir_import = new File(message_dump_dir, "import");
            message_dump_dir_imported = new File(message_dump_dir, "imported");
            message_dump_dir.mkdirs();
            message_dump_dir_own.mkdirs();
            message_dump_dir_import.mkdirs();
            message_dump_dir_imported.mkdirs();
            File message_dump_dir_readme = new File(message_dump_dir, "readme.txt");
            if (!message_dump_dir_readme.exists()) {
                BufferedWriter w = new BufferedWriter(new FileWriter(message_dump_dir_readme));
                w.write("This directory contains dump files for messages which arrived the platform.\n");
                w.write("There are three subdirectories for dump files:\n");
                w.write("- own:      for messages received with this peer. There is one file for each month.\n");
                w.write("- import:   hand-over directory for message dumps to be imported. Drop dumps here and they are imported.\n");
                w.write("- imported: dump files which had been processed from the import directory are moved here.\n");
                w.write("You can import dump files from other peers by dropping them into the import directory.\n");
                w.write("Each dump file must start with the prefix '" + MESSAGE_DUMP_FILE_PREFIX + "' to be recognized.\n");
                w.close();
            }
            messagelog = new RandomAccessFile(getCurrentDump(message_dump_dir_own), "rw");
            
            // load the config file(s);
            conf_dir = new File("conf");
            config.load(new FileInputStream(new File(conf_dir, "config.properties")));
            settings_dir = new File(datadir, "settings");
            settings_dir.mkdirs();
            customized_config = new File(settings_dir, "customized_config.properties");
            if (!customized_config.exists()) {
                BufferedWriter w = new BufferedWriter(new FileWriter(customized_config));
                w.write("# This file can be used to customize the configuration file conf/config.properties\n");
                w.close();
            }
            Properties customized_config_props = new Properties();
            customized_config_props.load(new FileInputStream(customized_config));
            config.putAll(customized_config_props);
            
            // use all config attributes with a key starting with "elasticsearch." to set elasticsearch settings
            Builder builder = ImmutableSettings.settingsBuilder();
            for (Map.Entry<Object, Object> entry: config.entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith("elasticsearch.")) builder.put(key.substring(14), (String) entry.getValue());
            }

            // start elasticsearch
            elasticsearch_node = NodeBuilder.nodeBuilder().settings(builder).node();
            elasticsearch_client = elasticsearch_node.client();
            
            // set mapping (that shows how 'elastic' elasticsearch is: it's always good to define data types)
            try {
                elasticsearch_client.admin().indices().prepareCreate(MESSAGES_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(USERS_INDEX_NAME).execute().actionGet();
            } catch (IndexAlreadyExistsException ee) {}; // existing indexes are simply ignored, not re-created
            elasticsearch_client.admin().indices().preparePutMapping(MESSAGES_INDEX_NAME).setSource(Tweet.MAPPING).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(USERS_INDEX_NAME).setSource(User.MAPPING).setType("_default_").execute().actionGet();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    private static File getCurrentDump(File path) {
        SimpleDateFormat formatYearMonth = new SimpleDateFormat("yyyyMM", Locale.US);
        formatYearMonth.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentDatePart = formatYearMonth.format(new Date());
        
        // if there is already a dump, use it
        String[] existingDumps = path.list();
        for (String d: existingDumps) {
            if (d.startsWith(MESSAGE_DUMP_FILE_PREFIX + currentDatePart) && d.endsWith(".txt")) {
                return new File(path, d);
            }
            
            // in case the file is a dump file but ends with '.txt', we compress it here on-the-fly
            if (d.startsWith(MESSAGE_DUMP_FILE_PREFIX) && d.endsWith(".txt")) {
                final File source = new File(path, d);
                final File dest = new File(path, d + ".gz");
                new Thread() {
                    public void run() {
                        byte[] buffer = new byte[2^20];
                        try {
                            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(dest), 2^20);
                            FileInputStream in = new FileInputStream(source);
                            int l; while ((l = in.read(buffer)) > 0) out.write(buffer, 0, l);
                            in.close(); out.finish(); out.close();
                            if (dest.exists()) source.delete();
                       } catch (IOException e) {}
                    }
                }.start();
            }
        }
        // create a new one, use a random number. The random is used to make it possible to join many different dumps from different locations without renaming them
        String random = (Long.toString(Math.abs(new Random(System.currentTimeMillis()).nextLong())) + "00000000").substring(0, 8);
        return new File(path, MESSAGE_DUMP_FILE_PREFIX + currentDatePart + "_" + random + ".txt");
    }

    public static File[] getOwnDumps() {
        return getDumps(message_dump_dir_own);
    }
    public static File[] getImportDumps() {
        return getDumps(message_dump_dir_import);
    }
    public static File[] getImportedDumps() {
        return getDumps(message_dump_dir_imported);
    }
    private static File[] getDumps(File path) {
        String[] list = path.list();
        TreeSet<File> dumps = new TreeSet<File>(); // sort the names with a tree set
        for (String s: list) if (s.startsWith(MESSAGE_DUMP_FILE_PREFIX)) dumps.add(new File(path, s));
        return dumps.toArray(new File[dumps.size()]);
    }
    public static boolean shiftProcessedDump(String dumpName) {
        File f = new File(message_dump_dir_import, dumpName);
        if (!f.exists()) return false;
        File g = new File(message_dump_dir_imported, dumpName);
        if (g.exists()) g.delete();
        return f.renameTo(g);
    }
    public static int importDump(File dumpFile) {
        try {
            InputStream is = new FileInputStream(dumpFile);
            String line;
            int newTweet = 0;
            BufferedReader br = null;
            try {
                if (dumpFile.getName().endsWith(".gz")) is = new GZIPInputStream(is);
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while((line = br.readLine()) != null) {
                    XContentParser parser = JsonXContent.jsonXContent.createParser(line);
                    Map<String, Object> tweet = parser == null ? null : parser.map();
                    if (tweet == null) continue;
                    @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) tweet.remove("user");
                    if (user == null) continue;
                    User u = new User(user);
                    Tweet t = new Tweet(tweet);
                    boolean newtweet = DAO.record(t, u, false);
                    if (newtweet) newTweet++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null) br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return newTweet;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    
    /**
     * close all objects in this class
     */
    public static void close() {
        Log.getLog().info("closing DAO");
        try {messagelog.close();} catch (IOException e) {e.printStackTrace();}
        elasticsearch_node.close();
        while (!elasticsearch_node.isClosed()) try {Thread.sleep(100);} catch (InterruptedException e) {break;}
        Log.getLog().info("closed DAO");
    }
    
    /**
     * get values from 
     * @param key
     * @param default_val
     * @return
     */
    public static String getConfig(String key, String default_val) {
        String value = config.getProperty(key);
        return value == null ? default_val : value;
    }
    
    public static String[] getConfig(String key, String[] default_val, String delim) {
        String value = config.getProperty(key);
        return value == null || value.length() == 0 ? default_val : value.split(delim);
    }
    
    public static long getConfig(String key, long default_val) {
        String value = config.getProperty(key);
        try {
            return value == null ? default_val : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return default_val;
        }
    }
    
    public static void transmitTimeline(Timeline tl) {
        newTweetTimelines.add(tl);
    }

    public static Timeline takeTimeline(int maxsize, long maxwait) {
        Timeline tl = new Timeline();
        try {
            Timeline tl0 = newTweetTimelines.poll(maxwait, TimeUnit.MILLISECONDS);
            if (tl0 == null) return tl;
            tl.putAll(tl0);
            while (tl0.size() < maxsize && newTweetTimelines.size() > 0 && newTweetTimelines.peek().size() + tl0.size() <= maxsize) {
                tl0 = newTweetTimelines.take();
                if (tl0 == null) return tl;
                tl.putAll(tl0);
            }
            return tl;
        } catch (InterruptedException e) {
            return tl;
        }
    }
    
    private static Map<String, Object> getUserMap(String screen_name) {
        try {
            GetResponse userresponse = elasticsearch_client.prepareGet(USERS_INDEX_NAME, null, screen_name).execute().actionGet();
            Map<String, Object> usermap;
            if (userresponse.isExists() && (usermap = userresponse.getSourceAsMap()) != null) {
                return usermap;
            }
            return null;
        } catch (IndexMissingException e) {
            // may happen for first query
            return null;
        }
    }
    
    private static Map<String, Object> getTweetMap(String id_str) {
        try {
            GetResponse tweetresponse = elasticsearch_client.prepareGet(MESSAGES_INDEX_NAME, null, id_str).execute().actionGet();
            Map<String, Object> twittermap;
            if (tweetresponse.isExists() && (twittermap = tweetresponse.getSourceAsMap()) != null) {
                return twittermap;
            }
            return null;
        } catch (IndexMissingException e) {
            // may happen for first query
            return null;
        }
    }
    
    public static Tweet getTweet(String id_str) {
        Tweet tweet = tweetCache.get(id_str);
        if (tweet != null) return tweet;
        if (id_str == null) return null;
        Map<String, Object> tweetmap = getTweetMap(id_str);
        if (tweetmap == null) return null;
        String screen_name = (String) tweetmap.get("screen_name");
        if (screen_name == null) return null;
        try {
            tweet = new Tweet(tweetmap);
        } catch (MalformedURLException e) {
            return null;
        }
        tweetCache.put(id_str, tweet);
        return tweet;
    }

    public static User getUser(String screen_name) {
        User user = userCache.get(screen_name);
        if (user != null) return user;
        if (screen_name == null) return null;
        Map<String, Object> usermap = getUserMap(screen_name);
        if (usermap == null) return null;
        user = new User(usermap);
        userCache.put(screen_name, user);
        return user;
    }
    
    /**
     * Store a tweet together with a user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param t a tweet
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public synchronized static boolean record(Tweet t, User u, boolean dump) {
        //assert t != null;
        //assert u != null;
        try {

            // check if tweet exists in index
            //Tweet fromCache = tweetCache.get(t.getIdStr());
            //Map<String, Object> fromIndex = getTweetMap(t.getIdStr());
            if (tweetCache.get(t.getIdStr()) != null || getTweetMap(t.getIdStr()) != null) return false; // we omit writing this again

            // check if user exists in index
            if (userCache.get(u.getScreenName()) == null && getUserMap(u.getScreenName()) == null) {
                userCache.put(u.getScreenName(), u);
                // record user into search index
                XContentBuilder userj = XContentFactory.jsonBuilder();
                u.toJSON(userj);
                elasticsearch_client.prepareIndex(USERS_INDEX_NAME, t.getSourceType().name(), u.getScreenName())
                        .setSource(userj).setVersion(1).setVersionType(VersionType.FORCE).execute().actionGet();
                //Log.getLog().info("loklak", "create userindex for " + userj.string());
                userj.close();
            }
            
            // record tweet into text file
            if (dump) {
                XContentBuilder logline = XContentFactory.jsonBuilder();
                t.toJSON(logline, u, false);
                messagelog.seek(messagelog.length()); // go to end of file
                messagelog.write(UTF8.getBytes(logline.string()));
                messagelog.writeByte('\n');
                logline.close();
            }
            
            // record tweet into search index
            tweetCache.put(t.getIdStr(), t);
            XContentBuilder tweetj = XContentFactory.jsonBuilder();
            t.toJSON(tweetj, null, true);
            elasticsearch_client.prepareIndex(MESSAGES_INDEX_NAME, t.getSourceType().name(), t.getIdStr())
                    .setSource(tweetj).setVersion(1).setVersionType(VersionType.FORCE).execute().actionGet();
            //Log.getLog().info("loklak", "create tweetindex for " + tweetj.string());
            tweetj.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static long countLocalMessages() {
        CountResponse response = elasticsearch_client.prepareCount(MESSAGES_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();
        return response.getCount();
    }
    
    public static long countLocalUsers() {
        CountResponse response = elasticsearch_client.prepareCount(USERS_INDEX_NAME)
                .setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();
        return response.getCount();
    }
    
    public static class searchLocal {
        public Timeline timeline;
        public Map<String, List<Map.Entry<String, Long>>> aggregations;

        /**
         * Search the local message cache using a elasticsearch query.
         * @param q - the query, for aggregation this which should include a time frame in the form since:yyyy-MM-dd until:yyyy-MM-dd
         * @param timezoneOffset - an offset in minutes that is applied on dates given in the query of the form since:date until:date
         * @param resultCount - the number of messages in the result; can be zero if only aggregations are wanted
         * @param dateHistogrammInterval - the date aggregation interval or null, if no aggregation wanted
         * @param aggregationLimit - the maximum count of facet entities, not search results
         * @param aggregationFields - names of the aggregation fields. If no aggregation is wanted, pass no (zero) field(s)
         */
        public searchLocal(String q, int timezoneOffset, int resultCount, int aggregationLimit, String... aggregationFields) {
            this.timeline = new Timeline();
            try {
                // prepare request
                searchQuery sq = new searchQuery(q, timezoneOffset);
                SearchRequestBuilder request = elasticsearch_client.prepareSearch(MESSAGES_INDEX_NAME)
                        .setSearchType(SearchType.QUERY_THEN_FETCH)
                        .setQuery(sq.queryBuilder)
                        .setFrom(0)
                        .setSize(resultCount);
                if (resultCount > 0) request.addSort("created_at", SortOrder.DESC);
                boolean addTimeHistogram = false;
                for (String field: aggregationFields) {
                    if (field.equals("created_at")) {
                        addTimeHistogram = true;
                        long interval = sq.until.getTime() - sq.since.getTime();
                        DateHistogram.Interval dateHistogrammInterval = interval > 1000 * 60 * 60 * 24 * 7 ? DateHistogram.Interval.DAY : interval > 1000 * 60 * 60 * 3 ? DateHistogram.Interval.HOUR : DateHistogram.Interval.MINUTE;
                        request.addAggregation(AggregationBuilders.dateHistogram("created_at").field("created_at").timeZone("UTC").minDocCount(0).interval(dateHistogrammInterval));
                    } else {
                        request.addAggregation(AggregationBuilders.terms(field).field(field).minDocCount(1).size(aggregationLimit));
                    }
                }
                
                // get response
                SearchResponse response = request.execute().actionGet();

                // evaluate search result
                //long totalHitCount = response.getHits().getTotalHits();
                SearchHit[] hits = response.getHits().getHits();
                for (SearchHit hit: hits) {
                    Map<String, Object> map = hit.getSource();
                    try {
                        Tweet tweet = new Tweet(map);
                        User user = getUser(tweet.getUserScreenName());
                        assert user != null;
                        if (user != null) {
                            timeline.addTweet(tweet);
                            timeline.addUser(user);
                        }
                    } catch (MalformedURLException e) {
                        continue;
                    }
                }
                
                // evaluate aggregation
                // collect results: fields
                this.aggregations = new HashMap<>();
                for (String field: aggregationFields) {
                    if (field.equals("created_at")) continue; // this has special handling below
                    Terms fieldCounts = response.getAggregations().get(field);
                    List<Bucket> buckets = fieldCounts.getBuckets();
                    ArrayList<Map.Entry<String, Long>> list = new ArrayList<>(buckets.size());
                    for (Bucket bucket: buckets) {
                        Map.Entry<String,Long> entry = new AbstractMap.SimpleEntry<String, Long>(bucket.getKey(), bucket.getDocCount());
                        list.add(entry);
                    }
                    aggregations.put(field, list);
                }
                // date histogram:
                if (addTimeHistogram) {
                    DateHistogram dateCounts = response.getAggregations().get("created_at");
                    ArrayList<Map.Entry<String, Long>> list = new ArrayList<>();
                    for (DateHistogram.Bucket bucket : dateCounts.getBuckets()) {
                        Calendar cal = Calendar.getInstance(UTCtimeZone);
                        cal.setTime(bucket.getKeyAsDate().toDate());
                        cal.add(Calendar.MINUTE, -timezoneOffset);
                        long docCount = bucket.getDocCount();
                        Map.Entry<String,Long> entry = new AbstractMap.SimpleEntry<String, Long>(responseDateFormat.format(cal.getTime()), docCount);
                        list.add(entry);
                    }
                    aggregations.put("created_at", list);
                }
            } catch (IndexMissingException e) {}
        }
    }
    
    public final static DateFormat queryDateFormat = new SimpleDateFormat("yyyy-MM-dd"); // this is the twitter search modifier format
    public final static DateFormat responseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // this is the format which morris.js understands for date-histogram graphs
    private final static Pattern tokenizerPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // tokenizes Strings into terms respecting quoted parts
    private final static Calendar UTCCalendar = Calendar.getInstance();
    private final static TimeZone UTCtimeZone = TimeZone.getTimeZone("UTC");
    static {
        UTCCalendar.setTimeZone(UTCtimeZone);
        queryDateFormat.setCalendar(UTCCalendar);
        responseDateFormat.setCalendar(UTCCalendar);
    }
    
    public static class searchQuery {
        QueryBuilder queryBuilder;
        Date since, until;
        public searchQuery(String q, int timezoneOffset) {
            // tokenize the query
            List<String> qe = new ArrayList<String>();
            Matcher m = tokenizerPattern.matcher(q);
            while (m.find()) qe.add(m.group(1));
            
            // twitter search syntax:
            //   term1 term2 term3 - all three terms shall appear
            //   "term1 term2 term3" - exact match of all terms
            //   term1 OR term2 OR term3 - any of the three terms shall appear
            //   from:user - tweets posted from that user
            //   to:user - tweets posted to that user
            //   @user - tweets which mention that user
            //   near:"location" within:xmi - tweets that are near that location
            //   #hashtag - tweets containing the given hashtag
            //   since:2015-04-01 until:2015-04-03 - tweets within given time range
            String text = "";
            ArrayList<String> users = new ArrayList<>();
            ArrayList<String> hashtags = new ArrayList<>();
            HashMap<String, String> modifier = new HashMap<>();
            for (String t: qe) {
                if (t.length() == 0) continue;
                if (t.startsWith("@")) {
                    users.add(t.substring(1));
                } else if (t.startsWith("#")) {
                    hashtags.add(t.substring(1));
                } else if (t.indexOf(':') > 0) {
                    int p = t.indexOf(':');
                    modifier.put(t.substring(0, p).toLowerCase(), t.substring(p + 1));
                } else {
                    text += t + " ";
                }
            }
            if (modifier.containsKey("to")) users.add(modifier.get("to"));
            text = text.trim();
            // compose query
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            if (text.length() > 0) query.must(QueryBuilders.matchQuery("text", text));
            for (String user: users) query.must(QueryBuilders.termQuery("mentions", user));
            for (String hashtag: hashtags) query.must(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            if (modifier.containsKey("from")) query.must(QueryBuilders.termQuery("screen_name", modifier.get("from")));
            if (modifier.containsKey("near")) {
                BoolQueryBuilder nearquery = QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("place_name", modifier.get("near")))
                        .should(QueryBuilders.matchQuery("text", modifier.get("near")));
                query.must(nearquery);
            }
            if (modifier.containsKey("since")) try {
                Calendar since = parseDateModifier(modifier.get("since"), timezoneOffset);
                this.since = since.getTime();
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").from(this.since);
                if (modifier.containsKey("until")) {
                    Calendar until = parseDateModifier(modifier.get("until"), timezoneOffset);
                    if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                        // until must be the day which is included in results.
                        // To get the result within the same day, we must add one day.
                        until.add(Calendar.DATE, 1);
                    }
                    this.until = until.getTime();
                    rangeQuery.to(this.until);
                } else {
                    this.until = new Date();
                }
                query.must(rangeQuery);
            } catch (ParseException e) {} else {
                this.since = new Date(0);
                this.until = new Date();
            }
            this.queryBuilder = query;
        }
    }
    
    public static Calendar parseDateModifier(String modifier, final int timezoneOffset) throws ParseException {
        modifier = modifier.replaceAll("_", " ");
        Calendar cal = Calendar.getInstance(UTCtimeZone);
        cal.setTime(modifier.indexOf(':') > 0 ? responseDateFormat.parse(modifier) : queryDateFormat.parse(modifier));
        cal.add(Calendar.MINUTE, timezoneOffset); // add a correction; i.e. for UTC+1 -60 minutes is added to patch a time given in UTC+1 to the actual time at UTC
        return cal;
    }
    
    public static Timeline[] scrapeTwitter(final String q, final int timezoneOffset) {
        String[] remote = DAO.getConfig("frontpeers", new String[0], ",");        
        Timeline allTweets = remote.length > 0 ? searchOnOtherPeers(remote, q, 100, timezoneOffset, "twitter", SearchClient.frontpeer_hash) : TwitterScraper.search(q);
        Timeline newTweets = new Timeline(); // we store new tweets here to be able to transmit them to peers
        if (allTweets == null) {// can be caused by time-out
            allTweets = new Timeline();
        } else {
            // record the result; this may be moved to a concurrent process
            for (Tweet t: allTweets) {
                User u = allTweets.getUser(t);
                assert u != null;
                boolean newTweet = record(t, u, true);
                if (newTweet) {
                    newTweets.addTweet(t);
                    newTweets.addUser(u);
                }
            }
            DAO.transmitTimeline(newTweets);
        }
        return new Timeline[]{allTweets, newTweets};
    }
    
    public static Timeline searchBackend(final String q, final int count, final int timezoneOffset, final String where) {
        String[] remote = DAO.getConfig("backend", new String[0], ",");
        return searchOnOtherPeers(remote, q, count, timezoneOffset, where, SearchClient.backend_hash);
    }
    
    public static Timeline searchOnOtherPeers(final String[] remote, final String q, final int count, final int timezoneOffset, final String where, final String provider_hash) {
        Timeline tl = new Timeline();
        for (String protocolhostportstub: remote) {
            Timeline tt = SearchClient.search(protocolhostportstub, q, where, count, timezoneOffset, provider_hash);
            tl.putAll(tt);
            // record the result; this may be moved to a concurrent process
            for (Tweet t: tt) {
                User u = tt.getUser(t);
                assert u != null;
                record(t, u, true);
            }
        }
        return tl;
    }
    
    public static void log(String line) {
        Log.getLog().info(line);
    }

    public static String noNULL(String s) {return s == null ? "" : s;}
    public static long noNULL(Number n) {return n == null ? 0 : n.longValue();}
    public static ArrayList<String> noNULL(ArrayList<String> l) {return l == null ? new ArrayList<String>(0) : l;}
    
}
