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

package org.loklak.data;

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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
import org.loklak.Caretaker;
import org.loklak.api.client.SearchClient;
import org.loklak.scraper.TwitterScraper;
import org.loklak.tools.DateParser;
import org.loklak.tools.UTF8;

/**
 * The Data Access Object for the message project.
 * This provides only static methods because the class methods shall be available for
 * all other classes.
 */
public class DAO {

    public final static String MESSAGE_DUMP_FILE_PREFIX = "messages_";
    public final static String QUERIES_INDEX_NAME = "queries";
    public final static String MESSAGES_INDEX_NAME = "messages";
    public final static String USERS_INDEX_NAME = "users";
    public final static int CACHE_MAXSIZE = 10000;
    
    public  static File conf_dir;
    private static File message_dump_dir, message_dump_dir_own, message_dump_dir_import, message_dump_dir_imported;
    private static File settings_dir, customized_config;
    private static RandomAccessFile messagelog;
    private static Node elasticsearch_node;
    private static Client elasticsearch_client;
    private static UserFactory users;
    private static MessageFactory messages;
    private static QueryFactory queries;
    private static BlockingQueue<Timeline> newMessageTimelines = new LinkedBlockingQueue<Timeline>();
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
            
            // define the index factories
            users = new UserFactory(elasticsearch_client, USERS_INDEX_NAME, CACHE_MAXSIZE);
            messages = new MessageFactory(elasticsearch_client, MESSAGES_INDEX_NAME, CACHE_MAXSIZE);
            queries = new QueryFactory(elasticsearch_client, QUERIES_INDEX_NAME, CACHE_MAXSIZE);
            
            // set mapping (that shows how 'elastic' elasticsearch is: it's always good to define data types)
            try {
                elasticsearch_client.admin().indices().prepareCreate(QUERIES_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(MESSAGES_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(USERS_INDEX_NAME).execute().actionGet();
            } catch (IndexAlreadyExistsException ee) {}; // existing indexes are simply ignored, not re-created
            elasticsearch_client.admin().indices().preparePutMapping(QUERIES_INDEX_NAME).setSource(queries.getMapping()).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(MESSAGES_INDEX_NAME).setSource(messages.getMapping()).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(USERS_INDEX_NAME).setSource(users.getMapping()).setType("_default_").execute().actionGet();
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
                    UserEntry u = new UserEntry(user);
                    MessageEntry t = new MessageEntry(tweet);
                    boolean newtweet = DAO.writeMessage(t, u, false);
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
    
    public static boolean getConfig(String key, boolean default_val) {
        String value = config.getProperty(key);
        return value == null ? default_val : value.equals("true") || value.equals("on") || value.equals("1");
    }
    
    public static void transmitTimeline(Timeline tl) {
        if (getConfig("backend", new String[0], ",").length > 0) newMessageTimelines.add(tl);
    }

    public static Timeline takeTimeline(int maxsize, long maxwait) {
        Timeline tl = new Timeline();
        try {
            Timeline tl0 = newMessageTimelines.poll(maxwait, TimeUnit.MILLISECONDS);
            if (tl0 == null) return tl;
            tl.putAll(tl0);
            while (tl0.size() < maxsize && newMessageTimelines.size() > 0 && newMessageTimelines.peek().size() + tl0.size() <= maxsize) {
                tl0 = newMessageTimelines.take();
                if (tl0 == null) return tl;
                tl.putAll(tl0);
            }
            return tl;
        } catch (InterruptedException e) {
            return tl;
        }
    }
    
    /**
     * Store a message together with a user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param t a tweet
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public synchronized static boolean writeMessage(MessageEntry t, UserEntry u, boolean dump) {
        //assert t != null;
        //assert u != null;
        try {

            // check if tweet exists in index
            if ((t instanceof TwitterScraper.TwitterTweet &&
                ((TwitterScraper.TwitterTweet) t).exist() != null &&
                ((TwitterScraper.TwitterTweet) t).exist().booleanValue()) ||
                messages.exists(t.getIdStr())) return false; // we omit writing this again

            // check if user exists in index
            if (!users.exists(u.getScreenName())) {
                users.writeEntry(u.getScreenName(), t.getSourceType().name(), u);
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
            messages.writeEntry(t.getIdStr(), t.getSourceType().name(), t);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static long countLocalMessages() {
        return countLocal(MESSAGES_INDEX_NAME);
    }
    
    public static long countLocalUsers() {
        return countLocal(USERS_INDEX_NAME);
    }

    public static long countLocalQueries() {
        return countLocal(QUERIES_INDEX_NAME);
    }
    
    private static long countLocal(String index) {
        CountResponse response = elasticsearch_client.prepareCount(index)
                .setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();
        return response.getCount();
    }
    
    public static boolean existMessage(String id) {
        return messages.exists(id);
    }
    
    public static boolean existUser(String id) {
        return users.exists(id);
    }
    
    public static boolean existQuery(String id) {
        return queries.exists(id);
    }
    
    public static boolean deleteQuery(String id, SourceType sourceType) {
        return queries.delete(id, sourceType);
    }
    
    public static void refreshQuery() {
        queries.refresh();
    }
    
    public static class SearchLocalMessages {
        public long hits;
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
        public SearchLocalMessages(String q, int timezoneOffset, int resultCount, int aggregationLimit, String... aggregationFields) {
            this.timeline = new Timeline();
            try {
                // prepare request
                QueryEntry.ElasticsearchQuery sq = new QueryEntry.ElasticsearchQuery(q, timezoneOffset);
                SearchRequestBuilder request = elasticsearch_client.prepareSearch(MESSAGES_INDEX_NAME)
                        .setSearchType(SearchType.QUERY_THEN_FETCH)
                        .setQuery(sq.queryBuilder)
                        .setFrom(0)
                        .setSize(resultCount);
                if (resultCount > 0) request.addSort("created_at", SortOrder.DESC);
                boolean addTimeHistogram = false;
                long interval = sq.until.getTime() - sq.since.getTime();
                DateHistogram.Interval dateHistogrammInterval = interval > 1000 * 60 * 60 * 24 * 7 ? DateHistogram.Interval.DAY : interval > 1000 * 60 * 60 * 3 ? DateHistogram.Interval.HOUR : DateHistogram.Interval.MINUTE;
                for (String field: aggregationFields) {
                    if (field.equals("created_at")) {
                        addTimeHistogram = true;
                        request.addAggregation(AggregationBuilders.dateHistogram("created_at").field("created_at").timeZone("UTC").minDocCount(0).interval(dateHistogrammInterval));
                    } else {
                        request.addAggregation(AggregationBuilders.terms(field).field(field).minDocCount(1).size(aggregationLimit));
                    }
                }
                
                // get response
                SearchResponse response = request.execute().actionGet();
                this.hits = response.getHits().getTotalHits();
                        
                // evaluate search result
                //long totalHitCount = response.getHits().getTotalHits();
                SearchHit[] hits = response.getHits().getHits();
                for (SearchHit hit: hits) {
                    Map<String, Object> map = hit.getSource();
                    MessageEntry tweet = new MessageEntry(map);
                    UserEntry user = users.read(tweet.getUserScreenName());
                    assert user != null;
                    if (user != null) {
                        timeline.addTweet(tweet);
                        timeline.addUser(user);
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
                        Calendar cal = Calendar.getInstance(DateParser.UTCtimeZone);
                        cal.setTime(bucket.getKeyAsDate().toDate());
                        cal.add(Calendar.MINUTE, -timezoneOffset);
                        long docCount = bucket.getDocCount();
                        Map.Entry<String,Long> entry = new AbstractMap.SimpleEntry<String, Long>(
                            (dateHistogrammInterval == DateHistogram.Interval.DAY ?
                                DateParser.dayDateFormat : DateParser.minuteDateFormat)
                            .format(cal.getTime()), docCount);
                        list.add(entry);
                    }
                    aggregations.put("created_at", list);
                }
            } catch (IndexMissingException e) {}
        }
    }

    /**
     * Search the local message cache using a elasticsearch query.
     * @param q - the query, can be empty for a matchall-query
     * @param resultCount - the number of messages in the result
     * @param sort_field - the field name to sort the result list, i.e. "query_first"
     * @param sort_order - the sort order (you want to use SortOrder.DESC here)
     */
    public static List<QueryEntry> SearchLocalQueries(final String q, final int resultCount, final String sort_field, final SortOrder sort_order, final Date since, final Date until, final String range_field) {
        List<QueryEntry> queries = new ArrayList<>();
        try {
            // prepare request
            BoolQueryBuilder suggest = QueryBuilders.boolQuery();
            if (q != null && q.length() > 0) {
                suggest.should(QueryBuilders.fuzzyLikeThisQuery("query").likeText(q).fuzziness(Fuzziness.fromEdits(2)));
                suggest.should(QueryBuilders.moreLikeThisQuery("query").likeText(q));
                suggest.should(QueryBuilders.matchPhrasePrefixQuery("query", q));
                if (q.indexOf('*') >= 0 || q.indexOf('?') >= 0) suggest.should(QueryBuilders.wildcardQuery("query", q));
            }

            BoolQueryBuilder query;
            
            if (range_field != null && range_field.length() > 0 && (since != null || until != null)) {
                query = QueryBuilders.boolQuery();
                if (q.length() > 0) query.must(suggest);
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(range_field);
                rangeQuery.from(since == null ? 0 : since.getTime());
                rangeQuery.to(until == null ? Long.MAX_VALUE : until.getTime());
                query.must(rangeQuery);
            } else {
                query = suggest;
            }
            
            SearchRequestBuilder request = elasticsearch_client.prepareSearch(QUERIES_INDEX_NAME)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setQuery(query)
                    .setFrom(0)
                    .setSize(resultCount)
                    .addSort(sort_field, sort_order);

            // get response
            SearchResponse response = request.execute().actionGet();

            // evaluate search result
            //long totalHitCount = response.getHits().getTotalHits();
            SearchHit[] hits = response.getHits().getHits();
            for (SearchHit hit: hits) {
                Map<String, Object> map = hit.getSource();
                queries.add(new QueryEntry(map));
            }
            
        } catch (IndexMissingException e) {}
        return queries;
    }
    
    public static Timeline[] scrapeTwitter(final String q, final int timezoneOffset, boolean byUserQuery) {
        // retrieve messages from remote server
        String[] remote = DAO.getConfig("frontpeers", new String[0], ",");        
        Timeline remoteMessages;
        if (remote.length > 0) {
            remoteMessages = searchOnOtherPeers(remote, q, 100, timezoneOffset, "twitter", SearchClient.frontpeer_hash);
            if (remoteMessages.size() == 0) {
                // maybe the remote server died, we try then ourself
                remoteMessages = TwitterScraper.search(q);
            }
        } else {
            remoteMessages = TwitterScraper.search(q);
        }
        
        // identify new tweets
        Timeline newMessages = new Timeline(); // we store new tweets here to be able to transmit them to peers
        if (remoteMessages == null) {// can be caused by time-out
            remoteMessages = new Timeline();
        } else {
            // record the result; this may be moved to a concurrent process
            for (MessageEntry t: remoteMessages) {
                UserEntry u = remoteMessages.getUser(t);
                assert u != null;
                boolean newTweet = writeMessage(t, u, true);
                if (newTweet) {
                    newMessages.addTweet(t);
                    newMessages.addUser(u);
                }
            }
            DAO.transmitTimeline(newMessages);
        }

        // record the query
        QueryEntry qe = queries.read(q);
        if (Caretaker.acceptQuery4Retrieval(q)) {
            if (qe == null) {
                // a new query occurred
                qe = new QueryEntry(q, timezoneOffset, remoteMessages, SourceType.TWITTER, byUserQuery);
            } else {
                // existing queries are updated
                qe.update(remoteMessages, byUserQuery);
            }
            try {
                queries.writeEntry(q, SourceType.TWITTER.name(), qe);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // accept rules may change, we want to delete the query then in the index
            if (qe != null) queries.delete(q, qe.source_type);
        }
        
        return new Timeline[]{remoteMessages, newMessages};
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
            for (MessageEntry t: tt) {
                UserEntry u = tt.getUser(t);
                assert u != null;
                writeMessage(t, u, true);
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
