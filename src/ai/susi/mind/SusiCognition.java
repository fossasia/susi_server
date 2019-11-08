/**
 *  SusiCognition
 *  Copyright 24.07.2016 by Michael Peter Christen, @0rb1t3r
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import ai.susi.json.JsonTray;
import com.google.common.base.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.server.ClientIdentity;
import ai.susi.tools.DateParser;

/**
 * An cognition is the combination of a query of a user with the response of susi.
 */
public class SusiCognition {

    private JSONObject json;

    public SusiCognition(JSONObject json) {
        this.json = json;
    }

    /**
     * Compute a cognition for a given query string.
     * The cognition contains a reaction on the query in the form of a dispute and actions
     * that should be taken as a result of the disputes actions.
     * 
     * The cognition is computed by first creating an observation thought which is feeded
     * into the mind layers to create a dispute that can be deduced from the given observation
     * and the history of observations a taken from the users log.
     * The mind layers are order from the most priorized one (the first one in the list) to
     * the least priorized one. 
     * The computed dispute may not only be computed by one single mind layer but by all of them
     * together. That may happen if intents in one layer uses reflection to call support from
     * another skill which may be embedded into another mind layer. The content of the layers
     * are constructed in such a way that they create a knowledge funnel, creating abstractions
     * on the higher levels and computing memory functions on the lower levels.
     * 
     * @param query the main observation that may change while all other context observations may be constant
     * @param timezoneOffset context observation, an offset to GMT as number of minutes
     * @param latitude context observation
     * @param longitude context observation
     * @param countryCode context observation, two letter code
     * @param countryName context observation, full name of a country
     * @param languageCode context observation, two letter code of the language
     * @param deviceType context observation
     * @param maxcount the maximum number of reactions, usually 1
     * @param identity context observation, the identity of the user. This is important to retrieve the users memory of past conversations.
     * @param mindLayers the different minds that shall be used, most prominent one as first in the list
     */
    public SusiCognition(
            final String query,
            int timezoneOffset,
            double latitude, double longitude,
            String countryCode, String countryName,
            String languageCode,
            String deviceType,
            ClientIdentity identity,
            boolean debug,
            final SusiMind... mindLayers) {
        this.json = new JSONObject(true);

        // get a response from susis mind
        String client = identity.getClient();
        this.setQuery(query);
        SusiThought observation = new SusiThought();
        observation.addObservation("timezoneOffset", Integer.toString(timezoneOffset));

        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            observation.addObservation("latitude", Double.toString(latitude));
            observation.addObservation("longitude", Double.toString(longitude));
        }

        if (!Strings.isNullOrEmpty(countryName) && !Strings.isNullOrEmpty(countryCode)) {
            observation.addObservation("country_name", countryName);
            observation.addObservation("country_code", countryCode);
        }

        SusiLanguage language = SusiLanguage.parse(languageCode);
        assert language != SusiLanguage.unknown; // we should always know a language
        if (language != SusiLanguage.unknown) observation.addObservation("language", language.name());

        long query_date = System.currentTimeMillis();

        // compute the mind's reaction: here we compute with a hierarchy of minds. The dispute is taken from the relevant mind level that was able to compute the dispute
        SusiThought dispute = SusiMind.reactMinds(query, language, identity, debug, observation, mindLayers);
        long answer_date = System.currentTimeMillis();

        // update country wise skill usage data
        if (!countryCode.equals("") && !countryName.equals("") && dispute != null) {
            List<String> skills = dispute.getSkills();
            for (String skill : skills) {
                try {
                    updateCountryWiseUsageData(skill, countryCode, countryName);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // update skill usage data
        if (dispute != null) try {
            List<String> skills = dispute.getSkills();
            for (String skill : skills) {
                updateUsageData(skill);
                updateDeviceWiseUsageData(skill, deviceType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // store answer and actions into json
        if (dispute != null) {
            List<SusiThought> thoughts = new ArrayList<>();
            thoughts.add(dispute);
            this.json.put("answers", new JSONArray(thoughts));
        }
        this.json.put("query_date", DateParser.utcFormatter.print(query_date));
        this.json.put("query_language", language.name());
        this.json.put("answer_date", DateParser.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
        this.json.put("client_id", Base64.getEncoder().encodeToString(client.getBytes(StandardCharsets.UTF_8)));

    }

    public void updateDeviceWiseUsageData(String skillPath, String deviceType) {
        String skillInfo[] = skillPath.split("/");
        if (skillInfo.length < 6) return;
        String model_name = skillInfo[3];
        String group_name = skillInfo[4];
        String language_name = skillInfo[5];
        String skill_name = skillInfo[6].split("\\.")[0];
        JsonTray skillUsage = DAO.deviceWiseSkillUsage;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray deviceWiseUsageData =new JSONArray();
        Boolean deviceExists = false;
        if (skillUsage.has(model_name)) {
            modelName = skillUsage.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {

                        deviceWiseUsageData = languageName.getJSONArray(skill_name);

                        for (int i = 0; i < deviceWiseUsageData.length(); i++) {
                            JSONObject deviceUsage = new JSONObject();
                            deviceUsage = deviceWiseUsageData.getJSONObject(i);
                            if (deviceUsage.get("device_type").equals(deviceType)) {
                                deviceUsage.put("count", deviceUsage.getInt("count") + 1);
                                deviceWiseUsageData.put(i,deviceUsage);
                                deviceExists = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!deviceExists) {
            JSONObject deviceUsage = new JSONObject();
            deviceUsage.put("device_type", deviceType);
            deviceUsage.put("count", 1);
            deviceWiseUsageData.put(deviceUsage);
        }

        languageName.put(skill_name, deviceWiseUsageData);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillUsage.put(model_name, modelName, true);
    }

    public void updateCountryWiseUsageData(String skillPath, String countryCode, String countryName) {
        String skillInfo[] = skillPath.split("/");
        if (skillInfo.length < 6) return;
        String model_name = skillInfo[3];
        String group_name = skillInfo[4];
        String language_name = skillInfo[5];
        String skill_name = skillInfo[6].split("\\.")[0];
        JsonTray skillUsage = DAO.countryWiseSkillUsage;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray countryWiseUsageData =new JSONArray();
        Boolean countryExists = false;
        if (skillUsage.has(model_name)) {
            modelName = skillUsage.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {

                        countryWiseUsageData = languageName.getJSONArray(skill_name);

                        for (int i = 0; i < countryWiseUsageData.length(); i++) {
                            JSONObject countryUsage = new JSONObject();
                            countryUsage = countryWiseUsageData.getJSONObject(i);
                            if (countryUsage.get("country_code").equals(countryCode)) {
                                countryUsage.put("count", countryUsage.getInt("count")+1);
                                countryWiseUsageData.put(i,countryUsage);
                                countryExists = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!countryExists) {
            JSONObject countryUsage = new JSONObject();
            countryUsage.put("country_code", countryCode);
            countryUsage.put("country_name", countryName);
            countryUsage.put("count", 1);
            countryWiseUsageData.put(countryUsage);
        }

        languageName.put(skill_name, countryWiseUsageData);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillUsage.put(model_name, modelName, true);
        return;
    }  
  
    public void updateUsageData(String skillPath) {
        String skillInfo[] = skillPath.split("/");
        if (skillInfo.length < 6) return;
        String model_name = skillInfo[3];
        String group_name = skillInfo[4];
        String language_name = skillInfo[5];
        String skill_name = skillInfo[6].split("\\.")[0];
        JsonTray skillUsage = DAO.skillUsage;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray usageData = new JSONArray();
        Boolean dateExists = false;
        String today = LocalDate.now().toString();
        if (skillUsage.has(model_name)) {
            modelName = skillUsage.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        usageData = languageName.getJSONArray(skill_name);
                        for (int i = 0; i<usageData.length(); i++) {
                            JSONObject dayUsage = new JSONObject();
                            dayUsage = usageData.getJSONObject(i);
                            if (dayUsage.get("date").equals(today)){
                                dayUsage.put("count", dayUsage.getInt("count")+1+"");
                                usageData.put(i,dayUsage);
                                dateExists = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!dateExists) {
            JSONObject dayUsage = new JSONObject();
            dayUsage.put("date", today);
            dayUsage.put("count", "1");
            usageData.put(dayUsage);
        }
        languageName.put(skill_name, usageData);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillUsage.put(model_name, modelName, true);
    }
    
    public void appendToFile(File f) throws IOException {
        try {
            Files.write(f.toPath(), (this.getJSON().toString(0) + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }
    
    public SusiCognition setIdentity(final String identity) {
        this.json.put("identity", identity);
        return this;
    }
    
    public SusiCognition setQuery(final String query) {
        this.json.put("query", query);
        return this;
    }
    
    public String getQuery() {
        if (!this.json.has("query")) return "";
        String q = this.json.getString("query");
        return q == null ? "" : q;
    }
    
    public Date getQueryDate() {
        String d = this.json.getString("query_date");
        return DateParser.utcFormatter.parseDateTime(d).toDate();
    }

    /**
     * The answer of an cognition contains a list of the mind-melted arguments as one thought
     * @return a list of answer thoughts
     */
    public List<SusiThought> getAnswers() {
        List<SusiThought> answers = new ArrayList<>();
        if (this.json.has("answers")) {
            JSONArray a = this.json.getJSONArray("answers");
            for (int i = 0; i < a.length(); i++) answers.add(new SusiThought(a.getJSONObject(i)));
        }
        return answers;
    }
    
    /**
     * the expression of a cognition is the actual string that comes out as response to
     * actions of the user
     * @return a response string
     */
    public String getExpression(final boolean ignoreWarnings) {
        List<SusiThought> answers = getAnswers();
        if (answers == null || answers.size() == 0) return "";
        SusiThought t = answers.get(0);
        List<SusiAction> actions = t.getActions(ignoreWarnings);
        if (actions == null || actions.size() == 0) return "";
        SusiAction a = actions.get(0);
        List<String> phrases = a.getPhrases();
        if (phrases == null || phrases.size() == 0) return "";
        return phrases.get(0);
    }

    /**
     * The cognition is the result of a though extraction. We can reconstruct
     * the dispute as list of last mindstates using the cognition data.
     * @return a backtrackable thought reconstructed from the cognition data
     */
    public SusiThought recallDispute() {
        SusiThought dispute = new SusiThought();
        if (this.json.has("answers")) {
            JSONArray answers = this.json.getJSONArray("answers"); // in most cases there is only one answer
            for (int i = answers.length() - 1; i >= 0; i--) {
                SusiThought clonedThought = new SusiThought(answers.getJSONObject(i));

                // add observations from metadata as variable content:
                // the query:
                dispute.addObservation("query", this.json.getString("query"));  // we can unify "query" in queries
                // the expression - that is an answer
                SusiAction expressionAction = null;
                for (SusiAction a: clonedThought.getActions(false)) {
                    ArrayList<String> phrases = a.getPhrases();
                    // not all actions have phrases!
                    if (phrases != null && phrases.size() > 0) {expressionAction = a; break;}
                }
                if (expressionAction != null) dispute.addObservation("answer", expressionAction.getPhrases().get(0)); // we can unify with "answer" in queries
                // the skill, can be used to analyze the latest answer
                List<String> skills = clonedThought.getSkills();
                if (skills.size() > 0) {
                    if(skills.get(0).startsWith(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER + "/file:")) {
                        dispute.addObservation("skill_source", "Etherpad Dream: " +skills.get(0).substring((SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER + "/file:").length()));
                    } else {
                        dispute.addObservation("skill_source", skills.get(0));
                    }
                    dispute.addObservation("skill_link", getSkillLink(skills.get(0)));
                }

                // add all data from the old dispute
                JSONArray clonedData = clonedThought.getData();
                if (clonedData.length() > 0) {
                    JSONObject row = clonedData.getJSONObject(0);
                    row.keySet().forEach(key -> {
                        if (key.startsWith("_")) {
                            try {
                                String observation = row.getString(key);
                                dispute.addObservation(key, observation);
                            } catch (JSONException e) {
                                // sometimes that is not string
                                DAO.log(e.getMessage());
                            }
                        }
                    });
                    //data.put(clonedData.get(0));
                }
            }
        }
        return dispute;
    }

    public JSONObject getJSON() {
        return this.json;
    }

    public String toString() {
        return this.json.toString();
    }

    public static String getSkillLink(String skillPath) {
        String link=skillPath;
        if(skillPath.startsWith(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER)) {
            if(skillPath.startsWith(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER + "/file:")) {
                link = "http://dream.susi.ai/p/" + skillPath.substring((SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER + "/file:").length());
            } else {
                link ="https://github.com/fossasia/susi_server/blob/development" + skillPath.substring(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SERVER.length());
            }
        } else if (skillPath.startsWith(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA)) {
            link = "https://github.com/fossasia/susi_skill_data/blob/master" + skillPath.substring(SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA.length());
        }
        return link;
    }
}
