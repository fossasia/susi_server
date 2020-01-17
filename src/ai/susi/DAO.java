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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;

import ai.susi.json.JsonFile;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiMemory;
import ai.susi.mind.SusiMind;
import ai.susi.mind.SusiSkill;
import ai.susi.server.APIException;
import ai.susi.server.AccessTracker;
import ai.susi.server.Accounting;
import ai.susi.server.Authentication;
import ai.susi.server.Authorization;
import ai.susi.server.ClientCredential;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Settings;
import ai.susi.tools.DateParser;
import ai.susi.tools.IO;
import ai.susi.tools.OS;


import org.json.JSONArray;
import org.json.JSONException;
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

    public final static int ATTENTION_TIME = 5;
    private final static String ACCESS_DUMP_FILE_PREFIX = "access_";
    public  static File conf_dir, bin_dir, html_dir, data_dir, skill_status_dir, susi_chatlog_dir, susi_skilllog_dir, draft_dir, model_watch_dir, susi_skill_repo, private_skill_watch_dir, susi_private_skill_repo, deleted_skill_dir, system_keys;
    private static File external_data, assets, dictionaries;
    private static Settings public_settings, private_settings;
    public  static AccessTracker access;
    private static Map<String, String> config = new HashMap<>();
    private static Logger logger;
    public static LogAppender logAppender;

    // AAA Schema for server usage
    private static JsonTray authentication;
    private static JsonTray authorization;
    private static JsonTray accounting;
    public  static JsonTray passwordreset;
    private static JsonFile login_keys;
    public static JsonTray group;
    public static JsonTray apiKeys;
    public static JsonTray captchaConfig;

    // CMS Schema for server usage
    public static JsonTray skillInfo;
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
    public static JsonTray skillSlideshow;
    
    // temporary solution for draft storage
    public static Map<String, Map<String, Draft>> drafts = new HashMap<>(); // key is the user's identity, inner map key is draft id


    static {
        PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSS} %p %c %x - %m%n");
        logger = Logger.getRootLogger();
        logger.removeAllAppenders();
        logAppender = new LogAppender(layout, 100000);
        logger.addAppender(logAppender);
        logger.addAppender(new ConsoleAppender(layout));
        logger.setLevel(Level.INFO);
    }

    // create the mind layers (all have a common memory)
    public static SusiMemory susi_memory;
    public static SusiMind susi_operation_linguistics; // this is the top mind layer
    public static SusiMind susi_operation_skills;      // this is the bottom mind layer
    public static SusiMind susi;
    

    /**
     * initialize the DAO
     * @param configMap
     * @param dataPath the path to the data directory
     */
    public static void init(Map<String, String> configMap, Path dataPath, boolean learnWorldKnowledge) throws Exception{

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
        draft_dir = new File(susi_memory_dir_new, "drafts");
        if (susi_memory_dir_old.exists() && !susi_chatlog_dir.exists()) {
            susi_memory_dir_old.renameTo(susi_chatlog_dir); // migrate old location
        }
        if (!susi_chatlog_dir.exists()) susi_chatlog_dir.mkdirs();
        if (!susi_skilllog_dir.exists()) susi_skilllog_dir.mkdirs();
        if (!draft_dir.exists()) draft_dir.mkdirs();

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
        login_keys = new JsonFile(login_keys_path.toFile(), false);
        OS.protectPath(login_keys_path);

        // Skill usage storage
        Path susi_skill_usage_dir = dataPath.resolve("skill_usage");
        susi_skill_usage_dir.toFile().mkdirs();
        Path skillUsage_per = susi_skill_usage_dir.resolve("skillUsage.json");
        Path skillUsage_vol = susi_skill_usage_dir.resolve("skillUsage_session.json");
        skillUsage = new JsonTray(skillUsage_per.toFile(), skillUsage_vol.toFile(), 1000000);
        OS.protectPath(skillUsage_per);
        OS.protectPath(skillUsage_vol);

        Path groups_per = settings_dir.resolve("groups.json");
        Path groups_vol = settings_dir.resolve("groups_session.json");
        group = new JsonTray(groups_per.toFile(), groups_vol.toFile(), 1000000);
        OS.protectPath(groups_per);
        OS.protectPath(groups_vol);

        /*System Keys storage*/
        Path system_keys_dir = dataPath.resolve("system_keys");
        system_keys_dir.toFile().mkdirs();
        Path apiKeys_per = system_keys_dir.resolve("apiKeys.json");
        Path apiKeys_vol = system_keys_dir.resolve("apiKeys_session.json");
        apiKeys = new JsonTray(apiKeys_per.toFile(), apiKeys_vol.toFile(), 1000000);
        OS.protectPath(apiKeys_per);
        OS.protectPath(apiKeys_vol);

        /*ReCaptcha config*/
        Path captchaConfig_per = settings_dir.resolve("captchaConfig.json");
        Path captchaConfig_vol = settings_dir.resolve("captchaConfig_session.json");
        captchaConfig = new JsonTray(captchaConfig_per.toFile(), captchaConfig_vol.toFile(), 1000000);
        OS.protectPath(captchaConfig_per);
        OS.protectPath(captchaConfig_vol);

        /* Basic attributes related to a skill*/
        Path susi_skill_info_dir = dataPath.resolve("skill_info");
        susi_skill_info_dir.toFile().mkdirs();
        Path skillInfo_per = susi_skill_info_dir.resolve("skillInfo.json");
        Path skillInfo_vol = susi_skill_info_dir.resolve("skillInfo_session.json");
        skillInfo = new JsonTray(skillInfo_per.toFile(), skillInfo_vol.toFile(), 1000000);
        OS.protectPath(skillInfo_per);
        OS.protectPath(skillInfo_vol);

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

        //Skill slideshow
        Path skillSlideshow_per = susi_skill_rating_dir.resolve("skillSlideshow.json");
        Path skillSlideshow_vol = susi_skill_rating_dir.resolve("skillSlideshow_session.json");
        skillSlideshow = new JsonTray(skillSlideshow_per.toFile(), skillSlideshow_vol.toFile(), 1000000);
        OS.protectPath(skillSlideshow_per);
        OS.protectPath(skillSlideshow_vol);

        // initialize the memory as a background task to prevent that this blocks too much
        susi_memory = new SusiMemory(susi_chatlog_dir, susi_skilllog_dir, ATTENTION_TIME);
        new Thread() {
            public void run() {
                susi_memory.initializeMemory();
            }
        }.start();

        // prepare skill repository
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
        SusiMind.Layer susi_generic_skills_media_discovery = new SusiMind.Layer("Media Discovery", new File(susi_generic_skills, "media_discovery"), false);
        if (!susi_generic_skills_media_discovery.path.exists()) susi_generic_skills_media_discovery.path.mkdirs();

        // wake up susi
        susi = new SusiMind(susi_memory);
        if (learnWorldKnowledge) {
            SusiMind.Layer system_skills_include = new SusiMind.Layer("General", new File(new File(conf_dir, "os_skills"), "include"), true);
            SusiMind.Layer system_skills_linuguistic = new SusiMind.Layer("General", new File(new File(conf_dir, "os_skills"), "linguistic"), true);
            SusiMind.Layer system_skills_operation = new SusiMind.Layer("General", new File(new File(conf_dir, "os_skills"), "operation"), true);
            SusiMind.Layer system_skills_system = new SusiMind.Layer("General", new File(new File(conf_dir, "os_skills"), "system"), true);
            SusiMind.Layer system_skills_test = new SusiMind.Layer("Local", new File(new File(conf_dir, "os_skills"), "test"), true);
            susi.addLayer(system_skills_include);
            susi.addLayer(system_skills_linuguistic);
            susi.addLayer(system_skills_operation);
            susi.addLayer(system_skills_system);
            susi.addLayer(system_skills_test);
            if (model_watch_dir.exists()) {
                SusiMind.Layer model_skills = new SusiMind.Layer("Model", new File(model_watch_dir, "general"), false);
                susi.addLayer(model_skills);
            }
            if (DAO.getConfig("local.mode", false)) {
                susi.addLayer(susi_generic_skills_media_discovery);
            }
        }

        // learn all available intents
        new Thread() {
            public void run() {
                observe();
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

        // open index
        Path index_dir = dataPath.resolve("index");
        if (index_dir.toFile().exists()) OS.protectPath(index_dir); // no other permissions to this path

        // create and document the data dump dir
        assets = new File(data_dir, "assets");
        external_data = new File(data_dir, "external");
        dictionaries = new File(external_data, "dictionaries");
        dictionaries.mkdirs();

        // initializing susi minf concurrently
        Thread susi_mind_init = new Thread() {
            public void run() {
                Thread.currentThread().setName("ObserveLearn");
                try {
                    susi.observe();
                } catch (IOException e) {
                    DAO.severe(e);
                }
            }
        };
        susi_mind_init.start();

        // initializing the log concurrently
        Path log_dump_dir = dataPath.resolve("log");
        log_dump_dir.toFile().mkdirs();
        OS.protectPath(log_dump_dir); // no other permissions to this path
        access = new AccessTracker(log_dump_dir.toFile(), ACCESS_DUMP_FILE_PREFIX, 60000, 3000);
        access.start(); // start monitor

        if (getConfig("skill_repo.enable", true)) {
                log("Starting Skill Data pull thread");
                SkillTransactions.init(getConfig("skill_repo.pull_delay", 60000));
        }
        log("finished DAO initialization");
    }

    public static void observe() {
        try {
            susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }
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

        // close pull thread
        SkillTransactions.close();

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

    public static void setConfig(String key, String value) {
        config.put(key, value);
    }

    public static Set<String> getConfigKeys() {
        return config.keySet();
    }

    /**
    * Checks if the client's domain is allowed by the bot creator
    * @param configureObject
    * @param @referer
    */
    public static boolean allowDomainForChatbot(JSONObject configureObject, String referer) {
        Boolean allowed_site = true;
        if (configureObject.getBoolean("allow_bot_only_on_own_sites") && configureObject.has("allowed_sites") && configureObject.getString("allowed_sites").length() > 0) {
            allowed_site = false;
            if (referer != null && referer.length() > 0) {
                String[] sites = configureObject.getString("allowed_sites").split(",");
                for (int i = 0; i < sites.length; i++) {
                    String site = sites[i].trim();
                    int referer_index = referer.indexOf("://");
                    String host = referer;
                    if (referer.indexOf('/',referer_index+3) > -1) {
                        host = referer.substring(0,referer.indexOf('/',referer_index+3));
                    }
                    if (host.equalsIgnoreCase(site)) {
                        allowed_site = true;
                        break;
                    }
                }
            }
        }
        return allowed_site; 
    }

    public static void deleteChatbot(String userId,String group,String language,String skill) {
        JsonTray chatbot = DAO.chatbot;
        JSONObject userIdName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        if (chatbot.has(userId)) {
            userIdName = chatbot.getJSONObject(userId);
            if (userIdName.has(group)) {
                groupName = userIdName.getJSONObject(group);
                if (groupName.has(language)) {
                    languageName = groupName.getJSONObject(language);
                    languageName.remove(skill);
                    chatbot.commit();
                }
            }
        }
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

    
    public static class Draft {
		JSONObject object;
    	Date created, modified;
    	public Draft(JSONObject o) {
    		this.object = o;
    		this.created = new Date();
    		this.modified = this.created;
    	}
    	public JSONObject getObject() {
			return object;
		}
		public Date getCreated() {
			return created;
		}
		public void setCreated(Date created) {
			this.created = created;
		}
		public Date getModified() {
			return modified;
		}
		public void setModified(Date modified) {
			this.modified = modified;
		}
    }
    
    public static void storeDraft(@Nonnull final ClientIdentity identity, final String id, final Draft draft) {
    	Map<String, Draft> d = drafts.get(identity.getClient());
    	if (d == null) {
    		d = new HashMap<>();
    		drafts.put(identity.getClient(), d);
    	}
    	Draft old = d.get(id);
    	if (old != null) draft.setCreated(old.getCreated());
    	d.put(id, draft);
    }

    public static Map<String, Draft> readDrafts(@Nonnull final ClientIdentity identity, final String... ids) {
    	Map<String, Draft> d = drafts.get(identity.getClient());
    	Map<String, Draft> r = new HashMap<>();
    	if (d == null) return r;
    	if (ids.length == 0) {
    		d.forEach(r::put);
    		return r;
    	}
    	for (String id: ids) if (d.containsKey(id)) r.put(id, d.get(id));
    	return r;
    }

    public static void deleteDraft(@Nonnull final ClientIdentity identity, final String id) {
    	Map<String, Draft> d = drafts.get(identity.getClient());
    	if (d != null) d.remove(id);
    }
    
    public static JSONObject getSkillRating(String model, String group, String language, String skillname) {
        // rating
        JsonTray skillRating = DAO.skillRating;
        if (skillRating.has(model)) {
            JSONObject modelName = skillRating.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);

                        putStars(skillName);
                        return skillName;
                    }
                }
            }
        }
        JSONObject newRating=new JSONObject();
        newRating.put("negative", "0");
        newRating.put("positive", "0");
        newRating.put("feedback_count", 0);
        newRating.put("bookmark_count", 0);

        JSONObject newFiveStarRating=new JSONObject();
        newFiveStarRating.put("one_star", 0);
        newFiveStarRating.put("two_star", 0);
        newFiveStarRating.put("three_star", 0);
        newFiveStarRating.put("four_star", 0);
        newFiveStarRating.put("five_star", 0);
        newFiveStarRating.put("avg_star", 0);
        newFiveStarRating.put("total_star", 0);

        newRating.put("stars", newFiveStarRating);

        return newRating;
    }

    public static void putStars(JSONObject skillName) {
        if (!skillName.has("stars")) {
            JSONObject newFiveStarRating = new JSONObject();
            newFiveStarRating.put("one_star", 0);
            newFiveStarRating.put("two_star", 0);
            newFiveStarRating.put("three_star", 0);
            newFiveStarRating.put("four_star", 0);
            newFiveStarRating.put("five_star", 0);
            newFiveStarRating.put("avg_star", 0);
            newFiveStarRating.put("total_star", 0);

            skillName.put("stars", newFiveStarRating);
        }
    }

    public static JSONArray getSupportedLanguages(String model, String group, String language, String skillname) {
        // rating
        JsonTray skillRating = DAO.skillSupportedLanguages;
        if (skillRating.has(model)) {
            JSONObject modelName = skillRating.getJSONObject(model);
            if (modelName.has(group)) {
                JSONArray groupName = modelName.getJSONArray(group);

                for (int i = 0; i < groupName.length(); i++) {
                    JSONArray supportedLanguages = groupName.getJSONArray(i);
                    for (int j = 0; j < supportedLanguages.length(); j++) {
                        JSONObject languageObject = supportedLanguages.getJSONObject(j);
                        String supportedLanguage = languageObject.get("language").toString();
                        String skillName = languageObject.get("name").toString();
                        if (supportedLanguage.equalsIgnoreCase(language) && skillName.equalsIgnoreCase(skillname)) {
                            return supportedLanguages;
                        }
                    }
                }
            }
        }
        JSONArray supportedLanguages = new JSONArray();
        JSONObject languageObject = new JSONObject();
        languageObject.put("language", language);
        languageObject.put("name", skillname);
        supportedLanguages.put(languageObject);
        return supportedLanguages;
    }

    public static String getSkillModifiedTime(String model_name, String group_name, String language_name, String skill_name) {
        // SKill Info
        JsonTray skillInfo = DAO.skillInfo;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        DateFormat dateFormatType = DateParser.iso8601Format;
        String skillModifiedTime = dateFormatType.format(new Date(0));

        if (skillInfo.has(model_name)) {
            modelName = skillInfo.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                        if (skillName.has("lastModifiedTime")) {
                            skillModifiedTime = skillName.getString("lastModifiedTime");
                        } else {
                            skillName.put("lastModifiedTime", skillModifiedTime);
                        }
                        return skillModifiedTime;
                    }
                }
            }
        }

        skillName.put("lastModifiedTime",skillModifiedTime);
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillInfo.put(model_name, modelName, true);
        return skillModifiedTime;
    }

    public static String getSkillCreationTime(String model_name, String group_name, String language_name, String skill_name, File skill_path) {
        // Skill Info
        JsonTray skillInfo = DAO.skillInfo;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        DateFormat dateFormatType = DateParser.iso8601Format;
        String skillCreationTime = dateFormatType.format(new Date(0));

        if (skillInfo.has(model_name)) {
            modelName = skillInfo.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                        if (skillName.has("creationTime")) {
                            skillCreationTime = skillName.getString("creationTime");
                        } else {
                            skillCreationTime = getFileCreationTime(skill_path);
                            skillName.put("creationTime", skillCreationTime );
                        }
                        return skillCreationTime;
                    }
                }
            }
        }

        skillCreationTime = getFileCreationTime(skill_path);
        skillName.put("creationTime",skillCreationTime);
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillInfo.put(model_name, modelName, true);
        return skillCreationTime;
    }

    public static String getFileCreationTime(File skill_path) {

        DateFormat dateFormatType = DateParser.iso8601Format;
        String skillCreationTime = dateFormatType.format(new Date(0));

        BasicFileAttributes attr = null;
        Path p = Paths.get(skill_path.getPath());
        try {
            attr = Files.readAttributes(p, BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(attr!=null){
            FileTime fileCreationTime = attr.creationTime();
            skillCreationTime = dateFormatType.format(new Date(fileCreationTime.toMillis()));
        }
        return skillCreationTime;
    }

    public static void sortByMostRating(List<JSONObject> jsonValues, boolean ascending) {
        // Get skills based on most ratings
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                Object valA, valB;
                int result=0;

                try {
                    valA = a.opt("skill_rating");
                    valB = b.opt("skill_rating");
                    if (valA == null || !((valA instanceof JSONObject))) valA = new JSONObject().put("stars", new JSONObject().put("avg_star", 0.0f));
                    if (valB == null || !((valB instanceof JSONObject))) valB = new JSONObject().put("stars", new JSONObject().put("avg_star", 0.0f));

                    JSONObject starsAObject = ((JSONObject) valA).getJSONObject("stars");
                    JSONObject starsBObject = ((JSONObject) valB).getJSONObject("stars");
                    int starsA = starsAObject.has("total_star") ? starsAObject.getInt("total_star") : 0;
                    int starsB = starsBObject.has("total_star") ? starsBObject.getInt("total_star") : 0;

                    float avgA = starsAObject.getFloat("avg_star");
                    float avgB = starsBObject.getFloat("avg_star");

                    if ((starsA < 10 && starsB < 10) || (starsA >= 10 && starsB >= 10)) {
                        result = ascending ? Float.compare(avgA, avgB) : Float.compare(avgB, avgA);
                    } else if (starsA < 10) {
                        return ascending ? -1 : 1;
                    } else {
                        return ascending ? 1 : -1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static void sortByTopRating(List<JSONObject> jsonValues, boolean ascending) {
        // Get skills based on top ratings
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                Object valA, valB;
                int result=0;

                try {
                    valA = a.opt("skill_rating");
                    valB = b.opt("skill_rating");
                    if (valA == null || !((valA instanceof JSONObject))) valA = new JSONObject().put("stars", new JSONObject().put("avg_star", 0.0f));
                    if (valB == null || !((valB instanceof JSONObject))) valB = new JSONObject().put("stars", new JSONObject().put("avg_star", 0.0f));
                    JSONObject starsAObject = ((JSONObject) valA).getJSONObject("stars");
                    JSONObject starsBObject = ((JSONObject) valB).getJSONObject("stars");
                    float avgA = starsAObject.getFloat("avg_star");
                    float avgB = starsBObject.getFloat("avg_star");
                    return ascending ? Float.compare(avgA, avgB) : Float.compare(avgB, avgA);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static void sortByCreationTime(List<JSONObject> jsonValues, boolean ascending) {
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            private static final String KEY_NAME = "creationTime";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();
                int result = 0;

                try {
                    valA = a.get(KEY_NAME).toString();
                    valB = b.get(KEY_NAME).toString();
                    result = ascending ? valA.compareToIgnoreCase(valB) : valB.compareToIgnoreCase(valA);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static void sortByModifiedTime(List<JSONObject> jsonValues, boolean ascending) {
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            private static final String KEY_NAME = "lastModifiedTime";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();
                int result = 0;

                try {
                    valA = a.get(KEY_NAME).toString();
                    valB = b.get(KEY_NAME).toString();
                    result = ascending ? valA.compareToIgnoreCase(valB) : valB.compareToIgnoreCase(valA);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static void sortBySkillName(List<JSONObject> jsonValues, boolean ascending) {
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            private static final String KEY_NAME = "skill_name";
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();

                try {
                    valA = a.get(KEY_NAME).toString();
                    valB = b.get(KEY_NAME).toString();
                } catch (JSONException e) {
                    //do nothing
                }
                return ascending ? valA.compareToIgnoreCase(valB) : valB.compareToIgnoreCase(valA);
            }
        });
    }

    public static void sortByUsageCount(List<JSONObject> jsonValues, boolean ascending) {
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                int valA;
                int valB;
                int result=0;

                try {
                    valA = a.getInt("usage_count");
                    valB = b.getInt("usage_count");
                    result = ascending ? Integer.compare(valA, valB) : Integer.compare(valB, valA);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static void sortByFeedbackCount(List<JSONObject> jsonValues, boolean ascending) {
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                Object valA, valB;
                int result=0;

                try {
                    valA = a.opt("skill_rating");
                    valB = b.opt("skill_rating");
                    if (valA == null || !(valA instanceof JSONObject) || ((JSONObject) valA).opt("feedback_count") == null) valA = new JSONObject().put("feedback_count", 0);
                    if (valB == null || !(valB instanceof JSONObject) || ((JSONObject) valB).opt("feedback_count") == null) valB = new JSONObject().put("feedback_count", 0);

                    result = ascending ?
                            Integer.compare(
                                    ((JSONObject) valA).getInt("feedback_count"),
                                    ((JSONObject) valB).getInt("feedback_count")) :
                            Integer.compare(
                                    ((JSONObject) valB).getInt("feedback_count"),
                                    ((JSONObject) valA).getInt("feedback_count")
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            }
        });
    }

    public static boolean getSkillReviewStatus(String model, String group, String language, String skillname) {
        // skill status
        JsonTray skillStatus = DAO.skillStatus;
        if (skillStatus.has(model)) {
            JSONObject modelName = skillStatus.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);

                        if (skillName.has("reviewed")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean getSkillEditStatus(String model, String group, String language, String skillname) {
        // skill status
        JsonTray skillStatus = DAO.skillStatus;
        if (skillStatus.has(model)) {
            JSONObject modelName = skillStatus.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);

                        if (skillName.has("editable")) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isStaffPick(String model, String group, String language, String skillname) {
        // return true if skill is a staff pick
        JsonTray skillStatus = DAO.skillStatus;
        if (skillStatus.has(model)) {
            JSONObject modelName = skillStatus.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);

                        if (skillName.has("staffPick")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSystemSkill(String model, String group, String language, String skillname) {
        // return true if skill is a system skill
        JsonTray skillStatus = DAO.skillStatus;
        if (skillStatus.has(model)) {
            JSONObject modelName = skillStatus.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);

                        if (skillName.has("systemSkill")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int getSkillUsage(String model, String group, String language, String skillname, int duration) {
        // usage
        int totalUsage = 0;
        JsonTray skillUsage = DAO.skillUsage;
        if (skillUsage.has(model)) {
            JSONObject modelName = skillUsage.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONArray skillName = languageName.getJSONArray(skillname);

                        // Fetch skill usage data for sorting purpose.
                        int startIndex = 0;
                        if(duration >= 0) {
                            startIndex = skillName.length() >= duration ? skillName.length()-duration : 0;
                        }
                        for (int i = startIndex; i<skillName.length(); i++)
                        {
                            JSONObject dayUsage = skillName.getJSONObject(i);
                            totalUsage += dayUsage.getInt("count");
                        }
                        return totalUsage;
                    }
                    else {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }
    
    /**
     * For some strange reason the skill name is requested here in lowercase, while the name may also be uppercase
     * this should be fixed in the front-end, however we implement a patch here to circumvent the problem if possible
     * Another strange effect is, that some file systems do match lowercase with uppercase (like in windows),
     * so testing skill.exists() would return true even if the name does not exist exactly as given in the file system.
     * @param languagepath
     * @param skill_name
     * @return the actual skill file if one exist or a skill file that is constructed from language and skill_name
     * @throws SusiActionException 
     */
    public static File getSkillFileInLanguage(File languagepath, String skill_name, boolean null_if_not_found) {

        String fn = skill_name + ".txt";
        String[] list = languagepath.list();
        
        DAO.log("getSkillFileInLanguage: languagepath:"+languagepath+ ", skill_name:" + skill_name + ", null_if_not_found:" + null_if_not_found);

        // first try: the skill name may be same or similar to the skill file name
        if(list !=null && list.length!=0){
            for (String n: list) {
                if (n.equals(fn) || n.toLowerCase().equals(fn)) {
                    return new File(languagepath, n);
                }
            }
        }

        // second try: the skill name may be same or similar to the skill name within the skill description
        // this is costly: we must parse the whole skill file
        if (list != null && list.length != 0){
            for (String n: list) {
                if (!n.endsWith(".txt") && !n.endsWith(".ezd")) continue;
                File f = new File(languagepath, n);
                try {
                    SusiSkill.ID skillid = new SusiSkill.ID(f);
                    SusiSkill skill = new SusiSkill(new BufferedReader(new FileReader(f)), skillid, false);
                    String sn = skill.getSkillName();
                    if (sn != null && (sn.equals(skill_name) || sn.toLowerCase().equals(skill_name) || sn.toLowerCase().replace(' ', '_').equals(skill_name))) {
                        return new File(languagepath, n);
                    }
                } catch (IOException | JSONException | SusiActionException e) {
                    continue;
                }
            }
        }

        // the final attempt is bad and may not succeed, but it's the only last thing left we could do.
        return null_if_not_found ? null : new File(languagepath, fn);
    }

    /**
     * the following method scans a given model for all files to see if it matches the skill name
     * @param model a path to a model directory
     * @param skill_name
     * @return
     * @throws SusiActionException 
     */
    public static File getSkillFileInModel(File model, String skill_name) throws SusiActionException {
        String[] groups = model.list();
        for (String group: groups) {
            if (group.startsWith(".")) continue;
            File gf = new File(model, group);
            if (!gf.isDirectory()) continue;
            String[] languages = gf.list();
            for (String language: languages) {
                if (language.startsWith(".")) continue;
                File l = new File(gf, language);
                if (!l.isDirectory()) continue;
                File skill = getSkillFileInLanguage(l, skill_name, true);
                if (skill != null) return skill;
            }
        }
        return null;
    }
}
