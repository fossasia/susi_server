/**
 *  GetSkillRatingsService
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

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;


/**
 * This Endpoint accepts 4 parameters. model,group,language,skill
 * before getting the feedack of a skill, the skill must exist in the directory.
 * http://localhost:4000/cms/getSkillFeedback.json?model=general&group=Knowledge&language=en&skill=who
 */
public class GetSkillFeedbackService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

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
        return "/cms/getSkillFeedback.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = SusiSkill.getSkillFileInLanguage(language, skill_name, false);

        JSONObject result = new JSONObject();

        if (!skill.exists()) {
            throw new APIException(422, "Skill does not exist.");
        }

        JsonTray feedbackSkill = DAO.feedbackSkill;
        JSONArray feedbackList = new JSONArray();

        if (feedbackSkill.has(model_name)) {
            JSONObject modelName = feedbackSkill.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                JSONObject groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    JSONObject  languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        feedbackList = languageName.getJSONArray(skill_name);
                        
                        result.put("skill_name", skill_name);
                        result.put("feedback", feedbackList);
                        result.put("accepted", true);
                        result.put("message", "Skill feedback fetched");
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        result.put("skill_name", skill_name);
        result.put("feedback", feedbackList);
        result.put("accepted", false);
        result.put("message", "Skill hasn't been given any feedback");
        return new ServiceResponse(result);
    }
}
