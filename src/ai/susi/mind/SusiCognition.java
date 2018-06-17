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

    JSONObject json;

    public SusiCognition(
            final String query,
            int timezoneOffset,
            double latitude, double longitude,
            String countryCode, String countryName,
            String languageName,
            int maxcount, ClientIdentity identity,
            final SusiMind... minds) {
        this.json = new JSONObject(true);
        
        // get a response from susis mind
        String client = identity.getClient();
        this.setQuery(query);
        this.json.put("count", maxcount);
        SusiThought observation = new SusiThought();
        observation.addObservation("timezoneOffset", Integer.toString(timezoneOffset));
        
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            observation.addObservation("latitude", Double.toString(latitude));
            observation.addObservation("longitude", Double.toString(longitude));
        }

        
        SusiLanguage language = SusiLanguage.parse(languageName);
        if (language != SusiLanguage.unknown) observation.addObservation("language", language.name());
        
        this.json.put("client_id", Base64.getEncoder().encodeToString(client.getBytes(StandardCharsets.UTF_8)));
        long query_date = System.currentTimeMillis();
        this.json.put("query_date", DateParser.utcFormatter.print(query_date));
        
        // compute the mind's reaction: here we compute with a hierarchy of minds. The dispute is taken from the relevant mind level that was able to compute the dispute
        List<SusiThought> dispute = SusiMind.reactMinds(query, language, maxcount, client, observation, minds);
        long answer_date = System.currentTimeMillis();

        // update country wise skill usage data
        if (!countryCode.equals("") && !countryName.equals("")) {
            try {
                List<String> skills = dispute.get(0).getSkills();
                for (String skill : skills) {
                    updateCountryWiseUsageData(skill, countryCode, countryName);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        // update skill usage data
        try {
            List<String> skills = dispute.get(0).getSkills();
            for (String skill : skills) {
                updateUsageData(skill);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        // store answer and actions into json
        this.json.put("answers", new JSONArray(dispute));
        this.json.put("answer_date", DateParser.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
        this.json.put("language", language.name());
    }

    private void updateCountryWiseUsageData(String skillPath, String countryCode, String countryName) {
        String skillInfo[] = skillPath.split("/");
        String model_name = skillInfo[3];
        String group_name = skillInfo[4];
        String language_name = skillInfo[5];
        String skill_name = skillInfo[6].split("\\.")[0];
        JsonTray skillUsage = DAO.countryWiseSkillUsage;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray countryWiseUsageData =new JSONArray();
        JSONObject countryUsage = new JSONObject();
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
        JSONObject dayUsage = new JSONObject();
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
            dayUsage.put("date", today);
            dayUsage.put("count", "1");
            usageData.put(dayUsage);
        }
        languageName.put(skill_name, usageData);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillUsage.put(model_name, modelName, true);
        return;
    }

    public SusiCognition(JSONObject json) {
        this.json = json;
    }
    
    public void appendToFile(File f) throws IOException {
        try {
            Files.write(f.toPath(), (this.getJSON().toString(0) + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
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
    public String getExpression() {
        List<SusiThought> answers = getAnswers();
        if (answers == null || answers.size() == 0) return "";
        SusiThought t = answers.get(0);
        List<SusiAction> actions = t.getActions();
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
                for (SusiAction a: clonedThought.getActions()) {
                    ArrayList<String> phrases = a.getPhrases();
                    // not all actions have phrases!
                    if (phrases != null && phrases.size() > 0) {expressionAction = a; break;}
                }
                if (expressionAction != null) dispute.addObservation("answer", expressionAction.getPhrases().get(0)); // we can unify with "answer" in queries
                // the skill, can be used to analyze the latest answer
                List<String> skills = clonedThought.getSkills();
                if (skills.size() > 0) {
                    if(skills.get(0).startsWith("/susi_server/file:")) {
                        dispute.addObservation("skill", "Etherpad Dream: " +skills.get(0).substring("/susi_server/file:/".length()));
                    } else {
                        dispute.addObservation("skill", skills.get(0));
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

    public String getSkillLink(String skillPath) {
        String link=skillPath;
        if(skillPath.startsWith("/susi_server")) {
            if(skillPath.startsWith("/susi_server/file:")) {
                link = "http://dream.susi.ai/p/" + skillPath.substring("/susi_server/file:/".length());
            } else {
                link ="https://github.com/fossasia/susi_server/blob/development" + skillPath.substring("/susi_server".length());
            }
        } else if (skillPath.startsWith("/susi_skill_data")) {
            link = "https://github.com/fossasia/susi_skill_data/blob/master" + skillPath.substring("/susi_skill_data".length());
        }
        return link;
    }
}
