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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.ConcurrentHashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.Caretaker;
import org.loklak.api.client.SearchClient;
import org.loklak.geo.GeoNames;
import org.loklak.harvester.SourceType;
import org.loklak.harvester.TwitterScraper;
import org.loklak.http.AccessTracker;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.tools.DateParser;
import org.loklak.tools.OS;
import org.loklak.tools.storage.JsonDataset;
import org.loklak.tools.storage.JsonReader;
import org.loklak.tools.storage.JsonRepository;
import org.loklak.tools.storage.JsonStreamReader;
import org.loklak.tools.storage.JsonFactory;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * The Data Access Object for the message project.
 * This provides only static methods because the class methods shall be available for
 * all other classes.
 * 
 * To debug, call elasticsearch directly i.e.:
 * 
 * get statistics
 * curl localhost:9200/_stats?pretty=true
 * 
 * get statistics for message index
 * curl -XGET 'http://127.0.0.1:9200/messages?pretty=true'
 * 
 * get mappings in message index
 * curl -XGET "http://localhost:9200/messages/_mapping?pretty=true"
 * 
 * get search result from message index
 * curl -XGET 'http://127.0.0.1:9200/messages/_search?q=*&pretty=true'
 */
public class DAO {

    public final static com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory();
    public final static ObjectMapper jsonMapper = new ObjectMapper(DAO.jsonFactory);
    public final static TypeReference<HashMap<String,Object>> jsonTypeRef = new TypeReference<HashMap<String,Object>>() {};

    public final static String MESSAGE_DUMP_FILE_PREFIX = "messages_";
    public final static String ACCOUNT_DUMP_FILE_PREFIX = "accounts_";
    public final static String USER_DUMP_FILE_PREFIX = "users_";
    public final static String ACCESS_DUMP_FILE_PREFIX = "access_";
    public final static String FOLLOWERS_DUMP_FILE_PREFIX = "followers_";
    public final static String FOLLOWING_DUMP_FILE_PREFIX = "following_";
    private static final String IMPORT_PROFILE_FILE_PREFIX = "profile_";
    public final static String QUERIES_INDEX_NAME = "queries";
    public final static String MESSAGES_INDEX_NAME = "messages";
    public final static String USERS_INDEX_NAME = "users";
    public final static String ACCOUNTS_INDEX_NAME = "accounts";
    private static final String IMPORT_PROFILE_INDEX_NAME = "import_profiles";
    public final static int CACHE_MAXSIZE = 10000;
    
    public  static File conf_dir, bin_dir;
    private static File external_data, assets, dictionaries;
    private static Path message_dump_dir, account_dump_dir, import_profile_dump_dir;
    private static JsonRepository message_dump, account_dump, import_profile_dump;
    public  static JsonDataset user_dump, followers_dump, following_dump;
    public  static AccessTracker access;
    private static File schema_dir, conv_schema_dir;
    private static Node elasticsearch_node;
    private static Client elasticsearch_client;
    public static UserFactory users;
    private static AccountFactory accounts;
    public static MessageFactory messages;
    public static QueryFactory queries;
    private static ImportProfileFactory importProfiles;
    private static Map<String, String> config = new HashMap<>();
    public  static GeoNames geoNames;
    
    /**
     * initialize the DAO
     * @param datadir the path to the data directory
     */
    public static void init(Map<String, String> configMap, Path dataPath) {
        config = configMap;
        conf_dir = new File("conf");
        bin_dir = new File("bin");
        File datadir = dataPath.toFile();
        try {
            // create and document the data dump dir
            assets = new File(datadir, "assets");
            external_data = new File(datadir, "external");
            dictionaries = new File(external_data, "dictionaries");
            dictionaries.mkdirs();
            
            // create message dump dir
            String message_dump_readme =
                "This directory contains dump files for messages which arrived the platform.\n" +
                "There are three subdirectories for dump files:\n" +
                "- own:      for messages received with this peer. There is one file for each month.\n" +
                "- import:   hand-over directory for message dumps to be imported. Drop dumps here and they are imported.\n" +
                "- imported: dump files which had been processed from the import directory are moved here.\n" +
                "You can import dump files from other peers by dropping them into the import directory.\n" +
                "Each dump file must start with the prefix '" + MESSAGE_DUMP_FILE_PREFIX + "' to be recognized.\n";
            message_dump_dir = dataPath.resolve("dump");
            message_dump = new JsonRepository(message_dump_dir.toFile(), MESSAGE_DUMP_FILE_PREFIX, message_dump_readme, JsonRepository.COMPRESSED_MODE, Runtime.getRuntime().availableProcessors());
            
            account_dump_dir = dataPath.resolve("accounts");
            account_dump_dir.toFile().mkdirs();
            OS.protectPath(account_dump_dir); // no other permissions to this path
            account_dump = new JsonRepository(account_dump_dir.toFile(), ACCOUNT_DUMP_FILE_PREFIX, null, JsonRepository.REWRITABLE_MODE, Runtime.getRuntime().availableProcessors());

            File user_dump_dir = new File(datadir, "accounts");
            user_dump_dir.mkdirs();
            user_dump = new JsonDataset(
                    user_dump_dir,USER_DUMP_FILE_PREFIX,
                    new JsonDataset.Column[]{new JsonDataset.Column("id_str", false), new JsonDataset.Column("screen_name", true)},
                    "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                    JsonRepository.REWRITABLE_MODE);
            followers_dump = new JsonDataset(
                    user_dump_dir, FOLLOWERS_DUMP_FILE_PREFIX,
                    new JsonDataset.Column[]{new JsonDataset.Column("screen_name", true)},
                    "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                    JsonRepository.REWRITABLE_MODE);
            following_dump = new JsonDataset(
                    user_dump_dir, FOLLOWING_DUMP_FILE_PREFIX,
                    new JsonDataset.Column[]{new JsonDataset.Column("screen_name", true)},
                    "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                    JsonRepository.REWRITABLE_MODE);
            
            Path log_dump_dir = dataPath.resolve("log");
            log_dump_dir.toFile().mkdirs();
            OS.protectPath(log_dump_dir); // no other permissions to this path
            access = new AccessTracker(log_dump_dir.toFile(), ACCESS_DUMP_FILE_PREFIX, 60000, 3000);
            access.start(); // start monitor
            
	        import_profile_dump_dir = dataPath.resolve("import-profiles");
            import_profile_dump = new JsonRepository(import_profile_dump_dir.toFile(), IMPORT_PROFILE_FILE_PREFIX, null, JsonRepository.COMPRESSED_MODE, Runtime.getRuntime().availableProcessors());

            // load schema folder
            conv_schema_dir = new File("conf/conversion");
            schema_dir = new File("conf/schema");            

            // use all config attributes with a key starting with "elasticsearch." to set elasticsearch settings
            Settings.Builder settings = Settings.builder();
            for (Map.Entry<String, String> entry: config.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("elasticsearch.")) settings.put(key.substring(14), entry.getValue());
            }
            // patch the home path
            settings.put("path.home", datadir.getAbsolutePath());
            settings.put("path.data", datadir.getAbsolutePath());
            settings.build();

            // load dictionaries if they are embedded here
            // read the file allCountries.zip from http://download.geonames.org/export/dump/allCountries.zip
            //File allCountries = new File(dictionaries, "allCountries.zip");
            File cities1000 = new File(dictionaries, "cities1000.zip");
            if (!cities1000.exists()) {
                // download this file
                ClientConnection.download("http://download.geonames.org/export/dump/cities1000.zip", cities1000);
            }
            if (cities1000.exists()) {
                geoNames = new GeoNames(cities1000, new File(conf_dir, "iso3166.json"), 1);
            } else {
                geoNames = null;
            }
            
            // start elasticsearch
            elasticsearch_node = NodeBuilder.nodeBuilder().settings(settings).node();
            elasticsearch_client = elasticsearch_node.client(); // TransportClient.builder().settings(settings).build();

            Path index_dir = dataPath.resolve("index");
            if (index_dir.toFile().exists()) OS.protectPath(index_dir); // no other permissions to this path
            
            // define the index factories
            messages = new MessageFactory(elasticsearch_client, MESSAGES_INDEX_NAME, CACHE_MAXSIZE);
            users = new UserFactory(elasticsearch_client, USERS_INDEX_NAME, CACHE_MAXSIZE);
            accounts = new AccountFactory(elasticsearch_client, ACCOUNTS_INDEX_NAME, CACHE_MAXSIZE);
            queries = new QueryFactory(elasticsearch_client, QUERIES_INDEX_NAME, CACHE_MAXSIZE);
            importProfiles = new ImportProfileFactory(elasticsearch_client, IMPORT_PROFILE_INDEX_NAME, CACHE_MAXSIZE);

            // create indices
            try {elasticsearch_client.admin().indices().prepareCreate(MESSAGES_INDEX_NAME).execute().actionGet();} catch (Throwable e) {};
            try {elasticsearch_client.admin().indices().prepareCreate(USERS_INDEX_NAME).execute().actionGet();} catch (Throwable e) {};
            try {elasticsearch_client.admin().indices().prepareCreate(ACCOUNTS_INDEX_NAME).execute().actionGet();} catch (Throwable e) {};
            try {elasticsearch_client.admin().indices().prepareCreate(QUERIES_INDEX_NAME).execute().actionGet();} catch (Throwable e) {};
            try {elasticsearch_client.admin().indices().prepareCreate(IMPORT_PROFILE_INDEX_NAME).execute().actionGet();} catch (Throwable e) {};

            // set mapping (that shows how 'elastic' elasticsearch is: it's always good to define data types)
            try {elasticsearch_client.admin().indices().preparePutMapping(MESSAGES_INDEX_NAME).setSource(messages.getMapping()).setType("_default_").execute().actionGet();} catch (Throwable e) {e.printStackTrace();};
            try {elasticsearch_client.admin().indices().preparePutMapping(USERS_INDEX_NAME).setSource(users.getMapping()).setType("_default_").execute().actionGet();} catch (Throwable e) {e.printStackTrace();};
            try {elasticsearch_client.admin().indices().preparePutMapping(ACCOUNTS_INDEX_NAME).setSource(accounts.getMapping()).setType("_default_").execute().actionGet();} catch (Throwable e) {e.printStackTrace();};
            try {elasticsearch_client.admin().indices().preparePutMapping(QUERIES_INDEX_NAME).setSource(queries.getMapping()).setType("_default_").execute().actionGet();} catch (Throwable e) {e.printStackTrace();};
            try {elasticsearch_client.admin().indices().preparePutMapping(IMPORT_PROFILE_INDEX_NAME).setSource(importProfiles.getMapping()).setType("_default_").execute().actionGet();} catch (Throwable e) {e.printStackTrace();};
            
            // finally wait for healty status of shards
            ClusterHealthResponse health;
            do {
                log("Waiting for elasticsearch yellow status");
                health = elasticsearch_client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
            } while (health.isTimedOut());
            /**
            do {
                log("Waiting for elasticsearch green status");
                health = elasticsearch_client.admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
            } while (health.isTimedOut());
            **/
            log("elasticsearch has started up! initializing the classifier");
            
            // start the classifier
            Classifier.init(10000, 1000);
            log("classifier initialized!");
            
            // initialize query harvesting
            if (getConfig("retrieval.queries.enabled", false)) {
                File harvestingPath = new File(datadir, "queries");
                if (!harvestingPath.exists()) harvestingPath.mkdirs();
                String[] list = harvestingPath.list();
                for (String queryfile: list) {
                    if (queryfile.startsWith(".") || queryfile.endsWith("~")) continue;
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(harvestingPath, queryfile))));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim().toLowerCase();
                            if (line.length() == 0) continue;
                            if (line.charAt(0) <= '9') {
                                // truncate statistic
                                int p = line.indexOf(' ');
                                if (p < 0) continue;
                                line = line.substring(p + 1).trim();
                            }
                            // write line into query database
                            if (!existQuery(line)) {
                                try {
                                    queries.writeEntry(
                                            line,
                                            SourceType.TWITTER.name(),
                                            new QueryEntry(line, 0, 60000, SourceType.TWITTER, false),
                                            true);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                queries.bulkCacheFlush();
            }
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
    
    public static Collection<File> getTweetOwnDumps() {
        return message_dump.getOwnDumps();
    }

    public static int importMessageDumps() throws IOException {
        int imported = 0;
        Collection<JsonReader> message_readers = message_dump.getImportDumpReaders(true);
        if (message_readers == null || message_readers.size() == 0) return 0;
        for (JsonReader reader: message_readers) {
            imported += importMessageDump(reader);
        }
        message_dump.shiftProcessedDumps();
        return imported;
    }
    
    public static void importAccountDumps() throws IOException {
        Collection<JsonReader> account_readers = account_dump.getImportDumpReaders(true);
        if (account_readers == null || account_readers.size() == 0) return;
        for (JsonReader reader: account_readers) {
            importAccountDump(reader);
        }
        account_dump.shiftProcessedDumps();
    }

    public static long importMessageDump(final JsonReader dumpReader) {
        (new Thread(dumpReader)).start();
        final AtomicLong newTweets = new AtomicLong(0);
        Thread[] indexerThreads = new Thread[dumpReader.getConcurrency()];
        for (int i = 0; i < dumpReader.getConcurrency(); i++) {
            indexerThreads[i] = new Thread() {
                public void run() {
                    JsonFactory tweet;
                    try {
                        while ((tweet = dumpReader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                            try {
                                Map<String, Object> json = tweet.getJson();
                                @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) json.remove("user");
                                if (user == null) continue;
                                UserEntry u = new UserEntry(user);
                                MessageEntry t = new MessageEntry(json);
                                DAO.writeMessage(t, u, false, false, true, true);
                                newTweets.incrementAndGet();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            indexerThreads[i].start();
        }
        boolean running = true;
        while (running) {
            long startTime = System.currentTimeMillis();
            long startCount = newTweets.get();
            running = false;
            for (int i = 0; i < dumpReader.getConcurrency(); i++) {
                if (indexerThreads[i].isAlive()) running = true;
            }
            try {Thread.sleep(10000);} catch (InterruptedException e) {}
            long runtime = System.currentTimeMillis() - startTime;
            long count = newTweets.get() - startCount;
            Log.getLog().info("imported " + newTweets.get() + " tweets at " + (count * 1000 / runtime) + " tweets per second");
        }
        try {DAO.users.bulkCacheFlush();} catch (IOException e) {}
        try {DAO.messages.bulkCacheFlush();} catch (IOException e) {}
        return newTweets.get();
    }
    
    public static void importAccountDump(final JsonReader dumpReader) {
        (new Thread(dumpReader)).start();
        Thread[] indexerThreads = new Thread[dumpReader.getConcurrency()];
        for (int i = 0; i < dumpReader.getConcurrency(); i++) {
            indexerThreads[i] = new Thread() {
                public void run() {
                    JsonFactory accountEntry;
                    try {
                        while ((accountEntry = dumpReader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                            try {
                                Map<String, Object> json = accountEntry.getJson();
                                AccountEntry a = new AccountEntry(json);
                                DAO.writeAccount(a, false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            indexerThreads[i].start();
        }
        for (int i = 0; i < dumpReader.getConcurrency(); i++) {
            try {indexerThreads[i].join();} catch (InterruptedException e) {}
        }
    }
    
    /**
     * close all objects in this class
     */
    public static void close() {
        Log.getLog().info("closing DAO");
        
        // close the dump files
        message_dump.close();
        account_dump.close();
        import_profile_dump.close();
        user_dump.close();
        followers_dump.close();
        following_dump.close();
        
        // close the tracker
        access.close();
        
        // close the index factories (flushes the caches)
        messages.close();
        users.close();
        accounts.close();
        queries.close();
        importProfiles.close();

        // close the index
        elasticsearch_client.close();
        elasticsearch_node.close();
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
    
    public static double getConfig(String key, double default_val) {
        String value = config.get(key);
        try {
            return value == null ? default_val : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return default_val;
        }
    }

    public static JsonNode getSchema(String key) throws IOException {
        File schema = new File(schema_dir, key);
        if (!schema.exists()) {
            throw new FileNotFoundException("No schema file with name " + key + " found");
        }
        return JsonLoader.fromFile(schema);
    }

    public static Map<String, Object> getConversionSchema(String key) throws IOException {
        File schema = new File(conv_schema_dir, key);
        if (!schema.exists()) {
            throw new FileNotFoundException("No schema file with name " + key + " found");
        }
        return DAO.jsonMapper.readValue(Files.toString(schema, Charsets.UTF_8), DAO.jsonTypeRef);
    }

    public static boolean getConfig(String key, boolean default_val) {
        String value = config.get(key);
        return value == null ? default_val : value.equals("true") || value.equals("on") || value.equals("1");
    }
    
    public static Set<String> getConfigKeys() {
        return config.keySet();
    }
    
    /**
     * Store a message together with a user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param t a tweet
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public static boolean writeMessage(MessageEntry t, UserEntry u, boolean checkExist, boolean dump, boolean overwriteUser, boolean bulk) {
        if (t == null) {
            return false;
        }
        try {
            // check if tweet exists in index
            if (checkExist && messages.exists(t.getIdStr())) return false; // we omit writing this again

            synchronized (DAO.class) {
                // check if user exists in index
                if (overwriteUser) {
                    UserEntry oldUser = users.read(u.getScreenName());
                    if (oldUser == null || !oldUser.equals(u)) {
                        writeUser(u, t.getSourceType().name(), bulk);
                    }
                } else {
                    if (!users.exists(u.getScreenName())) {
                        writeUser(u, t.getSourceType().name(), bulk);
                    } 
                }
    
                // record tweet into search index
                messages.writeEntry(t.getIdStr(), t.getSourceType().name(), t, bulk);
            }
            
            // record tweet into text file
            if (dump) message_dump.write(t.toMap(u, false, Integer.MAX_VALUE, ""));
            
            // teach the classifier
            Classifier.learnPhrase(t.getText(Integer.MAX_VALUE, ""));
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
    public synchronized static boolean writeUser(UserEntry u, String source_type, boolean bulk) {
        try {
            // record user into search index
            users.writeEntry(u.getScreenName(), source_type, u, bulk);
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
            if (dump) account_dump.write(a.toMap(null));

            // record account into search index
            accounts.writeEntry(a.getScreenName(), a.getSourceType().name(), a, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Store an import profile into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param i an import profile
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public synchronized static boolean writeImportProfile(ImportProfileEntry i, boolean dump) {
        try {
            // record import profile into text file
            if (dump) import_profile_dump.write(i.toMap());
            // record import profile into search index
            importProfiles.writeEntry(i.getId(), i.getSourceType().name(), i, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static long countLocalMessages(long millis) {
        return countLocal(MESSAGES_INDEX_NAME, millis);
    }
    
    public static long countLocalMessages(String provider_hash) {
        return countLocal(MESSAGES_INDEX_NAME, provider_hash);
    }
    
    public static long countLocalUsers() {
        return countLocal(USERS_INDEX_NAME, -1);
    }

    public static long countLocalQueries() {
        return countLocal(QUERIES_INDEX_NAME, -1);
    }
    
    public static long countLocalAccounts() {
        return countLocal(ACCOUNTS_INDEX_NAME, -1);
    }

    private static long countLocal(String index, long millis) {
        try {
            CountResponse response = elasticsearch_client.prepareCount(index)
                .setQuery(millis <= 0 ? QueryBuilders.matchAllQuery() : QueryBuilders.rangeQuery("created_at").from(new Date(System.currentTimeMillis() - millis)))
                .execute()
                .actionGet();
            return response.getCount();
        } catch (Throwable e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    private static long countLocal(String index, String provider_hash) {
        try {
            CountResponse response = elasticsearch_client.prepareCount(index)
                .setQuery(QueryBuilders.matchQuery("provider_hash", provider_hash))
                .execute()
                .actionGet();
            return response.getCount();
        } catch (Throwable e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static MessageEntry readMessage(String id) throws IOException {
        return messages.read(id);
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

    public  static boolean deleteImportProfile(String id, SourceType sourceType) {
        return importProfiles.delete(id, sourceType);
    }
    
    public static class SearchLocalMessages {
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
            // prepare request
            QueryEntry.ElasticsearchQuery sq = new QueryEntry.ElasticsearchQuery(q, timezoneOffset);
            SearchRequestBuilder request = elasticsearch_client.prepareSearch(MESSAGES_INDEX_NAME)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setQuery(sq.queryBuilder)
                    .setFrom(0)
                    .setSize(resultCount);
            request.clearRescorers();
            if (resultCount > 0) {
                request.addSort(
                        SortBuilders.fieldSort(order_field.getMessageFieldName())
                            .unmappedType(order_field.getMessageFieldType())
                            .order(SortOrder.DESC)
                        );
            }
            boolean addTimeHistogram = false;
            long interval = sq.until.getTime() - sq.since.getTime();
            DateHistogramInterval dateHistogrammInterval = interval > DateParser.WEEK_MILLIS ? DateHistogramInterval.DAY : interval > DateParser.HOUR_MILLIS * 3 ? DateHistogramInterval.HOUR : DateHistogramInterval.MINUTE;
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
            timeline.setHits((int) response.getHits().getTotalHits());
                    
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
                        timeline.add(tweet, user);
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
                    String key = bucket.getKeyAsString().trim();
                    if (key.length() > 0) {
                        String k = key.toLowerCase();
                        Long v = checkMap.get(k);
                        checkMap.put(k, v == null ? bucket.getDocCount() : v + bucket.getDocCount());
                    }
                }
                ArrayList<Map.Entry<String, Long>> list = new ArrayList<>(buckets.size());
                for (Bucket bucket: buckets) {
                    String key = bucket.getKeyAsString().trim();
                    if (key.length() > 0) {
                        Long v = checkMap.remove(key.toLowerCase());
                        if (v == null) continue;
                        list.add(new AbstractMap.SimpleEntry<String, Long>(key, v));
                    }
                }
                aggregations.put(field, list);
                //if (field.equals("place_country")) {
                    // special handling of country aggregation: add the country center as well
                //}
            }
            // date histogram:
            if (addTimeHistogram) {
                InternalHistogram<InternalHistogram.Bucket> dateCounts = response.getAggregations().get("created_at");              
                ArrayList<Map.Entry<String, Long>> list = new ArrayList<>();
                for (InternalHistogram.Bucket bucket : dateCounts.getBuckets()) {
                    Calendar cal = Calendar.getInstance(DateParser.UTCtimeZone);
                    org.joda.time.DateTime k = (org.joda.time.DateTime) bucket.getKey();
                    cal.setTime(k.toDate());
                    cal.add(Calendar.MINUTE, -timezoneOffset);
                    long docCount = bucket.getDocCount();
                    Map.Entry<String,Long> entry = new AbstractMap.SimpleEntry<String, Long>(
                        (dateHistogrammInterval == DateHistogramInterval.DAY ?
                            DateParser.dayDateFormat : DateParser.minuteDateFormat)
                        .format(cal.getTime()), docCount);
                    list.add(entry);
                }
                aggregations.put("created_at", list);
                
            }
        }
    }

    /**
     * Search the local user cache using a elasticsearch query.
     * @param screen_name - the user id
     */
    public static UserEntry searchLocalUserByScreenName(final String screen_name) {
        try {
            return users.read(screen_name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static UserEntry searchLocalUserByUserId(final String user_id) {
        if (user_id == null || user_id.length() == 0) return null;
        // prepare request
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(QueryBuilders.termQuery(UserFactory.field_user_id, user_id));

        SearchRequestBuilder request = elasticsearch_client.prepareSearch(USERS_INDEX_NAME)
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
        Map<String, Object> map = hits[0].getSource();
        return new UserEntry(map);
    }
    
    /**
     * Search the local account cache using an elasticsearch query.
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
    public static ResultList<QueryEntry> SearchLocalQueries(final String q, final int resultCount, final String sort_field, final String default_sort_type, final SortOrder sort_order, final Date since, final Date until, final String range_field) {
        ResultList<QueryEntry> queries = new ResultList<>();
        
        // prepare request
        BoolQueryBuilder suggest = QueryBuilders.boolQuery();
        if (q != null && q.length() > 0) {
            suggest.should(QueryBuilders.fuzzyQuery("query", q).fuzziness(Fuzziness.fromEdits(2)));
            suggest.should(QueryBuilders.moreLikeThisQuery("query").like(q));
            suggest.should(QueryBuilders.matchPhrasePrefixQuery("query", q));
            if (q.indexOf('*') >= 0 || q.indexOf('?') >= 0) suggest.should(QueryBuilders.wildcardQuery("query", q));
            suggest.minimumNumberShouldMatch(1);
        }

        BoolQueryBuilder query;
        
        if (range_field != null && range_field.length() > 0 && (since != null || until != null)) {
            query = QueryBuilders.boolQuery();
            if (q.length() > 0) query.must(suggest);
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(range_field);
            if (since != null) rangeQuery.from(since).includeLower(true);
            if (until != null) rangeQuery.to(until).includeUpper(true);
            query.must(rangeQuery);
        } else {
            query = suggest;
        }
        
        SearchRequestBuilder request = elasticsearch_client.prepareSearch(QUERIES_INDEX_NAME)
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setQuery(query)
                .setFrom(0)
                .setSize(resultCount)
                .addSort(
                        SortBuilders.fieldSort(sort_field)
                        .unmappedType(default_sort_type)
                        .order(sort_order));

        // get response
        SearchResponse response = request.execute().actionGet();

        // evaluate search result
        //long totalHitCount = response.getHits().getTotalHits();
        SearchHits rhits = response.getHits();
        long totalHits = rhits.getTotalHits();
        queries.setHits(totalHits);
        SearchHit[] hits = rhits.getHits();
        for (SearchHit hit: hits) {
            Map<String, Object> map = hit.getSource();
            queries.add(new QueryEntry(map));
        }
            
        return queries;
    }

    public static ImportProfileEntry SearchLocalImportProfiles(final String id) {
        try {
            return importProfiles.read(id);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Collection<ImportProfileEntry> SearchLocalImportProfilesWithConstraints(final Map<String, String> constraints, boolean latest) throws IOException {
        List<ImportProfileEntry> rawResults = new ArrayList<>();
        SearchRequestBuilder request = elasticsearch_client.prepareSearch(IMPORT_PROFILE_INDEX_NAME)
                .setSearchType(SearchType.QUERY_THEN_FETCH)
                .setFrom(0);

        BoolQueryBuilder bFilter = QueryBuilders.boolQuery();
        bFilter.must(QueryBuilders.termQuery("active_status", EntryStatus.ACTIVE.name().toLowerCase()));
        for (Object o : constraints.entrySet()) {
            @SuppressWarnings("rawtypes")
            Map.Entry entry = (Map.Entry) o;
            bFilter.must(QueryBuilders.termQuery((String) entry.getKey(), ((String) entry.getValue()).toLowerCase()));
        }
        request.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), bFilter));
        DAO.log(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), bFilter).toString());
        // get response
        SearchResponse response = request.execute().actionGet();

        // evaluate search result
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit: hits) {
            Map<String, Object> map = hit.getSource();
            rawResults.add(new ImportProfileEntry(map));
        }

        if (!latest) {
            return rawResults;
        }

        // filter results to display only latest profiles
        Map<String, ImportProfileEntry> latests = new HashMap<>();
        for (ImportProfileEntry entry : rawResults) {
            String uniqueKey;
            if (entry.getImporter() != null) {
                uniqueKey = entry.getSourceUrl() + entry.getImporter();
            } else {
                uniqueKey = entry.getSourceUrl() + entry.getClientHost();
            }
            if (latests.containsKey(uniqueKey)) {
                if (entry.getLastModified().compareTo(latests.get(uniqueKey).getLastModified()) > 0) {
                    latests.put(uniqueKey, entry);
                }
            } else {
                latests.put(uniqueKey, entry);
            }
        }
        return latests.values();
    }
    
    public static Timeline scrapeTwitter(final RemoteAccess.Post post, final String q, final Timeline.Order order, final int timezoneOffset, boolean byUserQuery, long timeout, boolean recordQuery) {
        // retrieve messages from remote server
        ArrayList<String> remote = DAO.getFrontPeers();
        Timeline tl;
        if (remote.size() > 0 && (peerLatency.get(remote.get(0)) == null || peerLatency.get(remote.get(0)).longValue() < 3000)) {
            long start = System.currentTimeMillis();
            tl = searchOnOtherPeers(remote, q, order, 100, timezoneOffset, "all", SearchClient.frontpeer_hash, timeout); // all must be selected here to catch up missing tweets between intervals
            // at this point the remote list can be empty as a side-effect of the remote search attempt
            if (post != null && remote.size() > 0 && tl != null) post.recordEvent("remote_scraper_on_" + remote.get(0), System.currentTimeMillis() - start);
            if (tl == null || tl.size() == 0) {
                // maybe the remote server died, we try then ourself
                start = System.currentTimeMillis();
                tl = TwitterScraper.search(q, order, true, true, 400);
                if (post != null) post.recordEvent("local_scraper_after_unsuccessful_remote", System.currentTimeMillis() - start);
            }
        } else {
            if (post != null && remote.size() > 0) post.recordEvent("omitted_scraper_latency_" + remote.get(0), peerLatency.get(remote.get(0)));
            long start = System.currentTimeMillis();
            tl = TwitterScraper.search(q, order, true, true, 400);
            if (post != null) post.recordEvent("local_scraper", System.currentTimeMillis() - start);
        }

        // record the query
        long start2 = System.currentTimeMillis();
        QueryEntry qe = null;
        try {
            qe = queries.read(q);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        if (qe != null || (recordQuery && Caretaker.acceptQuery4Retrieval(q))) {
            if (qe == null) {
                // a new query occurred
                qe = new QueryEntry(q, timezoneOffset, tl.period(), SourceType.TWITTER, byUserQuery);
            } else {
                // existing queries are updated
                qe.update(tl.period(), byUserQuery);
            }
            try {
                queries.writeEntry(q, qe.source_type == null ? SourceType.TWITTER.name() : qe.source_type.name(), qe, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // accept rules may change, we want to delete the query then in the index
            if (qe != null) queries.delete(q, qe.source_type);
        }
        if (post != null) post.recordEvent("query_recorder", System.currentTimeMillis() - start2);
        //log("SCRAPER: TIME LEFT after recording = " + (termination - System.currentTimeMillis()));
        
        return tl;
    }
    
    public static final Random random = new Random(System.currentTimeMillis());
    private static final Map<String, Long> peerLatency = new HashMap<>();
    private static ArrayList<String> getBestPeers(Collection<String> peers) {
        ArrayList<String> best = new ArrayList<>();
        if (peers == null || peers.size() == 0) return best;
        // first check if any of the given peers has unknown latency
        TreeMap<Long, String> o = new TreeMap<>();
        for (String peer: peers) {
            if (peerLatency.containsKey(peer)) {
                o.put(peerLatency.get(peer) * 1000 + best.size(), peer);
            } else {
                best.add(peer);
            }
        }
        best.addAll(o.values());
        return best;
    }
    public static void healLatency(float factor) {
        for (Map.Entry<String, Long> entry: peerLatency.entrySet()) {
            entry.setValue((long) (factor * entry.getValue()));
        }
    }

    private static Set<String> frontPeerCache = new HashSet<String>();
    private static Set<String> backendPeerCache = new HashSet<String>();
    
    public static void updateFrontPeerCache(RemoteAccess remoteAccess) {
        if (remoteAccess.getLocalHTTPPort() >= 80) {
            frontPeerCache.add("http://" + remoteAccess.getRemoteHost() + (remoteAccess.getLocalHTTPPort() == 80 ? "" : ":" + remoteAccess.getLocalHTTPPort()));
        } else if (remoteAccess.getLocalHTTPSPort() >= 443) {
            frontPeerCache.add("https://" + remoteAccess.getRemoteHost() + (remoteAccess.getLocalHTTPSPort() == 443 ? "" : ":" + remoteAccess.getLocalHTTPSPort()));
        }
    }
    
    /**
     * from all known front peers, generate a list of available peers, ordered by the peer latency
     * @return a list of front peers. only the first one shall be used, but the other are fail-over peers
     */
    public static ArrayList<String> getFrontPeers() {
        ArrayList<String> testpeers = new ArrayList<>();
        if (frontPeerCache.size() == 0) {
            String[] remote = DAO.getConfig("frontpeers", new String[0], ",");
            for (String peer: remote) frontPeerCache.add(peer);
            // add dynamically all peers that contacted myself
            for (Map<String, RemoteAccess> hmap: RemoteAccess.history.values()) {
                for (Map.Entry<String, RemoteAccess> peer: hmap.entrySet()) {
                    updateFrontPeerCache(peer.getValue());
                }
            }
        }
        testpeers.addAll(frontPeerCache);
        return getBestPeers(testpeers);
    }
    
    public static List<String> getBackendPeers() {
        List<String> testpeers = new ArrayList<>();
        if (backendPeerCache.size() == 0) {
            String[] remote = DAO.getConfig("backend", new String[0], ",");
            for (String peer: remote) backendPeerCache.add(peer);
        }
        testpeers.addAll(backendPeerCache);
        return getBestPeers(testpeers);
    }
    
    public static Timeline searchBackend(final String q, final Timeline.Order order, final int count, final int timezoneOffset, final String where, final long timeout) {
        List<String> remote = getBackendPeers();
        
        if (remote.size() > 0 /*&& (peerLatency.get(remote.get(0)) == null || peerLatency.get(remote.get(0)) < 3000)*/) { // condition deactivated because we need always at least one peer
            Timeline tt = searchOnOtherPeers(remote, q, order, count, timezoneOffset, where, SearchClient.backend_hash, timeout);
            if (tt != null) tt.writeToIndex();
            return tt;
        }
        return null;
    }

    private final static Random randomPicker = new Random(System.currentTimeMillis());
    
    public static Timeline searchOnOtherPeers(final List<String> remote, final String q, final Timeline.Order order, final int count, final int timezoneOffset, final String source, final String provider_hash, final long timeout) {
        // select remote peer
        while (remote.size() > 0) {
            int pick = randomPicker.nextInt(remote.size());
            String peer = remote.get(pick);
            long start = System.currentTimeMillis();
            try {
                Timeline tl = SearchClient.search(peer, q, order, source, count, timezoneOffset, provider_hash, timeout);
                peerLatency.put(peer, System.currentTimeMillis() - start);
                // to show which peer was used for the retrieval, we move the picked peer to the front of the list
                if (pick != 0) remote.add(0, remote.remove(pick));
                tl.setScraperInfo(tl.getScraperInfo().length() > 0 ? peer + "," + tl.getScraperInfo() : peer);
                return tl;
            } catch (IOException e) {
                DAO.log("searchOnOtherPeers: no IO to scraping target: " + e.getMessage());
                // the remote peer seems to be unresponsive, remove it (temporary) from the remote peer list
                peerLatency.put(peer, System.currentTimeMillis() - start);
                frontPeerCache.remove(peer);
                backendPeerCache.remove(peer);
                remote.remove(pick);
            }
        }
        return null;
    }
    
    public final static Set<Number> newUserIds = new ConcurrentHashSet<>();
    
    public static void announceNewUserId(Timeline tl) {
        for (MessageEntry message: tl) {
            UserEntry user = tl.getUser(message);
            assert user != null;
            if (user == null) continue;
            Number id = user.getUser();
            if (id != null) announceNewUserId(id);
        }
    }

    public static void announceNewUserId(Number id) {
        JsonFactory mapcapsule = DAO.user_dump.get("id_str", id.toString());
        Map<String, Object> map = null;
        try {map = mapcapsule == null ? null : mapcapsule.getJson();} catch (IOException e) {}
        if (map == null) newUserIds.add(id);
    }
    
    public static Set<Number> getNewUserIdsChunk() {
        if (newUserIds.size() < 100) return null;
        Set<Number> chunk = new HashSet<>();
        Iterator<Number> i = newUserIds.iterator();
        for (int j = 0; j < 100; j++) {
            chunk.add(i.next());
            i.remove();
        }
        return chunk;
    }
    
    public static void log(String line) {
        Log.getLog().info(line);
    }

}
