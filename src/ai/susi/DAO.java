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
import java.nio.file.Paths;
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
    public  static File conf_dir, bin_dir, html_dir, data_dir, skill_status_dir, susi_chatlog_dir, susi_skilllog_dir, model_watch_dir, susi_skill_repo, private_skill_watch_dir, susi_private_skill_repo, usi_skill_repo, deleted_skill_dir;
    public static String conflictsPlaceholder = "%CONFLICTS%";
    private static File external_data, assets, dictionaries;
    private static Settings public_settings, private_settings;
    public  static AccessTracker access;
    private static Map<String, String> config = new HashMap<>();
    public static Boolean pullStatus=true;
    private static Logger logger;
    private static LogAppender logAppender;

    // AAA Schema for server usage
    private static JsonTray authentication;
    private static JsonTray authorization;
    private static JsonTray accounting;
    public  static JsonTray passwordreset;
    private static JsonFile login_keys;
    public static JsonTray group;

    // CMS Schema for server usage
    public static JsonTray skillRating;
    public static JsonTray fiveStarSkillRating;
    public static JsonTray countryWiseSkillUsage;
    public static JsonTray skillUsage;
    public static JsonTray feedbackSkill;
    public static JsonTray feedbackLogs;
    public static JsonTray profileDetails;
    public static JsonTray deviceWiseSkillUsage;
    public static JsonTray bookmarkSkill;
    public static JsonTray chatbot;
    public static JsonTray skillStatus;
    public static JsonTray skillSupportedLanguages;
    public static JsonTray ratingsOverTime;
    public static JsonTray reportedSkills;


    static {
        PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSS} %p %c %x - %m%n");
        logger = Logger.getRootLogger();
        logger.removeAllAppenders();
        logAppender = new LogAppender(layout, 100000);
        logger.addAppender(logAppender);
        logger.addAppender(new ConsoleAppender(layout));
        logger.setLevel(Level.ALL);
    }

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
        File susi_memory_dir_new = new File(data_dir, "memory");
        File susi_memory_dir_old = new File(data_dir, "susi"); // old
        if (!susi_memory_dir_new.exists()) susi_memory_dir_new.mkdirs();
        susi_chatlog_dir = new File(susi_memory_dir_new, "chatlog");
        susi_skilllog_dir = new File(susi_memory_dir_new, "skilllog");
        if (susi_memory_dir_old.exists() && !susi_chatlog_dir.exists()) {
            susi_memory_dir_old.renameTo(susi_chatlog_dir); // migrate old location
        }
        if (!susi_chatlog_dir.exists()) susi_chatlog_dir.mkdirs();
        if (!susi_skilllog_dir.exists()) susi_skilllog_dir.mkdirs();
        // TODO:
        deleted_skill_dir = new File(new File(DAO.data_dir, "deleted_skill_dir"), "models");

        if(!deleted_skill_dir.exists()){
            DAO.deleted_skill_dir.mkdirs();
        }
        model_watch_dir = new File(new File(data_dir.getParentFile().getParentFile(), "susi_skill_data"), "models");
        private_skill_watch_dir = new File(new File(data_dir.getParentFile().getParentFile(), "susi_private_skill_data"), "users");
        susi_skill_repo = new File(data_dir.getParentFile().getParentFile(), "susi_skill_data/.git");
        susi_private_skill_repo = new File(data_dir.getParentFile().getParentFile(), "susi_private_skill_data/.git");
        File susi_generic_skills = new File(data_dir, "generic_skills");
        if (!susi_generic_skills.exists()) susi_generic_skills.mkdirs();
        File susi_generic_skills_media_discovery = new File(susi_generic_skills, "media_discovery");
        if (!susi_generic_skills_media_discovery.exists()) susi_generic_skills_media_discovery.mkdirs();

        // wake up susi
        File system_skills_general = new File(new File(conf_dir, "system_skills"), "general");
        File system_skills_localmode = new File(new File(conf_dir, "system_skills"), "localmode");
        susi = new SusiMind(susi_chatlog_dir, susi_skilllog_dir, system_skills_general);
        if (model_watch_dir.exists()) susi.addWatchpath(new File(model_watch_dir, "general"));
        if (DAO.getConfig("local.mode", false)) {
            susi.addWatchpath(system_skills_localmode);
            susi.addWatchpath(susi_generic_skills_media_discovery);
        }

        // initialize the memory as a background task to prevent that this blocks too much
        new Thread() {
            public void run() {
                susi.initializeMemory();
            }
        }.start();

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

        /*Chatbot storage*/
        Path susi_chatbot_dir = dataPath.resolve("chatbot");
        susi_chatbot_dir.toFile().mkdirs();
        Path susiChatbot_per = susi_chatbot_dir.resolve("chatbot.json");
        Path susiChatbot_vol = susi_chatbot_dir.resolve("chatbot_session.json");
        chatbot = new JsonTray(susiChatbot_per.toFile(), susiChatbot_vol.toFile(), 1000000);
        OS.protectPath(susiChatbot_per);
        OS.protectPath(susiChatbot_vol);

        /*Skill status storage*/
        Path skill_status_dir = dataPath.resolve("skill_status");
        skill_status_dir.toFile().mkdirs();
        Path skillStatus_per = skill_status_dir.resolve("skillStatus.json");
        Path skillStatus_vol = skill_status_dir.resolve("skillStatus_session.json");
        skillStatus = new JsonTray(skillStatus_per.toFile(), skillStatus_vol.toFile(), 1000000);
        OS.protectPath(skillStatus_per);
        OS.protectPath(skillStatus_vol);

        // Languages supported by a Skill
        Path skillSupportedLanguages_per = skill_status_dir.resolve("skillSupportedLanguages.json");
        Path skillSupportedLanguages_vol = skill_status_dir.resolve("skillSupportedLanguages_session.json");
        skillSupportedLanguages = new JsonTray(skillSupportedLanguages_per.toFile(), skillSupportedLanguages_vol.toFile(), 1000000);
        OS.protectPath(skillSupportedLanguages_per);
        OS.protectPath(skillSupportedLanguages_vol);

        /*Profile Details storage*/
        Path susi_profile_details_dir = dataPath.resolve("profile");
        susi_profile_details_dir.toFile().mkdirs();
        Path profileDetails_per = susi_profile_details_dir.resolve("profileDetails.json");
        Path profileDetails_vol = susi_profile_details_dir.resolve("profileDetails_session.json");
        profileDetails = new JsonTray(profileDetails_per.toFile(), profileDetails_vol.toFile(), 1000000);
        OS.protectPath(profileDetails_per);
        OS.protectPath(profileDetails_vol);

        //5 Star Skill Rating storage
        Path fiveStarSkillRating_per = susi_skill_rating_dir.resolve("fiveStarSkillRating.json");
        Path fiveStarSkillRating_vol = susi_skill_rating_dir.resolve("fiveStarSkillRating_session.json");
        fiveStarSkillRating = new JsonTray(fiveStarSkillRating_per.toFile(), fiveStarSkillRating_vol.toFile(), 1000000);
        OS.protectPath(fiveStarSkillRating_per);
        OS.protectPath(fiveStarSkillRating_vol);

        //Feedback Skill storage
        Path feedbackSkill_per = susi_skill_rating_dir.resolve("feedbackSkill.json");
        Path feedbackSkill_vol = susi_skill_rating_dir.resolve("feedbackSkill_session.json");
        feedbackSkill = new JsonTray(feedbackSkill_per.toFile(), feedbackSkill_vol.toFile(), 1000000);
        OS.protectPath(feedbackSkill_per);
        OS.protectPath(feedbackSkill_vol);

        //Feedback logs for analysis
        Path feedbackLogs_per = susi_skill_rating_dir.resolve("feedbackLogs.json");
        Path feedbackLogs_vol = susi_skill_rating_dir.resolve("feedbackLogs_session.json");
        feedbackLogs = new JsonTray(feedbackLogs_per.toFile(), feedbackLogs_vol.toFile(), 1000000);
        OS.protectPath(feedbackLogs_per);
        OS.protectPath(feedbackLogs_vol);

        //Bookmark Skill storage
        Path bookmarkSkill_per = susi_skill_rating_dir.resolve("bookmarkSkill.json");
        Path bookmarkSkill_vol = susi_skill_rating_dir.resolve("bookmarkSkill_session.json");
        bookmarkSkill = new JsonTray(bookmarkSkill_per.toFile(), bookmarkSkill_vol.toFile(), 1000000);
        OS.protectPath(bookmarkSkill_per);
        OS.protectPath(bookmarkSkill_vol);

        // Country wise skill usage
        Path country_wise_skill_usage_dir = dataPath.resolve("skill_usage");
        country_wise_skill_usage_dir.toFile().mkdirs();
        Path countryWiseSkillUsage_per = country_wise_skill_usage_dir.resolve("countryWiseSkillUsage.json");
        Path countryWiseSkillUsage_vol = country_wise_skill_usage_dir.resolve("countryWiseSkillUsage_session.json");
        countryWiseSkillUsage = new JsonTray(countryWiseSkillUsage_per.toFile(), countryWiseSkillUsage_vol.toFile(), 1000000);
        OS.protectPath(countryWiseSkillUsage_per);
        OS.protectPath(countryWiseSkillUsage_vol);

        // Skill usage storage
        Path susi_skill_usage_dir = dataPath.resolve("skill_usage");
        susi_skill_usage_dir.toFile().mkdirs();
        Path skillUsage_per = susi_skill_usage_dir.resolve("skillUsage.json");
        Path skillUsage_vol = susi_skill_usage_dir.resolve("skillUsage_session.json");
        skillUsage = new JsonTray(skillUsage_per.toFile(), skillUsage_vol.toFile(), 1000000);
        OS.protectPath(skillUsage_per);
        OS.protectPath(skillUsage_vol);

        // Device wise skill usage
        Path device_wise_skill_usage_dir = dataPath.resolve("skill_usage");
        device_wise_skill_usage_dir.toFile().mkdirs();
        Path deviceWiseSkillUsage_per = device_wise_skill_usage_dir.resolve("deviceWiseSkillUsage.json");
        Path deviceWiseSkillUsage_vol = device_wise_skill_usage_dir.resolve("deviceWiseSkillUsage_session.json");
        deviceWiseSkillUsage = new JsonTray(deviceWiseSkillUsage_per.toFile(), deviceWiseSkillUsage_vol.toFile(), 1000000);
        OS.protectPath(deviceWiseSkillUsage_per);
        OS.protectPath(deviceWiseSkillUsage_vol);

        // Skill ratings over time
        Path ratingsOverTime_per = susi_skill_rating_dir.resolve("ratingsOverTime.json");
        Path ratingsOverTime_vol = susi_skill_rating_dir.resolve("ratingsOverTime_session.json");
        ratingsOverTime = new JsonTray(ratingsOverTime_per.toFile(), ratingsOverTime_vol.toFile(), 1000000);
        OS.protectPath(ratingsOverTime_per);
        OS.protectPath(ratingsOverTime_vol);

        // Report a skill as inappropriate
        Path reportedSkills_per = susi_skill_rating_dir.resolve("reportedSkills.json");
        Path reportedSkills_vol = susi_skill_rating_dir.resolve("reportedSkills_session.json");
        reportedSkills = new JsonTray(reportedSkills_per.toFile(), reportedSkills_vol.toFile(), 1000000);
        OS.protectPath(reportedSkills_per);
        OS.protectPath(reportedSkills_vol);

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

        log("Starting Skill Data pull thread");
        Thread pullThread = new Thread() {
            @Override
            public void run() {
                while (DAO.pullStatus) {
                    try {
                        Thread.sleep(getConfig("skill_repo.pull_delay", 60000));
                    } catch (InterruptedException e) {
                        break;
                    }
                    try {
                        DAO.pull(DAO.getGit());
                    } catch (Exception e) {
                        DAO.pullStatus =false;
                        severe("SKILL PULL THREAD", e);
                    }
                }
            }
        };
        pullThread.start();

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
        log("closing DAO");

        // close the tracker
        access.close();

        // close AAA for session hand-over
        authentication.close();
        authorization.close();
        passwordreset.close();
        accounting.close();

        log("closed DAO");
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
        logger.info(line);
    }

    public static void severe(String line) {
        logger.warn(line);
    }

    public static void severe(String line, Throwable e) {
        logger.warn(line, e);
    }

    public static void severe(Throwable e) {
        logger.warn("", e);
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
      Repository repo;
      File repoFile = susi_skill_repo;
      if (repoFile.exists()) {
      // Open an existing repository
      repo = new FileRepositoryBuilder()
      .setGitDir(susi_skill_repo)
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build();
      } else {
        // Create a new repository
        repo = new FileRepositoryBuilder()
        .setGitDir(susi_skill_repo)
        .build();
        repo.create();
      }
      return repo;
    }

    public static Repository getPrivateRepository() throws IOException {
        Repository repo;
        File repoFile = susi_private_skill_repo;
        if (repoFile.exists()) {
        // Open an existing repository
        repo = new FileRepositoryBuilder()
        .setGitDir(susi_private_skill_repo)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();
        } else {
        // Create a new repository
        repo = new FileRepositoryBuilder()
        .setGitDir(susi_private_skill_repo)
        .build();
        repo.create();
        }
        return repo;
    }

    public static Git getGit() throws IOException {
        Git git = new Git(getRepository());
        return git;
    }

    public static Git getPrivateGit() throws IOException {
        Git git = new Git(getPrivateRepository());
        return git;
    }

    public static void pull(Git git) throws IOException {
        try {
            PullResult pullResult = git.pull().call();
            MergeResult mergeResult = pullResult.getMergeResult();

            if (mergeResult!=null && mergeResult.getConflicts()!=null) {
                pullStatus =false;
                // we have conflicts send email to admin
                try {
                    EmailHandler.sendEmail(getConfig("skill_repo.admin_email",""), "SUSI Skill Data Conflicts", getConflictsMailContent(mergeResult));
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            } else {
                PushCommand push = git.push();
                push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getConfig("github.username", ""), getConfig("github.password","")));
                push.call();
            }

        } catch (GitAPIException e) {
            throw new IOException (e.getMessage());
        }
    }
    
    public static Thread addAndPushCommit(String commit_message, String userEmail, boolean concurrently) {
    	Runnable process = new Runnable() {
			@Override
			public void run() {
				try (Git git = getGit()) {
		            long t0 = System.currentTimeMillis();
		            git.add().setUpdate(true).addFilepattern(".").call();
		            long t1 = System.currentTimeMillis();
		            git.add().addFilepattern(".").call(); // takes long 
		            long t2 = System.currentTimeMillis();
		            
		            // commit the changes
		            DAO.pushCommit(git, commit_message, userEmail); // takes long
		            long t3 = System.currentTimeMillis();
		            DAO.log("jgit statistics: add-1: " + (t1 - t0) + "ms, add-2: " + (t2 - t1) + "ms, push: " + (t3 - t2) + "ms");
		        } catch (IOException | GitAPIException e) {
		            e.printStackTrace();

		        }
			}
    	};
    	if (concurrently) {
    		Thread t = new Thread(process);
    		t.start();
    		return t;
    	} else {
    		process.run();
    		return null;
    	}
    }

    /**
     * commit user changes to the skill data repository
     * @param git
     * @param commit_message
     * @param userEmail
     * @throws IOException
     */
    public static void pushCommit(Git git, String commit_message, String userEmail) throws IOException {

        // fix bad email setting
        if (userEmail==null || userEmail.isEmpty()) {
            assert false; // this should not happen
            userEmail = "anonymous@";
        }

        try {
            git.commit()
                    .setAllowEmpty(false)
                    .setAll(true)
                    .setAuthor(new PersonIdent(userEmail,userEmail))
                    .setMessage(commit_message)
                    .call();
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

    private static String getConflictsMailContent(MergeResult mergeResult) throws APIException {
        // get template file
        String result="";
        String conflictLines= "";
        try {
            result = IO.readFileCached(Paths.get(DAO.conf_dir + "/templates/conflicts-mail.txt"));
            for (String path :mergeResult.getConflicts().keySet()) {
                int[][] c = mergeResult.getConflicts().get(path);
                conflictLines+= "Conflicts in file " + path + "\n";
                for (int i = 0; i < c.length; ++i) {
                    conflictLines+= "  Conflict #" + i+1 +"\n";
                    for (int j = 0; j < (c[i].length) - 1; ++j) {
                        if (c[i][j] >= 0)
                            conflictLines+= "    Chunk for "
                                    + mergeResult.getMergedCommits()[j] + " starts on line #"
                                    + c[i][j] +"\n";
                    }
                }
            }
            result = result.replace(conflictsPlaceholder, conflictLines);
        } catch (IOException e) {
            throw new APIException(500, "No conflicts email template");
        }
        return result;
    }
}
