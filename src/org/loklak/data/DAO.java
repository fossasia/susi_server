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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jetty.util.ConcurrentHashSet;

import com.github.fge.jackson.JsonLoader;
import com.google.common.base.Charsets;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.Caretaker;
import org.loklak.api.client.SearchClient;
import org.loklak.geo.GeoNames;
import org.loklak.harvester.SourceType;
import org.loklak.harvester.TwitterScraper;
import org.loklak.http.AccessTracker;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.AccountEntry;
import org.loklak.objects.ImportProfileEntry;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.Peers;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.ResultList;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.server.Accounting;
import org.loklak.server.Query;
import org.loklak.tools.DateParser;
import org.loklak.tools.OS;
import org.loklak.tools.storage.JsonDataset;
import org.loklak.tools.storage.JsonFile;
import org.loklak.tools.storage.JsonReader;
import org.loklak.tools.storage.JsonRepository;
import org.loklak.tools.storage.JsonStreamReader;
import org.loklak.tools.storage.JsonFactory;

import com.fasterxml.jackson.databind.JsonNode;

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
    public final static com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper(DAO.jsonFactory);
    public final static com.fasterxml.jackson.core.type.TypeReference<HashMap<String,Object>> jsonTypeRef = new com.fasterxml.jackson.core.type.TypeReference<HashMap<String,Object>>() {};
    
    public final static String MESSAGE_DUMP_FILE_PREFIX = "messages_";
    public final static String ACCOUNT_DUMP_FILE_PREFIX = "accounts_";
    public final static String USER_DUMP_FILE_PREFIX = "users_";
    public final static String ACCESS_DUMP_FILE_PREFIX = "access_";
    public final static String FOLLOWERS_DUMP_FILE_PREFIX = "followers_";
    public final static String FOLLOWING_DUMP_FILE_PREFIX = "following_";
    private static final String IMPORT_PROFILE_FILE_PREFIX = "profile_";
    
    public final static int CACHE_MAXSIZE =   10000;
    public final static int EXIST_MAXSIZE = 4000000;
    
    public  static File conf_dir, bin_dir, html_dir;
    private static File external_data, assets, dictionaries;
    public static JsonFile public_settings, private_settings;
    private static Path message_dump_dir, account_dump_dir, import_profile_dump_dir;
    public static JsonRepository message_dump;
    private static JsonRepository account_dump;
    private static JsonRepository import_profile_dump;
    public  static JsonDataset user_dump, followers_dump, following_dump;
    public  static AccessTracker access;
    private static File schema_dir, conv_schema_dir;
    private static ElasticsearchClient elasticsearch_client;
    //private static Node elasticsearch_node;
    //private static Client elasticsearch_client;
    public static UserFactory users;
    private static AccountFactory accounts;
    public static MessageFactory messages;
    public static QueryFactory queries;
    private static ImportProfileFactory importProfiles;
    private static Map<String, String> config = new HashMap<>();
    public  static GeoNames geoNames;
    public static Peers peers = new Peers();
    
    // AAA Schema for server usage
    public static JsonFile authentication;
    public static JsonFile authorization;
    public static JsonFile accounting_persistent;
    public static Map<String, Accounting> accounting_temporary = new HashMap<>();
    
    public static enum IndexName {
    	queries, messages, users, accounts, import_profiles;
    }
    
    /**
     * initialize the DAO
     * @param datadir the path to the data directory
     */
    public static void init(Map<String, String> configMap, Path dataPath) throws Exception{

        log("initializing loklak DAO");
        
        config = configMap;
        conf_dir = new File("conf");
        bin_dir = new File("bin");
        html_dir = new File("html");
        
        // initialize public and private keys
		public_settings = new JsonFile(new File("data/settings/public.settings.json"));
		File private_file = new File("data/settings/private.settings.json");
		private_settings = new JsonFile(private_file);
		OS.protectPath(private_file.toPath());
		
		if(!private_settings.loadPrivateKey() || !public_settings.loadPublicKey()){
        	log("Can't load key pair. Creating new one");
        	
        	// create new key pair
        	KeyPairGenerator keyGen;
			try {
				String algorithm = "RSA";
				keyGen = KeyPairGenerator.getInstance(algorithm);
				keyGen.initialize(2048);
				KeyPair keyPair = keyGen.genKeyPair();
				private_settings.setPrivateKey(keyPair.getPrivate(), algorithm);
				public_settings.setPublicKey(keyPair.getPublic(), algorithm);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			log("Key creation finished. Peer hash: " + public_settings.getPeerHashAlgorithm() + " " + public_settings.getPeerHash());
        }
        else{
        	log("Key pair loaded from file. Peer hash: " + public_settings.getPeerHashAlgorithm() + " " + public_settings.getPeerHash());
        }
        
        File datadir = dataPath.toFile();
        // check if elasticsearch shall be accessed as external cluster
        String transport = configMap.get("elasticsearch_transport.enabled");
        if (transport != null && "true".equals(transport)) {
            String cluster_name = configMap.get("elasticsearch_transport.cluster.name");
            String transport_addresses_string = configMap.get("elasticsearch_transport.addresses");
            if (transport_addresses_string != null && transport_addresses_string.length() > 0) {
                String[] transport_addresses = transport_addresses_string.split(",");
                elasticsearch_client = new ElasticsearchClient(transport_addresses, cluster_name);
            }
        } else {
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
            
            // start elasticsearch
            elasticsearch_client = new ElasticsearchClient(settings);
        }
        
        // open AAA storage
        Path settings_dir = dataPath.resolve("settings");
        settings_dir.toFile().mkdirs();
        Path authentication_path = settings_dir.resolve("authentication.json");
        authentication = new JsonFile(authentication_path.toFile());
        OS.protectPath(authentication_path);
        Path authorization_path = settings_dir.resolve("authorization.json");
        authorization = new JsonFile(authorization_path.toFile());
        OS.protectPath(authorization_path);
        
        // open index
        Path index_dir = dataPath.resolve("index");
        if (index_dir.toFile().exists()) OS.protectPath(index_dir); // no other permissions to this path

        // define the index factories
        messages = new MessageFactory(elasticsearch_client, IndexName.messages.name(), CACHE_MAXSIZE, EXIST_MAXSIZE);
        users = new UserFactory(elasticsearch_client, IndexName.users.name(), CACHE_MAXSIZE, EXIST_MAXSIZE);
        accounts = new AccountFactory(elasticsearch_client, IndexName.accounts.name(), CACHE_MAXSIZE, EXIST_MAXSIZE);
        queries = new QueryFactory(elasticsearch_client, IndexName.queries.name(), CACHE_MAXSIZE, EXIST_MAXSIZE);
        importProfiles = new ImportProfileFactory(elasticsearch_client, IndexName.import_profiles.name(), CACHE_MAXSIZE, EXIST_MAXSIZE);

        // create indices and set mapping (that shows how 'elastic' elasticsearch is: it's always good to define data types)
        File mappingsDir = new File(new File(conf_dir, "elasticsearch"), "mappings");
        int shards = Integer.parseInt(configMap.get("elasticsearch.index.number_of_shards"));
        int replicas = Integer.parseInt(configMap.get("elasticsearch.index.number_of_replicas"));
        for (IndexName index: IndexName.values()) {
            log("initializing index '" + index.name() + "'...");
        	try {
        	    elasticsearch_client.createIndexIfNotExists(index.name(), shards, replicas);
        	} catch (Throwable e) {
        		e.printStackTrace();
        	}
            try {
                elasticsearch_client.setMapping(index.name(), new File(mappingsDir, index.name() + ".json"));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        // elasticsearch will probably take some time until it is started up. We do some other stuff meanwhile..

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
        message_dump = new JsonRepository(message_dump_dir.toFile(), MESSAGE_DUMP_FILE_PREFIX, message_dump_readme, JsonRepository.COMPRESSED_MODE, true, Runtime.getRuntime().availableProcessors());
        
        account_dump_dir = dataPath.resolve("accounts");
        account_dump_dir.toFile().mkdirs();
        OS.protectPath(account_dump_dir); // no other permissions to this path
        account_dump = new JsonRepository(account_dump_dir.toFile(), ACCOUNT_DUMP_FILE_PREFIX, null, JsonRepository.REWRITABLE_MODE, false, Runtime.getRuntime().availableProcessors());

        File user_dump_dir = new File(datadir, "accounts");
        user_dump_dir.mkdirs();
        user_dump = new JsonDataset(
                user_dump_dir,USER_DUMP_FILE_PREFIX,
                new JsonDataset.Column[]{new JsonDataset.Column("id_str", false), new JsonDataset.Column("screen_name", true)},
                "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                JsonRepository.REWRITABLE_MODE, false, Integer.MAX_VALUE);
        followers_dump = new JsonDataset(
                user_dump_dir, FOLLOWERS_DUMP_FILE_PREFIX,
                new JsonDataset.Column[]{new JsonDataset.Column("screen_name", true)},
                "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                JsonRepository.REWRITABLE_MODE, false, Integer.MAX_VALUE);
        following_dump = new JsonDataset(
                user_dump_dir, FOLLOWING_DUMP_FILE_PREFIX,
                new JsonDataset.Column[]{new JsonDataset.Column("screen_name", true)},
                "retrieval_date", DateParser.PATTERN_ISO8601MILLIS,
                JsonRepository.REWRITABLE_MODE, false, Integer.MAX_VALUE);
        
        Path log_dump_dir = dataPath.resolve("log");
        log_dump_dir.toFile().mkdirs();
        OS.protectPath(log_dump_dir); // no other permissions to this path
        access = new AccessTracker(log_dump_dir.toFile(), ACCESS_DUMP_FILE_PREFIX, 60000, 3000);
        access.start(); // start monitor

        import_profile_dump_dir = dataPath.resolve("import-profiles");
        import_profile_dump = new JsonRepository(import_profile_dump_dir.toFile(), IMPORT_PROFILE_FILE_PREFIX, null, JsonRepository.COMPRESSED_MODE, false, Runtime.getRuntime().availableProcessors());

        // load schema folder
        conv_schema_dir = new File("conf/conversion");
        schema_dir = new File("conf/schema");            

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
        
        // finally wait for healthy status of elasticsearch shards
        ClusterHealthStatus required_status = ClusterHealthStatus.fromString(config.get("elasticsearch_requiredClusterHealthStatus"));
        boolean ok;
        do {
            log("Waiting for elasticsearch " + required_status.name() + " status");
            ok = elasticsearch_client.wait_ready(60000l, required_status);
        } while (!ok);
        /**
        do {
            log("Waiting for elasticsearch green status");
            health = elasticsearch_client.admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();
        } while (health.isTimedOut());
        **/
        log("elasticsearch has started up!");

        // start the classifier
        new Thread(){
            public void run() {
                log("initializing the classifier...");
                try {
                    Classifier.init(10000, 1000);
                } catch (Throwable ee) {
                    ee.printStackTrace();
                }
                log("classifier initialized!");
            }
        }.start();

        log("initializing queries...");
        File harvestingPath = new File(datadir, "queries");
        if (!harvestingPath.exists()) harvestingPath.mkdirs();
        String[] list = harvestingPath.list();
        for (String queryfile: list) {
            if (queryfile.startsWith(".") || queryfile.endsWith("~")) continue;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(harvestingPath, queryfile))));
                String line;
                List<IndexEntry<QueryEntry>> bulkEntries = new ArrayList<>();
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
                        bulkEntries.add(
                            new IndexEntry<QueryEntry>(
                                line,
                                SourceType.TWITTER.name(),
                                new QueryEntry(line, 0, 60000, SourceType.TWITTER, false))
                        );
                    }
                    if (bulkEntries.size() > 1000) {
                        queries.writeEntries(bulkEntries);
                        bulkEntries.clear();
                    }
                }
                queries.writeEntries(bulkEntries);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log("queries initialized.");
        
        log("finished DAO initialization");
    }
    
    public static boolean wait_ready(long maxtimemillis) {
        ClusterHealthStatus required_status = ClusterHealthStatus.fromString(config.get("elasticsearch_requiredClusterHealthStatus"));
        return elasticsearch_client.wait_ready(maxtimemillis, required_status);
    }
    
    public static String pendingClusterTasks() {
        return elasticsearch_client.pendingClusterTasks();
    }
    
    public static String clusterStats() {
        return elasticsearch_client.clusterStats();
    }

    public static Map<String, String> nodeSettings() {
        return elasticsearch_client.nodeSettings();
    }
    
    public static File getAssetFile(String screen_name, String id_str, String file) {
        String letter0 = ("" + screen_name.charAt(0)).toLowerCase();
        String letter1 = ("" + screen_name.charAt(1)).toLowerCase();
        File storage_path = new File(new File(new File(assets, letter0), letter1), screen_name);
        return new File(storage_path, id_str + "_" + file); // all assets for one user in one file
    }
    
    public static Collection<File> getTweetOwnDumps(int count) {
        return message_dump.getOwnDumps(count);
    }

    public static void importAccountDumps(int count) throws IOException {
        Collection<File> dumps = account_dump.getImportDumps(count);
        if (dumps == null || dumps.size() == 0) return;
        for (File dump: dumps) {
            JsonReader reader = account_dump.getDumpReader(dump);
            final JsonReader dumpReader = reader;
            Thread[] indexerThreads = new Thread[dumpReader.getConcurrency()];
            for (int i = 0; i < dumpReader.getConcurrency(); i++) {
                indexerThreads[i] = new Thread() {
                    public void run() {
                        JsonFactory accountEntry;
                        try {
                            while ((accountEntry = dumpReader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                                try {
                                    JSONObject json = accountEntry.getJSON();
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
            account_dump.shiftProcessedDump(dump.getName());
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

    public static JSONObject getConversionSchema(String key) throws IOException {
        File schema = new File(conv_schema_dir, key);
        if (!schema.exists()) {
            throw new FileNotFoundException("No schema file with name " + key + " found");
        }
        return new JSONObject(com.google.common.io.Files.toString(schema, Charsets.UTF_8));
    }

    public static boolean getConfig(String key, boolean default_val) {
        String value = config.get(key);
        return value == null ? default_val : value.equals("true") || value.equals("on") || value.equals("1");
    }
    
    public static Set<String> getConfigKeys() {
        return config.keySet();
    }

    public static class MessageWrapper {
        public MessageEntry t;
        public UserEntry u;
        public boolean dump;
        public MessageWrapper(MessageEntry t, UserEntry u, boolean dump) {
            this.t = t;
            this.u = u;
            this.dump = dump;
        }
    }
    
    /**
     * Store a message together with a user into the search index
     * @param mw a message wrapper
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public static boolean writeMessage(MessageWrapper mw) {
        if (mw.t == null) return false;
        try {
            synchronized (DAO.class) {
                // record tweet into search index and check if this is a new entry
                boolean exists = messages.writeEntry(new IndexEntry<MessageEntry>(mw.t.getIdStr(), mw.t.getSourceType().name(), mw.t));

                // check if tweet exists in index
                if (exists) return false; // we don't need to write the user and also not to the message dump

                // write the user into the index
                users.writeEntry(new IndexEntry<UserEntry>(mw.u.getScreenName(), mw.t.getSourceType().name(), mw.u));

                // record tweet into text file
                if (mw.dump) message_dump.write(mw.t.toJSON(mw.u, false, Integer.MAX_VALUE, ""));
             }
            
            // teach the classifier
            Classifier.learnPhrase(mw.t.getText(Integer.MAX_VALUE, ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Set<String> writeMessageBulk(Collection<MessageWrapper> mws) {
        List<MessageWrapper> noDump = new ArrayList<>();
        List<MessageWrapper> dump = new ArrayList<>();
        for (MessageWrapper mw: mws) {
            if (mw.t == null) continue;
            if (mw.dump) dump.add(mw); else noDump.add(mw);
        }
        writeMessageBulkNoDump(noDump);
        return writeMessageBulkDump(dump);
    }

    /**
     * write messages without writing them to the dump file
     * @param mws a collection of message wrappers
     * @return a set of message IDs which had been created with this bulk write.
     */
    private static Set<String> writeMessageBulkNoDump(Collection<MessageWrapper> mws) {
        if (mws.size() == 0) return new HashSet<>();
        List<IndexEntry<UserEntry>> userBulk = new ArrayList<>();
        List<IndexEntry<MessageEntry>> messageBulk = new ArrayList<>();
        for (MessageWrapper mw: mws) {
            if (messages.existsCache(mw.t.getIdStr())) continue; // we omit writing this again
            synchronized (DAO.class) {
                // write the user into the index
                userBulk.add(new IndexEntry<UserEntry>(mw.u.getScreenName(), mw.t.getSourceType().name(), mw.u));
    
                // record tweet into search index
                messageBulk.add(new IndexEntry<MessageEntry>(mw.t.getIdStr(), mw.t.getSourceType().name(), mw.t));
             }
                
            // teach the classifier
            Classifier.learnPhrase(mw.t.getText(Integer.MAX_VALUE, ""));
        }
        ElasticsearchClient.BulkWriteResult result = null;
        try {
            result = messages.writeEntries(messageBulk);
            users.writeEntries(userBulk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result == null) return new HashSet<String>();
        return result.getCreated();
    }
    
    private static Set<String> writeMessageBulkDump(Collection<MessageWrapper> mws) {
        Set<String> created = writeMessageBulkNoDump(mws);

        for (MessageWrapper mw: mws) try {
            if (!created.contains(mw.t.getIdStr())) continue;
            synchronized (DAO.class) {
                // record tweet into text file
                message_dump.write(mw.t.toJSON(mw.u, false, Integer.MAX_VALUE, ""));
             }
                
            // teach the classifier
            Classifier.learnPhrase(mw.t.getText(Integer.MAX_VALUE, ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return created;
    }
    
    /**
     * Store an account together with a user into the search index
     * This method is synchronized to prevent concurrent IO caused by this call.
     * @param a an account 
     * @param u a user
     * @return true if the record was stored because it did not exist, false if it was not stored because the record existed already
     */
    public static boolean writeAccount(AccountEntry a, boolean dump) {
        try {
            // record account into text file
            if (dump) account_dump.write(a.toJSON(null));

            // record account into search index
            accounts.writeEntry(new IndexEntry<AccountEntry>(a.getScreenName(), a.getSourceType().name(), a));
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
    public static boolean writeImportProfile(ImportProfileEntry i, boolean dump) {
        try {
            // record import profile into text file
            if (dump) import_profile_dump.write(i.toJSON());
            // record import profile into search index
            importProfiles.writeEntry(new IndexEntry<ImportProfileEntry>(i.getId(), i.getSourceType().name(), i));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static long countLocalMessages(String provider_hash) {
        return elasticsearch_client.countLocal(IndexName.messages.name(), provider_hash);
    }

    public static long countLocalMessages(long millis) {
        return elasticsearch_client.count(IndexName.messages.name(), "timestamp", millis);
    }
    
    public static long countLocalUsers() {
        return elasticsearch_client.count(IndexName.users.name(), "timestamp", -1);
    }

    public static long countLocalQueries() {
        return elasticsearch_client.count(IndexName.queries.name(), "timestamp", -1);
    }
    
    public static long countLocalAccounts() {
        return elasticsearch_client.count(IndexName.accounts.name(), "timestamp", -1);
    }

    public static MessageEntry readMessage(String id) throws IOException {
        return messages.read(id);
    }
    
    public static boolean existMessage(String id) {
        return messages != null && messages.exists(id);
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
        public ElasticsearchClient.Query query;
            
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
            QueryEntry.ElasticsearchQuery sq = new QueryEntry.ElasticsearchQuery(q, timezoneOffset);
            long interval = sq.until.getTime() - sq.since.getTime();
            this.query =  elasticsearch_client.query(IndexName.messages.name(), sq.queryBuilder, order_field.getMessageFieldName(), timezoneOffset, resultCount, interval, "created_at", aggregationLimit, aggregationFields);
            timeline.setHits(query.hitCount);
                    
            // evaluate search result
            for (Map<String, Object> map: query.result) {
                MessageEntry tweet = new MessageEntry(new JSONObject(map));
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
            this.aggregations = query.aggregations;
        }
    }

    public static LinkedHashMap<String, Long> FullDateHistogram(int timezoneOffset) {
        return elasticsearch_client.fullDateHistogram(IndexName.messages.name(), timezoneOffset, "created_at");
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
        Map<String, Object> map = elasticsearch_client.query(IndexName.users.name(), UserEntry.field_user_id, user_id);
        if (map == null) return null;
        return new UserEntry(new JSONObject(map));
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
        ResultList<Map<String, Object>> result = elasticsearch_client.fuzzyquery(IndexName.queries.name(), "query", q, resultCount, sort_field, default_sort_type, sort_order, since, until, range_field);
        queries.setHits(result.getHits());
        for (Map<String, Object> map: result) {
            queries.add(new QueryEntry(new JSONObject(map)));
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
        List<Map<String, Object>> result = elasticsearch_client.queryWithConstraints(IndexName.import_profiles.name(), "active_status", ImportProfileEntry.EntryStatus.ACTIVE.name().toLowerCase(), constraints, latest);
        for (Map<String, Object> map: result) {
            rawResults.add(new ImportProfileEntry(new JSONObject(map)));
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
    
    public static Timeline scrapeTwitter(final Query post, final String q, final Timeline.Order order, final int timezoneOffset, boolean byUserQuery, long timeout, boolean recordQuery) {
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
        } catch (IOException | JSONException e1) {
            e1.printStackTrace();
        }
        
        if (recordQuery && Caretaker.acceptQuery4Retrieval(q)) {
            if (qe == null) {
                // a new query occurred
                qe = new QueryEntry(q, timezoneOffset, tl.period(), SourceType.TWITTER, byUserQuery);
            } else {
                // existing queries are updated
                qe.update(tl.period(), byUserQuery);
            }
            try {
                queries.writeEntry(new IndexEntry<QueryEntry>(q, qe.source_type == null ? SourceType.TWITTER.name() : qe.source_type.name(), qe));
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
        String[] remote = DAO.getConfig("frontpeers", new String[0], ",");
        ArrayList<String> testpeers = new ArrayList<>();
        if (remote.length > 0) {
            for (String peer: remote) testpeers.add(peer);
            return testpeers;
        }
        if (frontPeerCache.size() == 0) {
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
                peerLatency.put(peer, 3600000L);
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
        JSONObject map = null;
        try {map = mapcapsule == null ? null : mapcapsule.getJSON();} catch (IOException e) {}
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
