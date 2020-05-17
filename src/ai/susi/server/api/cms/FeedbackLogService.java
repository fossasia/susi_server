package ai.susi.server.api.cms;

/**
 *  FeedbackLogService
 *  Copyright by Anup Kumar Panwar, @AnupKumarPanwar
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

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;


/**
 * This Endpoint accepts 8 parameters. model,group,language,skill,rating,query,reply,country_name,country_code.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/feedbackLog.json
 */
public class FeedbackLogService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 7947060716231250102L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/feedbackLog.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();
        String skill_rate = call.get("rating", null);
        String user_query = call.get("user_query", null);
        String susi_reply = call.get("susi_reply", null);
        String country_name = call.get("country_name", null);
        String country_code = call.get("country_code", null);
        String device_type = call.get("device_type", "Others");
        String skill_path = SusiSkill.SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA + "/models/" + model_name + "/" + group_name + "/" + language_name + "/" + skill_name + ".txt";


        JSONObject result = new JSONObject();
        result.put("accepted", false);
        JsonTray skillRating = DAO.feedbackLogs;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray skillName = new JSONArray();
        JSONObject feedbackLogObject = new JSONObject();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONArray(skill_name);
                        feedbackLogObject.put("timestamp", timestamp);
                        feedbackLogObject.put("feedback", skill_rate);
                        feedbackLogObject.put("user_query", user_query);
                        feedbackLogObject.put("susi_reply", susi_reply);
                        feedbackLogObject.put("country_name", country_name);
                        feedbackLogObject.put("country_code", country_code);
                        feedbackLogObject.put("device_type", device_type);
                        feedbackLogObject.put("skill_path", skill_path);
                        skillName.put(feedbackLogObject);
                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        skillRating.put(model_name, modelName, true);
                        result.put("accepted", true);
                        result.put("message", "Feedback log updated");
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        languageName.put(skill_name, createFeedbackLogObject(skill_rate, user_query, susi_reply, country_name, country_code, device_type, skill_path));
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        result.put("accepted", true);
        result.put("message", "Feedback log updated");
        return new ServiceResponse(result);

    }

    /* Utility function*/
    public JSONArray createFeedbackLogObject(String skill_rate, String user_query, String susi_reply, String country_name, String country_code, String device_type, String skill_path) {
        JSONArray skillName = new JSONArray();
        JSONObject feedbackLogObject = new JSONObject();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        feedbackLogObject.put("timestamp", timestamp);
        feedbackLogObject.put("feedback", skill_rate);
        feedbackLogObject.put("user_query", user_query);
        feedbackLogObject.put("susi_reply", susi_reply);
        feedbackLogObject.put("country_name", country_name);
        feedbackLogObject.put("country_code", country_code);
        feedbackLogObject.put("device_type", device_type);
        skillName.put(feedbackLogObject);
        return skillName;
    }
}
