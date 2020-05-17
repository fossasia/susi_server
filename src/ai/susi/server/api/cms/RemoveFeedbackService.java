package ai.susi.server.api.cms;

/**
 *  RemoveFeedbackService
 *  Copyright by Akshat Garg, @akshatnitd
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
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,access_token
 * http://localhost:4000/cms/removeFeedback.json?model=general&group=Knowledge&language=en&skill=aboutsusi&&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class RemoveFeedbackService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -5131270951807551092L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/removeFeedback.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JSONObject result = new JSONObject();

        if (!authorization.getIdentity().isAnonymous()) {
        	String idvalue = authorization.getIdentity().getName(); // Get id from the access_token

            JsonTray feedbackSkill = DAO.feedbackSkill;
            JSONObject modelName = new JSONObject();
            JSONObject groupName = new JSONObject();
            JSONObject languageName = new JSONObject();
            JSONArray skillName = new JSONArray();

            Boolean feedbackUpdated = false;

            if (feedbackSkill.has(model_name)) {
                modelName = feedbackSkill.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        languageName = groupName.getJSONObject(language_name);
                        if (languageName.has(skill_name)) {
                            skillName = languageName.getJSONArray(skill_name);
                            JSONObject feedbackObject = new JSONObject();

                            for (int i = 0; i < skillName.length(); i++) {
                                feedbackObject = skillName.getJSONObject(i);
                                if ((authorization.getIdentity().isEmail() && feedbackObject.get("email").equals(idvalue)) ||
                                    (authorization.getIdentity().isUuid() && feedbackObject.get("uuid").equals(idvalue))) {
                                    skillName.remove(i);
                                    updateSkillRatingJSON(skillQuery);
                                    feedbackUpdated = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (!feedbackUpdated ) {
                result.put("message", "Skill feedback missing");
            } else {
                result.put("message", "Skill feedback updated");
            }

            languageName.put(skill_name, skillName);
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            feedbackSkill.put(model_name, modelName, true);
            result.put("accepted", true);
            return new ServiceResponse(result);
        } else {
            throw new APIException(422, "Access token not given.");
        }
    }

    // Reduce feedback_count from  skill_rating object to the skillRatingJSON
    private void updateSkillRatingJSON(SkillQuery skillQuery) {
        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JsonTray skillRating = DAO.skillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                    }
                }
            }
        }

        if (skillName.has("feedback_count")) {
            int skillFeedback = skillName.getInt("feedback_count");
            skillName.put("feedback_count", skillFeedback - 1 );
        } 

        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        return;
    }

}
