/**
 *  SusiService
 *  Copyright 29.06.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.susi;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiArgument;
import ai.susi.mind.SusiCognition;
import ai.susi.mind.SusiLanguage;
import ai.susi.mind.SusiMind;
import ai.susi.mind.SusiSkill;
import ai.susi.mind.SusiThought;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import org.json.JSONException;
import org.json.JSONObject;
import ai.susi.json.JsonTray;
import org.json.JSONTokener;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SusiService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/chat.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "").trim();
        int count = post.get("count", 1);
        int timezoneOffset = post.get("timezoneOffset", 0); // minutes, i.e. -60
        double latitude = post.get("latitude", Double.NaN); // i.e. 8.68
        double longitude = post.get("longitude", Double.NaN); // i.e. 50.11
        String countryName = post.get("country_name", "");
        String countryCode = post.get("country_code", "");
        String language = post.get("language", "en");
        String deviceType = post.get("device_type", "Others");
        String dream = post.get("dream", ""); // an instant dream setting, to be used for permanent dreaming
        String focus = post.get("focus", ""); // a focused skill, one that can be activated an stays focused until the skill is ended. Focused Skills are created using the meta "on" inside.
        String persona = post.get("persona", ""); // an instant persona setting, to be used for permanent personas. This should be the persona name
        String instant = post.get("instant", ""); // an instant skill text, given as LoT directly here. This should be a complete app/persona/skill text.
        // for applying private skill
        String privateSkill = post.get("privateskill", null);
        String userId = post.get("userid", "");
        String group_name = post.get("group", "");
        String skill_name = post.get("skill", "");
        String excludeDefaultSkills = post.get("excludeDefaultSkills","");
        Boolean exclude_default_skills = false; // if this is true, default skills would be excluded

        if (excludeDefaultSkills.equalsIgnoreCase("true")) {
            exclude_default_skills = true;
        }

        DAO.observe(); // get a database update

        SusiThought recall = null;
        if (dream == null || dream.length() == 0 || persona == null || persona.length() == 0) {
            // compute a recall
            SusiArgument observation_argument = new SusiArgument();
            List<SusiCognition> cognitions = DAO.susi_memory.getCognitions(user.getIdentity().getClient(), true);
            cognitions.forEach(cognition -> observation_argument.think(cognition.recallDispute()));
            recall = observation_argument.mindmeld(false);

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
        }

        // we create a hierarchy of minds which overlap each other completely. The first element in the array is the 'most conscious' mind.
        List<SusiMind> minds = new ArrayList<>();

        // instant dreams
        if (instant != null && instant.length() > 0) try {
            instant = instant.replaceAll("\\\\n", "\n"); // yes, the number of "\" is correct
            // fill an empty mind with the skilltext
            SusiMind instantMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
            JSONObject rules = SusiSkill.readLoTSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(instant.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), SusiLanguage.unknown, "instant", true);
            File origin = new File("file://instant");
            instantMind.learn(rules, origin, true);
            SusiSkill.ID skillid = new SusiSkill.ID(origin);
            SusiSkill activeskill = instantMind.getSkillMetadata().get(skillid);
            instantMind.setActiveSkill(activeskill);
            minds.add(instantMind);
        } catch (JSONException e) {
            DAO.severe(e.getMessage(), e);
        }

        // etherpad dreaming
        if (dream != null && dream.length() > 0) try {
            // read the pad for the dream
            String etherpadApikey = DAO.getConfig("etherpad.apikey", "");
            String etherpadUrlstub = DAO.getConfig("etherpad.urlstub", "");
            String padurl = etherpadUrlstub + "/api/1/getText?apikey=" + etherpadApikey + "&padID=$query$";
            JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(ConsoleService.loadData(padurl, dream)));
            JSONObject json = new JSONObject(serviceResponse);
            String text = json.getJSONObject("data").getString("text");
            // in case that the text contains a "*" we are in danger that we cannot stop dreaming, therefore we simply add the stop rule here to the text
            text = text + "\n\nwake up|stop dream|stop dreaming|end dream|end dreaming\ndreaming disabled^^>_etherpad_dream\n\n";
            text = text + "\n\ndream *\nI am currently dreaming $_etherpad_dream$, first wake up before dreaming again\n\n";
            // fill an empty mind with the dream
            SusiMind dreamMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
            JSONObject rules = SusiSkill.readLoTSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), SusiLanguage.unknown, dream, true);
            File origin = new File("file://" + dream);
            dreamMind.learn(rules, origin, true);
            SusiSkill.ID skillid = new SusiSkill.ID(origin);
            SusiSkill activeskill = dreamMind.getSkillMetadata().get(skillid);
            dreamMind.setActiveSkill(activeskill);
            // susi is now dreaming.. Try to find an answer out of the dream
            minds.add(dreamMind);
        } catch (JSONException | IOException e) {
            DAO.severe(e.getMessage(), e);
        }
        
        // on-skills: if a user has switched on a skill with "run skill" of a skill which has the "on"-property
        if (focus != null && focus.length() > 0) try {
            
            SusiMind focusMind = new SusiMind(DAO.susi_memory);
            JSONObject focus_skill = DAO.susi.getFocusSkill(focus);
            String originpath = focus_skill.getString("origin");
            focusMind.learn(focus_skill, new File(originpath), true);
            minds.add(focusMind);
        } catch (JSONException e) {
            DAO.severe(e.getMessage(), e);
        }
        
        // a persona or a private skill
        if ((persona != null && persona.length() > 0) || (privateSkill != null && userId.length() > 0 && group_name.length() > 0 && language.length() > 0 && skill_name.length() > 0)) {
            File skillfile = null;
            if (persona != null && persona.length() > 0) {
              skillfile = DAO.getSkillFileInModel(new File(DAO.model_watch_dir, "persona"), persona);
            }
            else {
              // read the private skill
              File private_skill_dir = new File(DAO.private_skill_watch_dir, userId);
              File group_file = new File(private_skill_dir, group_name);
              File language_file = new File(group_file, language);
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
                            Boolean allowed_site = true;
                            JSONObject configureName = skillName.getJSONObject("configure");
                            if (configureName.getBoolean("enable_default_skills") == false) {
                                exclude_default_skills = true;
                            }
                            HttpServletRequest request = post.getRequest();
                            String referer = request.getHeader("Referer");
                            if (DAO.allowDomainForChatbot(configureName, referer) == false) {
                                JSONObject json = new JSONObject(true);
                                json.put("accepted", false);
                                json.put("message", "Not allowed to use on this domain");
                                return new ServiceResponse(json);
                            }
                        }
                    }
                }
            }
            }
            // read the persona
            if (skillfile != null) try {
                String text = new String(Files.readAllBytes(skillfile.toPath()), StandardCharsets.UTF_8);
                // in case that the text contains a "*" we are in danger that we cannot sleep again, therefore we simply add the stop rule here to the text
                text = text + "\n\n* conscious mode|* conscious|conscious *\n$1$>_persona_awake is now conscious\n\nsleep|forget yourself|no yourself|unconscious|unconscious mode|That's enough|* dreamless slumber|Freeze|Cease * functions\nPersona will sleep now. Unconscious state activated.^^>_persona_awake\n\n";

                // fill an empty mind with the dream
                SusiMind awakeMind = new SusiMind(DAO.susi_memory); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
                JSONObject rules = SusiSkill.readLoTSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)), SusiLanguage.unknown, skillfile.getAbsolutePath(), false);
                awakeMind.learn(rules, skillfile, true);
                SusiSkill.ID skillid = new SusiSkill.ID(skillfile);
                SusiSkill activeskill = awakeMind.getSkillMetadata().get(skillid);
                awakeMind.setActiveSkill(activeskill);
                // we are awake!
                minds.add(awakeMind);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        
        // finally add the general mind definition. It's there if no other mind is conscious or the other minds do not find an answer.
        if (exclude_default_skills == false) {
            minds.add(DAO.susi);
        }

        // answer with built-in intents
        SusiCognition cognition = new SusiCognition(q, timezoneOffset, latitude, longitude, countryCode, countryName, language, deviceType, count, user.getIdentity(), minds.toArray(new SusiMind[minds.size()]));
        if (cognition.getAnswers().size() > 0) try {
            DAO.susi_memory.addCognition(user.getIdentity().getClient(), cognition, true);
        } catch (IOException e) {
            DAO.severe(e.getMessage());
        }
        JSONObject json = cognition.getJSON();
        return new ServiceResponse(json);
    }

}
