package ai.susi.server.api.cms;

/**
 *  RateSkillService
 *  Copyright by Saurabh Jain, @Saurabhjn76
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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,rating.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/rateSkill.json?model=general&group=Knowledge&skill=who&rating=positive
 */
public class RateSkillService extends AbstractAPIHandler implements APIHandler {


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
        return "/cms/rateSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();
        String skill_rate = call.get("rating", null);

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        JsonTray skillRating = DAO.skillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONObject skillName = languageName.getJSONObject(skill_name);
                        skillName.put(skill_rate, skillName.getInt(skill_rate) + 1 + "");
                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        skillRating.put(model_name, modelName, true);
                        result.put("accepted", true);
                        result.put("message", "Skill ratings updated");
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        languageName.put(skill_name, createRatingObject(skill_rate));
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        result.put("accepted", true);
        result.put("message", "Skill ratings added");
        return new ServiceResponse(result);

    }

    /* Utility function*/
    public JSONObject createRatingObject(String skill_rate) {
    	JSONObject skillName = new JSONObject();
        skillName.put("positive", "0");
        skillName.put("negative", "0");
        skillName.put("feedback_count", 0);
        skillName.put("bookmark_count", 0);

        JSONObject skillStars = new JSONObject();

        skillStars.put("one_star", 0);
        skillStars.put("two_star", 0);
        skillStars.put("three_star", 0);
        skillStars.put("four_star", 0);
        skillStars.put("five_star", 0);
        skillStars.put("avg_star", 0);
        skillStars.put("total_star", 0);

        skillName.put(skill_rate, skillName.getInt(skill_rate) + 1 + "");
        return skillName;
    }


}
