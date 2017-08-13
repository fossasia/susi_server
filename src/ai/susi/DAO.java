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

package ai.susi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.io.Files;

import ai.susi.json.JsonFile;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiMind;

import ai.susi.server.APIException;
import ai.susi.server.AccessTracker;
import ai.susi.server.Accounting;
import ai.susi.server.Authentication;
import ai.susi.server.Authorization;
import ai.susi.server.ClientCredential;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Settings;

import ai.susi.tools.IO;
import ai.susi.tools.OS;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;

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
    public  static File conf_dir, bin_dir, html_dir, data_dir, susi_memory_dir, model_watch_dir, susi_skill_repo;
    private static File external_data, assets, dictionaries;
    private static Settings public_settings, private_settings;
    public  static AccessTracker access;
    private static Map<String, String> config = new HashMap<>();
    
    // AAA Schema for server usage
    private static JsonTray authentication;
    private static JsonTray authorization;
    private static JsonTray accounting;
    public  static JsonTray passwordreset;
    private static JsonFile login_keys;
    public static JsonTray group;
    public static JsonTray skillRating;


    // built-in artificial intelligence
    public static SusiMind susi;

    /**
     * initialize the DAO
     * @param configMap
     * @param dataPath the path to the data directory
     */
    public static void init(Map<String, String> configMap, Path dataPath) throws Exception{

        log("initializing SUSI DAO");
        
        config = configMap;
        conf_dir = new File("conf");
        bin_dir = new File("bin");
        html_dir = new File("html");
        data_dir = dataPath.toFile().getAbsoluteFile();
        susi_memory_dir = new File(data_dir, "susi");
        model_watch_dir = new File(new File(data_dir.getParentFile().getParentFile(), "susi_skill_data"), "models");
        susi_skill_repo = new File(data_dir.getParentFile().getParentFile(), "susi_skill_data/.git");

        // wake up susi
        File susiinitpath = new File(conf_dir, "susi");
        susi = model_watch_dir.exists() ?
                new SusiMind(susi_memory_dir, susiinitpath, model_watch_dir) :
                new SusiMind(susi_memory_dir, susiinitpath);
        String susi_boilerplate_name = "susi_cognition_boilerplate.json";
        File susi_boilerplate_file = new File(susi_memory_dir, susi_boilerplate_name);
        if (!susi_boilerplate_file.exists()) Files.copy(new File(conf_dir, "susi/" + susi_boilerplate_name + ".example"), susi_boilerplate_file);

        // initialize public and private keys
		public_settings = new Settings(new File("data/settings/public.settings.json"));
		File private_file = new File("data/settings/private.settings.json");
		private_settings = new Settings(private_file);
		OS.protectPath(private_file.toPath());
		
		if (!private_settings.loadPrivateKey() || !public_settings.loadPublicKey()) {
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
        
        // check if elasticsearch shall be accessed as external cluster
        
        // open AAA storage
        Path settings_dir = dataPath.resolve("settings");
        settings_dir.toFile().mkdirs();
        Path authentication_path_per = settings_dir.resolve("authentication.json");
        Path authentication_path_vol = settings_dir.resolve("authentication_session.json");
        authentication = new JsonTray(authentication_path_per.toFile(), authentication_path_vol.toFile(), 1000000);
        OS.protectPath(authentication_path_per);
        OS.protectPath(authentication_path_vol);
        Path authorization_path_per = settings_dir.resolve("authorization.json");
        Path authorization_path_vol = settings_dir.resolve("authorization_session.json");
        authorization = new JsonTray(authorization_path_per.toFile(), authorization_path_vol.toFile(), 1000000);
        OS.protectPath(authorization_path_per);
        OS.protectPath(authorization_path_vol);
        Path passwordreset_path_per = settings_dir.resolve("passwordreset.json");
        Path passwordreset_path_vol = settings_dir.resolve("passwordreset_session.json");
        passwordreset = new JsonTray(passwordreset_path_per.toFile(), passwordreset_path_vol.toFile(), 1000000);
        OS.protectPath(passwordreset_path_per);
        OS.protectPath(passwordreset_path_vol);
        Path accounting_path_per = settings_dir.resolve("accounting.json");
        Path accounting_path_vol = settings_dir.resolve("accounting_session.json");
        accounting = new JsonTray(accounting_path_per.toFile(), accounting_path_vol.toFile(), 1000000);
        OS.protectPath(accounting_path_per);
        OS.protectPath(accounting_path_vol);

        Path login_keys_path = settings_dir.resolve("login-keys.json");
        login_keys = new JsonFile(login_keys_path.toFile());
        OS.protectPath(login_keys_path);


        Path groups_per = settings_dir.resolve("groups.json");
        Path groups_vol = settings_dir.resolve("groups_session.json");
        group = new JsonTray(groups_per.toFile(), groups_vol.toFile(), 1000000);
        OS.protectPath(groups_per);
        OS.protectPath(groups_vol);

        /*Skill Rating storage*/
        Path susi_skill_rating_dir = dataPath.resolve("skill_rating");
        susi_skill_rating_dir.toFile().mkdirs();
        Path skillRating_per = susi_skill_rating_dir.resolve("skillRating.json");
        Path skillRating_vol = susi_skill_rating_dir.resolve("skillRating_session.json");
        skillRating = new JsonTray(skillRating_per.toFile(), skillRating_vol.toFile(), 1000000);
        OS.protectPath(skillRating_per);
        OS.protectPath(skillRating_vol);

        // open index
        Path index_dir = dataPath.resolve("index");
        if (index_dir.toFile().exists()) OS.protectPath(index_dir); // no other permissions to this path

        // create and document the data dump dir
        assets = new File(data_dir, "assets");
        external_data = new File(data_dir, "external");
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
    

	/**
	 * Registers a key for an identity.
	 * TODO: different algorithms
	 * @param id
	 * @param key
     */
	public static void registerKey(ClientIdentity id, PublicKey key) throws APIException{
		JSONObject user_obj;
		try{
			user_obj = DAO.login_keys.getJSONObject(id.toString());
		} catch (Throwable e){
			user_obj = new JSONObject();
			DAO.login_keys.put(id.toString(), user_obj);
		}
		user_obj.put(IO.getKeyHash(key), IO.getKeyAsString(key));
		DAO.login_keys.commit();
	}

	public static String loadKey(ClientIdentity identity, String keyhash) {
		String id = identity.toString();
		if (!login_keys.has(id)) return null;
		JSONObject json = login_keys.getJSONObject(id);
		if (!json.has(keyhash)) return null;
		return json.getString(keyhash);
	}
	
	public static Authentication getAuthentication(@Nonnull ClientCredential credential) {
		return new Authentication(credential, authentication);
	}
	
	public static boolean hasAuthentication(@Nonnull ClientCredential credential) {
		return authentication.has(credential.toString());
	}
	
	public static void deleteAuthentication(@Nonnull ClientCredential credential) {
		authentication.remove(credential.toString());
	}

	public static Authorization getAuthorization(@Nonnull ClientIdentity identity) {
		 return new Authorization(identity, authorization);
	}
	
	public static boolean hasAuthorization(@Nonnull ClientIdentity credential) {
		return authorization.has(credential.toString());
	}
	
	public static Collection<ClientIdentity> getAuthorizedClients() {
		ArrayList<ClientIdentity> i = new ArrayList<>();
		for (String id: authorization.keys()) {
		    if(id.contains("host"))
		        continue;
			i.add(new ClientIdentity(id));
		}
		return i;
	}

	public static void deleteAuthorization(@Nonnull ClientIdentity credential) {
	    authorization.remove(credential.toString());
    }
    
    public static Accounting getAccounting(@Nonnull ClientIdentity identity) {
         return new Accounting(identity, accounting);
    }
    
    public static boolean hasAccounting(@Nonnull ClientIdentity credential) {
        return accounting.has(credential.toString());
    }

    public static Repository getRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir((susi_skill_repo))
                        .readEnvironment() // scan environment GIT_* variables
                        .findGitDir() // scan up the file system tree
                        .build();
        return repository;
    }

    public static Git getGit() throws IOException {
        Git git = new Git(getRepository());
        return git;
    }

    public static void pull(Git git) throws IOException {
        try {
            PullResult pullResult = git.pull().call();
            MergeResult mergeResult = pullResult.getMergeResult();
            
            // TODO: check if the mergeResult contains a hint that we have to commit a merge
            // TODO: commit and push the merge
            
            // TODO: check if the pull created any conflict. In case of an conflict, we must react dramatically: send an administrator an email to call for a fix
            Status status = git.status().call();
            status.getConflicting();
            
            
        } catch (GitAPIException e) {
            throw new IOException (e.getMessage());
        }
    }

    public static void pushCommit(Git git, String commit_message) throws IOException {
     // and then commit the changes
        try {
            git.commit().setMessage(commit_message).call();
            String remote = "origin";
            String branch = "refs/heads/master";
            String trackingBranch = "refs/remotes/" + remote + "/master";
            RefSpec spec = new RefSpec(branch + ":" + branch);

            // TODO: pull & merge
            
            PushCommand push = git.push();
            push.setForce(true);
            push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getConfig("github.username", ""), getConfig("github.password","")));
            push.call();
        } catch (GitAPIException e) {
            throw new IOException (e.getMessage());
        }
    }
    
}
