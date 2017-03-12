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

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.io.Files;

import org.eclipse.jetty.util.log.Log;
import org.loklak.http.AccessTracker;
import org.loklak.server.Accounting;
import org.loklak.server.Settings;
import org.loklak.server.UserRoles;
import org.loklak.susi.SusiMind;
import org.loklak.tools.OS;
import org.loklak.tools.storage.JsonFile;
import org.loklak.tools.storage.JsonTray;

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

    private final static String ACCESS_DUMP_FILE_PREFIX = "access_";
    
    public  static File conf_dir, bin_dir, html_dir, data_dir, susi_watch_dir;
    private static File external_data, assets, dictionaries;
    public static Settings public_settings, private_settings;
    public  static AccessTracker access;
    private static Map<String, String> config = new HashMap<>();
    
    // AAA Schema for server usage
    public static JsonTray authentication;
    public static JsonTray authorization;
    public static JsonTray accounting;
    public static UserRoles userRoles;
    public static JsonTray passwordreset;
    public static Map<String, Accounting> accounting_temporary = new HashMap<>();
    public static JsonFile login_keys;
    
    // built-in artificial intelligence
    public static SusiMind susi;
    
    /**
     * initialize the DAO
     * @param configMap
     * @param dataPath the path to the data directory
     */
    public static void init(Map<String, String> configMap, Path dataPath) throws Exception{

        log("initializing loklak DAO");
        
        config = configMap;
        conf_dir = new File("conf");
        bin_dir = new File("bin");
        html_dir = new File("html");
        data_dir = new File("data");
        susi_watch_dir = new File(data_dir, "susi");
        
        // wake up susi
        File susiinitpath = new File(conf_dir, "susi");
        susi = new SusiMind(susiinitpath, susi_watch_dir, susi_watch_dir);
        String susi_boilerplate_name = "susi_cognition_boilerplate.json";
        File susi_boilerplate_file = new File(susi_watch_dir, susi_boilerplate_name);
        if (!susi_boilerplate_file.exists()) Files.copy(new File(conf_dir, "susi/" + susi_boilerplate_name + ".example"), susi_boilerplate_file);
        
        // initialize public and private keys
		public_settings = new Settings(new File("data/settings/public.settings.json"));
		File private_file = new File("data/settings/private.settings.json");
		private_settings = new Settings(private_file);
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
				throw e;
			}
			log("Key creation finished. Peer hash: " + public_settings.getPeerHashAlgorithm() + " " + public_settings.getPeerHash());
        }
        else{
        	log("Key pair loaded from file. Peer hash: " + public_settings.getPeerHashAlgorithm() + " " + public_settings.getPeerHash());
        }
        
        File datadir = dataPath.toFile();
        // check if elasticsearch shall be accessed as external cluster
        
        // open AAA storage
        Path settings_dir = dataPath.resolve("settings");
        settings_dir.toFile().mkdirs();
        Path authentication_path_per = settings_dir.resolve("authentication.json");
        Path authentication_path_vol = settings_dir.resolve("authentication_session.json");
        authentication = new JsonTray(authentication_path_per.toFile(), authentication_path_vol.toFile(), 10000);
        OS.protectPath(authentication_path_per);
        OS.protectPath(authentication_path_vol);
        Path authorization_path_per = settings_dir.resolve("authorization.json");
        Path authorization_path_vol = settings_dir.resolve("authorization_session.json");
        authorization = new JsonTray(authorization_path_per.toFile(), authorization_path_vol.toFile(), 10000);
        OS.protectPath(authorization_path_per);
        OS.protectPath(authorization_path_vol);
        Path passwordreset_path_per = settings_dir.resolve("passwordreset.json");
        Path passwordreset_path_vol = settings_dir.resolve("passwordreset_session.json");
        passwordreset = new JsonTray(passwordreset_path_per.toFile(), passwordreset_path_vol.toFile(), 10000);
        OS.protectPath(passwordreset_path_per);
        OS.protectPath(passwordreset_path_vol);
        Path accounting_path_per = settings_dir.resolve("accounting.json");
        Path accounting_path_vol = settings_dir.resolve("accounting_session.json");
        accounting = new JsonTray(accounting_path_per.toFile(), accounting_path_vol.toFile(), 10000);
        OS.protectPath(accounting_path_per);
        OS.protectPath(accounting_path_vol);
        Path login_keys_path = settings_dir.resolve("login-keys.json");
        login_keys = new JsonFile(login_keys_path.toFile());
        OS.protectPath(login_keys_path);


        Log.getLog().info("Initializing user roles");

        Path userRoles_path = settings_dir.resolve("userRoles.json");
        userRoles = new UserRoles(new JsonFile(userRoles_path.toFile()));
        OS.protectPath(userRoles_path);

        try{
            userRoles.loadUserRolesFromObject();
            Log.getLog().info("Loaded user roles from file");
        }catch (IllegalArgumentException e){
            Log.getLog().info("Load default user roles");
            userRoles.loadDefaultUserRoles();
        }

        // open index
        Path index_dir = dataPath.resolve("index");
        if (index_dir.toFile().exists()) OS.protectPath(index_dir); // no other permissions to this path

        // create and document the data dump dir
        assets = new File(datadir, "assets");
        external_data = new File(datadir, "external");
        dictionaries = new File(external_data, "dictionaries");
        dictionaries.mkdirs();

        
        Path log_dump_dir = dataPath.resolve("log");
        log_dump_dir.toFile().mkdirs();
        OS.protectPath(log_dump_dir); // no other permissions to this path
        access = new AccessTracker(log_dump_dir.toFile(), ACCESS_DUMP_FILE_PREFIX, 60000, 3000);
        access.start(); // start monitor

        log("finished DAO initialization");
    }
    
    public static File getAssetFile(String screen_name, String id_str, String file) {
        String letter0 = ("" + screen_name.charAt(0)).toLowerCase();
        String letter1 = ("" + screen_name.charAt(1)).toLowerCase();
        File storage_path = new File(new File(new File(assets, letter0), letter1), screen_name);
        return new File(storage_path, id_str + "_" + file); // all assets for one user in one file
    }

    /**
     * close all objects in this class
     */
    public static void close() {
        Log.getLog().info("closing DAO");
        
        // close the tracker
        access.close();
        
        // close AAA for session hand-over
        authentication.close();
        authorization.close();
        passwordreset.close();
        accounting.close();

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

    public static boolean getConfig(String key, boolean default_val) {
        String value = config.get(key);
        return value == null ? default_val : value.equals("true") || value.equals("on") || value.equals("1");
    }
    
    public static Set<String> getConfigKeys() {
        return config.keySet();
    }
    
    public static final Random random = new Random(System.currentTimeMillis());

    public static void log(String line) {
        Log.getLog().info(line);
    }

    public static void severe(String line) {
        Log.getLog().warn(line);
    }

    public static void severe(String line, Throwable e) {
        Log.getLog().warn(line, e);
    }
    
    public static void severe(Throwable e) {
        Log.getLog().warn(e);
    }

}
