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

import java.io.BufferedInputStream;
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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
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
import org.loklak.api.client.ClientConnection;
import org.loklak.api.client.SearchClient;
import org.loklak.geo.GeoJsonReader;
import org.loklak.geo.GeoNames;
import org.loklak.geo.LocationSource;
import org.loklak.geo.GeoJsonReader.Feature;
import org.loklak.geo.PlaceContext;
import org.loklak.harvester.SourceType;
import org.loklak.harvester.TwitterScraper;
import org.loklak.tools.DateParser;
import org.loklak.tools.JsonDataset;
import org.loklak.tools.JsonDump;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Data Access Object for the message project.
 * This provides only static methods because the class methods shall be available for
 * all other classes.
 */
public class DAO {

    public final static JsonFactory jsonFactory = new JsonFactory();
    public final static String MESSAGE_DUMP_FILE_PREFIX = "messages_";
    public final static String ACCOUNT_DUMP_FILE_PREFIX = "accounts_";
    public final static String QUERIES_INDEX_NAME = "queries";
    public final static String MESSAGES_INDEX_NAME = "messages";
    public final static String USERS_INDEX_NAME = "users";
    public final static String ACCOUNTS_INDEX_NAME = "accounts";
    public final static int CACHE_MAXSIZE = 10000;
    
    public  static File conf_dir;
    private static File external_data, assets, dictionaries;
    private static File message_dump_dir;
    private static JsonDump message_dump, account_dump;
    public  static JsonDataset twitter_user_dump;
    private static File settings_dir, customized_config;
    private static RandomAccessFile messagelog, accountlog;
    private static Node elasticsearch_node;
    private static Client elasticsearch_client;
    private static UserFactory users;
    private static AccountFactory accounts;
    private static MessageFactory messages;
    private static QueryFactory queries;
    private static BlockingQueue<Timeline> newMessageTimelines = new LinkedBlockingQueue<Timeline>();
    private static Map<String, String> config = new HashMap<>();
    public  static GeoNames geoNames;
    
    /**
     * initialize the DAO
     * @param datadir the path to the data directory
     */
    public static void init(File datadir) {
        try {
            // create and document the data dump dir
            assets = new File(datadir, "assets");
            external_data = new File(datadir, "external");
            geoJson_import = new File(new File(external_data, "geojson"), "import");
            geoJson_imported = new File(new File(external_data, "geojson"), "imported");
            geoJson_import.mkdirs();
            geoJson_imported.mkdirs();
            dictionaries = new File(external_data, "dictionaries");
            dictionaries.mkdirs();
            
            // load dictionaries if they are embedded here
            // read the file allCountries.zip from http://download.geonames.org/export/dump/allCountries.zip
            //File allCountries = new File(dictionaries, "allCountries.zip");
            File cities1000 = new File(dictionaries, "cities1000.zip");
            if (!cities1000.exists()) {
                // download this file
                ClientConnection.download("http://download.geonames.org/export/dump/cities1000.zip", cities1000);
            }
            if (cities1000.exists()) {
                geoNames = new GeoNames(cities1000, 1);
            } else {
                geoNames = null;
            }
            
            // create message dump dir
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
            messagelog = new RandomAccessFile(getCurrentDump(message_dump_dir_own, MESSAGE_DUMP_FILE_PREFIX), "rw");
            File account_dump_dir = new File(datadir, "accounts");
            account_dump_dir.mkdirs();
            accountlog = new RandomAccessFile(getCurrentDump(account_dump_dir, ACCOUNT_DUMP_FILE_PREFIX), "rw");
            
            // load the config file(s);
            conf_dir = new File("conf");
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(conf_dir, "config.properties")));
            for (Map.Entry<Object, Object> entry: prop.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
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
            for (Map.Entry<Object, Object> entry: customized_config_props.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
            
            // use all config attributes with a key starting with "elasticsearch." to set elasticsearch settings
            Builder builder = ImmutableSettings.settingsBuilder();
            for (Map.Entry<String, String> entry: config.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("elasticsearch.")) builder.put(key.substring(14), entry.getValue());
            }

            // start elasticsearch
            elasticsearch_node = NodeBuilder.nodeBuilder().settings(builder).node();
            elasticsearch_client = elasticsearch_node.client();
            
            // define the index factories
            messages = new MessageFactory(elasticsearch_client, MESSAGES_INDEX_NAME, CACHE_MAXSIZE);
            users = new UserFactory(elasticsearch_client, USERS_INDEX_NAME, CACHE_MAXSIZE);
            accounts = new AccountFactory(elasticsearch_client, ACCOUNTS_INDEX_NAME, CACHE_MAXSIZE);
            queries = new QueryFactory(elasticsearch_client, QUERIES_INDEX_NAME, CACHE_MAXSIZE);
            
            // set mapping (that shows how 'elastic' elasticsearch is: it's always good to define data types)
            try {
                elasticsearch_client.admin().indices().prepareCreate(MESSAGES_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(USERS_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(ACCOUNTS_INDEX_NAME).execute().actionGet();
                elasticsearch_client.admin().indices().prepareCreate(QUERIES_INDEX_NAME).execute().actionGet();
            } catch (IndexAlreadyExistsException ee) {}; // existing indexes are simply ignored, not re-created
            elasticsearch_client.admin().indices().preparePutMapping(MESSAGES_INDEX_NAME).setSource(messages.getMapping()).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(USERS_INDEX_NAME).setSource(users.getMapping()).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(ACCOUNTS_INDEX_NAME).setSource(accounts.getMapping()).setType("_default_").execute().actionGet();
            elasticsearch_client.admin().indices().preparePutMapping(QUERIES_INDEX_NAME).setSource(queries.getMapping()).setType("_default_").execute().actionGet();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public static File getAssetFile(String screen_name, String id_str, String file) {
        String letter0 = ("" + screen_name.charAt(0)).toLowerCase();
        String letter1 = ("" + screen_name.charAt(1)).toLowerCase();
        File storage_path = new File(new File(new File(assets, letter0), letter1), screen_name);
        return new File(storage_path, id_str + "_" + file); // all assets for one user in one file
    }
    
    private static File getCurrentDump(File path, String prefix) {
        SimpleDateFormat formatYearMonth = new SimpleDateFormat("yyyyMM", Locale.US);
        formatYearMonth.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentDatePart = formatYearMonth.format(new Date());
        
        // if there is already a dump, use it
        String[] existingDumps = path.list();
        if (existingDumps != null) for (String d: existingDumps) {
            if (d.startsWith(prefix + currentDatePart) && d.endsWith(".txt")) {
                return new File(path, d);
            }
            
            // in case the file is a dump file but ends with '.txt', we compress it here on-the-fly
            if (d.startsWith(prefix) && d.endsWith(".txt")) {
                final File source = new File(path, d);
                final File dest = new File(path, d + ".gz");
                new Thread() {
                    public void run() {
                        byte[] buffer = new byte[2^20];
                        try {
                            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(dest), 65536){{def.setLevel(Deflater.BEST_COMPRESSION);}};
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
        return new File(path, prefix + currentDatePart + "_" + random + ".txt");
    }

    public static File[] getTweetOwnDumps() {
        return getDumps(message_dump_dir_own, MESSAGE_DUMP_FILE_PREFIX, null);
    }
    public static File[] getTweetImportDumps() {
        return getDumps(message_dump_dir_import, MESSAGE_DUMP_FILE_PREFIX, null);
    }
    public static File[] getTweetImportedDumps() {
        return getDumps(message_dump_dir_imported, MESSAGE_DUMP_FILE_PREFIX, null);
    }
    public static File[] getGeoJsonImportDumps() {
        return getDumps(geoJson_import, null, ".json");
    }
    public static File[] getGeoJsonImportedDumps() {
        return getDumps(geoJson_imported, null, ".json");
    }
    private static File[] getDumps(final File path, final String prefix, final String suffix) {
        String[] list = path.list();
        TreeSet<File> dumps = new TreeSet<File>(); // sort the names with a tree set
        for (String s: list) {
            if ((prefix == null || s.startsWith(prefix)) &&
                (suffix == null || s.endsWith(suffix))) dumps.add(new File(path, s));
        }
        return dumps.toArray(new File[dumps.size()]);
    }
    public static boolean shiftProcessedTweetDump(String dumpName) {
        return shiftProcessedDump(dumpName, message_dump_dir_import, message_dump_dir_imported);
    }
    public static boolean shiftProcessedGeoJsonDump(String dumpName) {
        return shiftProcessedDump(dumpName, geoJson_import, geoJson_imported);
    }
    public static boolean shiftProcessedDump(String dumpName, File from, File to) {
        File f = new File(from, dumpName);
        if (!f.exists()) return false;
        File g = new File(to, dumpName);
        if (g.exists()) g.delete();
        return f.renameTo(g);
    }
    public static int importDump(File dumpFile) {
        int concurrency = Runtime.getRuntime().availableProcessors();
        final DumpReader dumpReader = new DumpReader(dumpFile, concurrency);
        dumpReader.start();
        final AtomicInteger newTweet = new AtomicInteger(0);
        Thread[] indexerThreads = new Thread[concurrency];
        for (int i = 0; i < concurrency; i++) {
            indexerThreads[i] = new Thread() {
                public void run() {
                    Map<String, Object> tweet;
                    try {
                        while ((tweet = dumpReader.take()) != POISON_TWEET_MAP) {
                            @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) tweet.remove("user");
                            if (user == null) continue;
                            UserEntry u = new UserEntry(user);
                            MessageEntry t = new MessageEntry(tweet);
                            boolean newtweet = DAO.writeMessage(t, u, false, true);
                            if (newtweet) newTweet.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            indexerThreads[i].start();
        }
        for (int i = 0; i < concurrency; i++) {
            try {indexerThreads[i].join();} catch (InterruptedException e) {}
        }
        return newTweet.get();
    }
    private final static Map<String, Object> POISON_TWEET_MAP = new HashMap<>();
    public static class DumpReader extends Thread {

        private ArrayBlockingQueue<Map<String, Object>> tweets;
        private File dumpFile;
        private int concurrency;
        
        public DumpReader(File dumpFile, int concurrency) {
            this.tweets = new ArrayBlockingQueue<>(1000);
            this.dumpFile = dumpFile;
            this.concurrency = concurrency;
        }
        
        public Map<String, Object> take() throws InterruptedException {
            return this.tweets.take();
        }
        
        public void run() {
            try {
                InputStream is = new FileInputStream(this.dumpFile);
                String line;
                BufferedReader br = null;
                try {
                    if (dumpFile.getName().endsWith(".gz")) is = new GZIPInputStream(is);
                    br = new BufferedReader(new InputStreamReader(is, UTF8.charset));
                    while((line = br.readLine()) != null) {
                        try {
                            XContentParser parser = JsonXContent.jsonXContent.createParser(line);
                            Map<String, Object> tweet = parser == null ? null : parser.map();
                            if (tweet == null) continue;
                            this.tweets.put(tweet);
                        } catch (Throwable e) {
                            Log.getLog().warn("cannot parse line \"" + line + "\"", e);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (br != null) br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < this.concurrency; i++) {
                        try {this.tweets.put(POISON_TWEET_MAP);} catch (InterruptedException e) {}
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static int importGeoJson(File dumpFile) {
        try {
            int newTweet = 0;
            InputStream is = new BufferedInputStream(new FileInputStream(dumpFile));
            Date commonCreationDate = new Date();
            try {
                GeoJsonReader reader = new GeoJsonReader(is, Runtime.getRuntime().availableProcessors() * 2 + 1);
                new Thread(reader).start();
                Feature feature;
                while ((feature = reader.take()) != GeoJsonReader.POISON_FEATURE) {
                    String screen_name = "*geojson*";
                    String provider_hash = Integer.toHexString(dumpFile.hashCode());
                    String id = feature.id.length() == 0 ? feature.properties.get("id") : feature.id;
                    URL url = null;
                    try {url = new URL(feature.properties.get("url"));} catch (MalformedURLException e) {}
                    
                    MessageEntry t = new MessageEntry();
                    t.setCreatedAt(commonCreationDate);
                    t.setSourceType(SourceType.IMPORT);
                    t.setProviderType(ProviderType.GENERIC);
                    t.setProviderHash(provider_hash);
                    t.setScreenName(screen_name);
                    t.setIdStr(provider_hash + "_" + id);
                    t.setText(feature.properties.get("description"));
                    if (url != null) t.setStatusIdUrl(url);
                    t.setRetweetCount(0);
                    t.setFavouritesCount(0);
                    t.setImages(feature.properties.get("thumbs"));
                    t.setPlaceId("");
                    t.setPlaceName(feature.properties.get("title"), PlaceContext.FROM);
                    double lon = Double.parseDouble(feature.properties.get("geo_longitude"));
                    double lat = Double.parseDouble(feature.properties.get("geo_latitude"));
                    t.setLocationPoint(new double[]{lon, lat}); // coordinate order is longitude, latitude
                    t.setLocationMark(new double[]{lon, lat}); // coordinate order is longitude, latitude
                    t.setLocationRadius(0);
                    t.setLocationSource(LocationSource.REPORT);
                    t.enrich();
                    
                    UserEntry u = new UserEntry(screen_name, "", "");
                    boolean newtweet = DAO.writeMessage(t, u, false, false);
                    if (newtweet) newTweet++;
                    
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
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
        String value = config.get(key);
        return value == null ? default_val : value;
    }
    
    public static String[] getConfig(String key, String[] default_val, String delim) {
        String value = config.get(key);
        return value == null || value.length() == 0 ? default_val : value.split(delim);
    }
    
    public static long getConfig(String key, long default_val) {
        String value = config.get(key);
        try {
            return value == null ? default_val : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return default_val;
        }
    }
    
    public static boolean getConfig(String key, boolean default_val) {
        String value = config.get(key);
        return value == null ? default_val : value.equals("true") || value.equals("on") || value.equals("1");
    }
    
    public static Set<String> getConfigKeys() {
        return config.keySet();
    }
    
    public static void transmitTimeline(Timeline tl) {
        if (getConfig("backend", new String[0], ",").length > 0) newMessageTimelines.add(tl);
    }

    public static Timeline takeTimeline(Timeline.Order order, int maxsize, long maxwait) {
        Timeline tl = new Timeline(order);
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
    public synchronized static boolean writeMessage(MessageEntry t, UserEntry u, boolean dump, boolean overwriteUser) {
        try {

            // check if tweet exists in index
            if ((t instanceof TwitterScraper.TwitterTweet &&
                ((TwitterScraper.TwitterTweet) t).exist() != null &&
                ((TwitterScraper.TwitterTweet) t).exist().booleanValue()) ||
                messages.exists(t.getIdStr())) return false; // we omit writing this again

            // check if user exists in index
            if (overwriteUser) {
                UserEntry oldUser = users.read(u.getScreenName());
                if (oldUser == null || !oldUser.equals(u)) {
                    writeUser(u, t.getSourceType().name());
                }
            } else {
                if (!users.exists(u.getScreenName())) {
                    writeUser(u, t.getSourceType().name());
                } 
            }

            // record tweet into text file
            if (dump) {
                messagelog.seek(messagelog.length()); // go to end of file
                messagelog.write(UTF8.getBytes(new ObjectMapper().writer().writeValueAsString(t.toMap(u, false))));
                messagelog.writeByte('\n');
            }

            // record tweet into search index
            messages.writeEntry(t.getIdStr(), t.getSourceType().name(), t);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * Store an user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param a an account 
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public synchronized static boolean writeUser(UserEntry u, String source_type) {
        try {
            // record user into search index
            users.writeEntry(u.getScreenName(), source_type, u);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * Store an account together with a user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param a an account 
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public synchronized static boolean writeAccount(AccountEntry a, boolean dump) {
        try {
            // record account into text file
            if (dump) {
                accountlog.seek(accountlog.length()); // go to end of file
                accountlog.write(UTF8.getBytes(new ObjectMapper().writer().writeValueAsString(a.toMap(null))));
                accountlog.writeByte('\n');
            }

            // record tweet into search index
            accounts.writeEntry(a.getScreenName(), a.getSourceType().name(), a);
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
    
    public static long countLocalAccounts() {
        return countLocal(ACCOUNTS_INDEX_NAME);
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
    
    public static class SearchLocalMessages {
        public long hits;
        public Timeline timeline;
        public Map<String, List<Map.Entry<String, Long>>> aggregations;

        /**
         * Search the local message cache using a elasticsearch query.
         * @param q - the query, for aggregation this which should include a time frame in the form since:yyyy-MM-dd until:yyyy-MM-dd
         * @param order_field - the field to order the results, i.e. Timeline.Order.CREATED_AT
         * @param timezoneOffset - an offset in minutes that is applied on dates given in the query of the form since:date until:date
         * @param resultCount - the number of messages in the result; can be zero if only aggregations are wanted
         * @param dateHistogrammInterval - the date aggregation interval or null, if no aggregation wanted
         * @param aggregationLimit - the maximum count of facet entities, not search results
         * @param aggregationFields - names of the aggregation fields. If no aggregation is wanted, pass no (zero) field(s)
         */
        public SearchLocalMessages(final String q, Timeline.Order order_field, int timezoneOffset, int resultCount, int aggregationLimit, String... aggregationFields) {
            this.timeline = new Timeline(order_field);
            try {
                // prepare request
                QueryEntry.ElasticsearchQuery sq = new QueryEntry.ElasticsearchQuery(q, timezoneOffset);
                SearchRequestBuilder request = elasticsearch_client.prepareSearch(MESSAGES_INDEX_NAME)
                        .setSearchType(SearchType.QUERY_THEN_FETCH)
                        .setQuery(sq.queryBuilder)
                        .setFrom(0)
                        .setSize(resultCount);
                request.clearRescorers();
                if (resultCount > 0) request.addSort(order_field.getMessageFieldName(), SortOrder.DESC);
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
                    try {
                        UserEntry user = users.read(tweet.getScreenName());
                        assert user != null;
                        if (user != null) {
                            timeline.addTweet(tweet);
                            timeline.addUser(user);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
                // evaluate aggregation
                // collect results: fields
                this.aggregations = new HashMap<>();
                for (String field: aggregationFields) {
                    if (field.equals("created_at")) continue; // this has special handling below
                    Terms fieldCounts = response.getAggregations().get(field);
                    List<Bucket> buckets = fieldCounts.getBuckets();
                    // aggregate double-tokens (matching lowercase)
                    Map<String, Long> checkMap = new HashMap<>();
                    for (Bucket bucket: buckets) {
                        if (bucket.getKey().trim().length() > 0) {
                            String k = bucket.getKey().toLowerCase();
                            Long v = checkMap.get(k);
                            checkMap.put(k, v == null ? bucket.getDocCount() : v + bucket.getDocCount());
                        }
                    }
                    ArrayList<Map.Entry<String, Long>> list = new ArrayList<>(buckets.size());
                    for (Bucket bucket: buckets) {
                        if (bucket.getKey().trim().length() > 0) {
                            Long v = checkMap.remove(bucket.getKey().toLowerCase());
                            if (v == null) continue;
                            list.add(new AbstractMap.SimpleEntry<String, Long>(bucket.getKey(), v));
                        }
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
     * Search the local user cache using a elasticsearch query.
     * @param screen_name - the user id
     */
    public static UserEntry searchLocalUser(final String screen_name) {
        try {
            return users.read(screen_name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        /*
        if (screen_name == null || screen_name.length() == 0) return null;
        try {
            // prepare request
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            query.must(QueryBuilders.termQuery("screen_name", screen_name));

            SearchRequestBuilder request = elasticsearch_client.prepareSearch(QUERIES_INDEX_NAME)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setQuery(query)
                    .setFrom(0)
                    .setSize(1);

            // get response
            SearchResponse response = request.execute().actionGet();

            // evaluate search result
            //long totalHitCount = response.getHits().getTotalHits();
            SearchHit[] hits = response.getHits().getHits();
            if (hits.length == 0) return null;
            assert hits.length == 1;
            Map<String, Object> map = hits[1].getSource();
            return new UserEntry(map);            
        } catch (IndexMissingException e) {}
        return null;
        */
    }

    /**
     * Search the local account cache using a elasticsearch query.
     * @param screen_name - the user id
     */
    public static AccountEntry searchLocalAccount(final String screen_name) {
        try {
            return accounts.read(screen_name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
    
    public static Timeline[] scrapeTwitter(final String q, final Timeline.Order order, final int timezoneOffset, boolean byUserQuery) {
        // retrieve messages from remote server
        String[] remote = DAO.getConfig("frontpeers", new String[0], ",");        
        Timeline remoteMessages;
        if (remote.length > 0) {
            remoteMessages = searchOnOtherPeers(remote, q, order, 100, timezoneOffset, "twitter", SearchClient.frontpeer_hash);
            if (remoteMessages.size() == 0) {
                // maybe the remote server died, we try then ourself
                remoteMessages = TwitterScraper.search(q, order);
            }
        } else {
            remoteMessages = TwitterScraper.search(q, order);
        }
        
        // identify new tweets
        Timeline newMessages = new Timeline(order); // we store new tweets here to be able to transmit them to peers
        if (remoteMessages == null) {// can be caused by time-out
            remoteMessages = new Timeline(order);
        } else {
            // record the result; this may be moved to a concurrent process
            for (MessageEntry t: remoteMessages) {
                UserEntry u = remoteMessages.getUser(t);
                assert u != null;
                boolean newTweet = writeMessage(t, u, true, true);
                if (newTweet) {
                    newMessages.addTweet(t);
                    newMessages.addUser(u);
                }
            }
            DAO.transmitTimeline(newMessages);
        }

        // record the query
        QueryEntry qe = null;
        try {
            qe = queries.read(q);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (Caretaker.acceptQuery4Retrieval(q)) {
            if (qe == null) {
                // a new query occurred
                qe = new QueryEntry(q, timezoneOffset, remoteMessages.period(), SourceType.TWITTER, byUserQuery);
            } else {
                // existing queries are updated
                qe.update(remoteMessages.period(), byUserQuery);
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
    
    public static Timeline searchBackend(final String q, final Timeline.Order order, final int count, final int timezoneOffset, final String where) {
        String[] remote = DAO.getConfig("backend", new String[0], ",");
        return searchOnOtherPeers(remote, q, order, count, timezoneOffset, where, SearchClient.backend_hash);
    }
    
    public static Timeline searchOnOtherPeers(final String[] remote, final String q, final Timeline.Order order, final int count, final int timezoneOffset, final String where, final String provider_hash) {
        Timeline tl = new Timeline(order);
        for (String protocolhostportstub: remote) {
            Timeline tt = SearchClient.search(protocolhostportstub, q, order, where, count, timezoneOffset, provider_hash);
            tl.putAll(tt);
            // record the result; this may be moved to a concurrent process
            for (MessageEntry t: tt) {
                UserEntry u = tt.getUser(t);
                assert u != null;
                writeMessage(t, u, true, false);
            }
        }
        return tl;
    }
    
    public static void log(String line) {
        Log.getLog().info(line);
    }

}
