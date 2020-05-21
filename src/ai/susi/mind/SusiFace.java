/**
 *  SusiFace
 *  Copyright 21.05.2020 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.mind;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.server.APIException;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.tools.EtherpadClient;
import ai.susi.tools.IO;
import ai.susi.tools.OnlineCaution;

/**
 * The SusiFace class is the interface from outside (http requests) to the inside (susi mind).
 * It implements the callable interface to be used in a concurrent framework.
 */
public class SusiFace implements Callable<SusiCognition> {

    private String  q, clientHost, referer, countryName, countryCode, language, deviceType, privateSkill,
                    userId, group_name, skill_name, excludeDefaultSkills;
    private String  dream = ""; // an instant dream setting, to be used for permanent dreaming
    private String  focus = ""; // a focused skill, one that can be activated an stays focused until the skill is ended. Focused Skills are created using the meta "on" inside.
    private String  persona = ""; // an instant persona setting, to be used for permanent personas. This should be the persona name
    private String  instant = ""; // an instant skill text, given as LoT directly here. This should be a complete app/persona/skill text.
    private Boolean exclude_default_skills = false; // if this is true, default skills would be excluded
    private int     timezoneOffset; // minutes, i.e. -60
    private double  latitude, longitude; // i.e. 8.68 , 50.11
    private Authorization user;
    private boolean debug;


    public SusiFace(String q) {
        this.q = q;

        this.clientHost = "localhost";
        this.referer = "";
        this.timezoneOffset = 0;
        this.latitude = Double.NaN;
        this.longitude = Double.NaN;
        this.countryName = "";
        this.countryCode = "";
        this.language = "en";
        this.deviceType = "Others";
        this.dream = "";
        this.focus = "";
        this.persona = "";
        this.instant = "";
        this.privateSkill = null;
        this.userId = "";
        this.group_name = "";
        this.skill_name = "";
        this.excludeDefaultSkills = "";
        this.exclude_default_skills = false;
        this.user = null;
        this.debug = false;
    }

    public SusiFace setAuthorization(Authorization user) {
        this.user = user;
        return this;
    }

    public SusiFace setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public SusiFace setPost(Query post) {

        OnlineCaution.demand("SusiService", 5000);

        // parameters
        this.clientHost = post.getClientHost();
        this.referer = post.getRequest().getHeader("Referer");
        this.timezoneOffset = post.get("timezoneOffset", 0);
        this.latitude = post.get("latitude", Double.NaN);
        this.longitude = post.get("longitude", Double.NaN);
        this.countryName = post.get("country_name", "");
        this.countryCode = post.get("country_code", "");
        this.language = post.get("language", "en");
        this.deviceType = post.get("device_type", "Others");
        this.dream = post.get("dream", "");
        this.focus = post.get("focus", "");
        this.persona = post.get("persona", "");
        this.instant = post.get("instant", "");
        // for applying private skill
        this.privateSkill = post.get("privateskill", null);
        this.userId = post.get("userid", "");
        this.group_name = post.get("group", "");
        this.skill_name = post.get("skill", "");
        this.excludeDefaultSkills = post.get("excludeDefaultSkills","");
        this.exclude_default_skills = false;

        if (this.excludeDefaultSkills.equalsIgnoreCase("true")) {
            this.exclude_default_skills = true;
        }

        return this;
    }

    @Override
    public SusiCognition call() throws Exception {

        DAO.observe(); // get a database update

        // compute a recall
        ArrayList<SusiThought> recalls = new ArrayList<>();
        List<SusiCognition> cognitions = DAO.susi_memory.getCognitions(user.getIdentity().getClient(), true);
        cognitions.forEach(cognition -> recalls.add(cognition.recallDispute()));
        SusiThought recall = SusiArgument.mindmeld(recalls, false);

        // now that we have a recall, use it to set the dream/persona
        if (dream == null || dream.length() == 0) {
            dream = recall.getObservation("_etherpad_dream");
        }
        if (persona == null || persona.length() == 0) {
            persona = recall.getObservation("_persona_awake");
        }

        // the focused skill is a single skill which can be activated and has special abilities,
        // like it may have catchall-phrases, a greeting phrase and a good-by phrase
        if (focus == null || focus.length() == 0) {
            focus = recall.getObservation("_focused_on");
        }

        // read language preferences; this may overwrite the given call information
        String user_language = recall.getObservation("_user_language");
        if (user_language != null && user_language.length() > 0) language = user_language;
        SusiLanguage susi_language = SusiLanguage.parse(language);

        // we create a hierarchy of minds which overlap each other completely. The first element in the array is the 'most conscious' mind.
        List<SusiMind> minds = new ArrayList<>();

        // instant dreams
        if (instant != null && instant.length() > 0) try {
            instant = instant.replaceAll("\\\\n", "\n"); // yes, the number of "\" is correct
            // fill an empty mind with the skilltext
            SusiMind instantMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
            SusiSkill.ID skillid = new SusiSkill.ID(SusiLanguage.unknown, "instant");
            SusiSkill skill = new SusiSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(instant.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), skillid, true);
            instantMind.learn(skill, skillid, true);
            SusiSkill activeskill = instantMind.getSkillMetadata().get(skillid);
            instantMind.setActiveSkill(activeskill);
            minds.add(instantMind);
        } catch (IOException | JSONException | SusiActionException e) {
            DAO.severe(e.getMessage(), e);
        }

        // Local etherpad dreaming, reading from http://localhost:9001
        // This cannot be activated, its always reading a dream named "susi"
        // We consider two location options for the etherpad,
        // either ~/SUSI.AI/etherpad-lite or data/etherpad-lite
        EtherpadClient etherpad = new EtherpadClient();
        if (etherpad.isPrivate()) try {
            String content = etherpad.setTextIfEmpty("susi", new File(new File(DAO.conf_dir, "etherpad_dream_lot_tutorial"), "susi.txt"));
            if (EtherpadClient.padContainsSkill(content)) {
                // fill an empty mind with the dream
                SusiMind dreamMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
                SusiSkill.ID skillid = new SusiSkill.ID(susi_language, "susi");
                SusiSkill skill = new SusiSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), skillid, true);
                dreamMind.learn(skill, skillid, true);
                SusiSkill activeskill = dreamMind.getSkillMetadata().get(skillid);
                dreamMind.setActiveSkill(activeskill);
                minds.add(dreamMind);
            }
        } catch (JSONException | IOException | SusiActionException e) {
            // ignore silently if pad is not available
            //DAO.severe(e.getMessage(), e);
        }

        // global etherpad dreaming, reading from http://dream.susi.ai
        if (dream != null && dream.length() > 0) try {
            // read the pad for the dream
            String text = etherpad.getText(dream);
            // in case that the text contains a "*" we are in danger that we cannot stop dreaming, therefore we simply add the stop rule here to the text
            text = text + "\n\nwake up|stop dream|stop dreaming|end dream|end dreaming\ndreaming disabled^^>_etherpad_dream\n\n";
            text = text + "\n\ndream *\nI am currently dreaming $_etherpad_dream$, first wake up before dreaming again\n\n";
            // fill an empty mind with the dream
            SusiMind dreamMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
            SusiSkill.ID skillid = new SusiSkill.ID(susi_language, dream);
            SusiSkill skill = new SusiSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), skillid, true);
            dreamMind.learn(skill, skillid, true);
            SusiSkill activeskill = dreamMind.getSkillMetadata().get(skillid);
            dreamMind.setActiveSkill(activeskill);
            // susi is now dreaming.. Try to find an answer out of the dream
            minds.add(dreamMind);
        } catch (JSONException | IOException | SusiActionException e) {
            // ignore silently if pad is not available
            //DAO.severe(e.getMessage(), e);
        }

        // on-skills: if a user has switched on a skill with "run skill" of a skill which has the "on"-property
        if (focus != null && focus.length() > 0) try {

            SusiMind focusMind = new SusiMind(DAO.susi_memory);
            Set<SusiSkill> focus_skills = DAO.susi.getFocusSkills(focus);
            if (focus_skills == null || focus_skills.isEmpty()) {
                DAO.log("tried to load non-existing focus skill " + focus);
            } else {
                for (SusiSkill focus_skill: focus_skills) {
                    String originpath = focus_skill.getID().getPath();
                    SusiSkill.ID skillid = new SusiSkill.ID(susi_language, originpath);
                    focusMind.learn(focus_skill, skillid, true);
                    minds.add(focusMind);
                }
            }
        } catch (JSONException e) {
            DAO.severe(e.getMessage(), e);
        }

        // a persona or a private skill
        if ((persona != null && persona.length() > 0) || (privateSkill != null && userId.length() > 0 && group_name.length() > 0 && language.length() > 0 && skill_name.length() > 0)) try {
            File skillfile = null;
            if (persona != null && persona.length() > 0) {
              skillfile = DAO.getSkillFileInModel(new File(DAO.model_watch_dir, "persona"), persona);
            } else {
              // read the private skill
              File language_file = IO.resolvePath(DAO.private_skill_watch_dir.toPath(), userId, group_name, language).toFile();
              skillfile = DAO.getSkillFileInLanguage(language_file, skill_name, false);
              // extracting configuration settings of this private skill
              JsonTray chatbot = DAO.chatbot;
              JSONObject userName = new JSONObject();
              JSONObject groupName = new JSONObject();
              JSONObject languageName = new JSONObject();
              JSONObject skillName = new JSONObject();
              if (chatbot.has(userId)) {
                userName = chatbot.getJSONObject(userId);
                if (userName.has(group_name)) {
                    groupName = userName.getJSONObject(group_name);
                    if (groupName.has(language)) {
                        languageName = groupName.getJSONObject(language);
                        if (languageName.has(skill_name)) {
                            skillName = languageName.getJSONObject(skill_name);
                            //Boolean allowed_site = true;
                            JSONObject configureName = skillName.getJSONObject("configure");
                            if (configureName.getBoolean("enable_default_skills") == false) {
                                exclude_default_skills = true;
                            }
                            if (DAO.allowDomainForChatbot(configureName, this.referer) == false) {
                                throw new APIException(500, "Not allowed to use on this domain");
                            }
                        }
                    }
                }
              }
            }
            // read the persona
            if (skillfile != null) {
                String text = new String(Files.readAllBytes(skillfile.toPath()), StandardCharsets.UTF_8);
                // in case that the text contains a "*" we are in danger that we cannot sleep again, therefore we simply add the stop rule here to the text
                text = text + "\n\n* conscious mode|* conscious|conscious *\n$1$>_persona_awake is now conscious\n\nsleep|forget yourself|no yourself|unconscious|unconscious mode|That's enough|* dreamless slumber|Freeze|Cease * functions\nPersona will sleep now. Unconscious state activated.^^>_persona_awake\n\n";

                // fill an empty mind with the dream
                SusiMind awakeMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
                SusiSkill.ID skillid = new SusiSkill.ID(SusiLanguage.unknown, skillfile.getName());
                SusiSkill skill = new SusiSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), skillid, false);
                awakeMind.learn(skill, skillid, true);
                SusiSkill activeskill = awakeMind.getSkillMetadata().get(skillid);
                awakeMind.setActiveSkill(activeskill);
                // we are awake!
                minds.add(awakeMind);
            }
        } catch (JSONException | IOException | SusiActionException e) {
            e.printStackTrace();
        }

        // finally add the general mind definition. It's there if no other mind is conscious or the other minds do not find an answer.
        if (exclude_default_skills == false) {
            minds.add(DAO.susi);
        }

        // answer using the mind-stack
        SusiCognition cognition = new SusiCognition(q, this.clientHost, timezoneOffset, latitude, longitude, countryCode, countryName, language, deviceType, user.getIdentity(), debug, minds.toArray(new SusiMind[minds.size()]));
        if (cognition.getAnswerThoughts().size() > 0) try {
            DAO.susi_memory.addCognition(user.getIdentity().getClient(), cognition, debug /*storeToCache*/);
        } catch (IOException e) {
            DAO.severe(e.getMessage());
        }
        return cognition;
    }

}
