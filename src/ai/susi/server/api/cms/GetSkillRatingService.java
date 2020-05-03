/**
 *  GetSkillRatingsService
 *  Copyright by Chetan, @dyanmitechetan
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
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


/**
 * This Endpoint accepts 4 parameters. model,group,language,skill
 * before getting a rating of a skill, the skill must exist in the directory.
 * http://localhost:4000/cms/getSkillRating.json?model=general&group=Knowledge&skill=who
 */
public class GetSkillRatingService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 1420414106164188352L;

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
        return "/cms/getSkillRating.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JSONObject result = new JSONObject();

        JsonTray skillRating = DAO.skillRating;
        if (skillRating.has(model_name)) {
            JSONObject modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                JSONObject groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    JSONObject  languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONObject skillName = languageName.getJSONObject(skill_name);

                        DAO.putStars(skillName);
                        result.put("skill_name", skill_name);
                        result.put("skill_rating", skillName);
                        result.put("accepted", true);
                        result.put("message", "Skill ratings fetched");
                        return new ServiceResponse(result);
                    }
                }
            }
        }

        JSONObject tempSkillRating = new JSONObject();
        tempSkillRating.put("negative", "0");
        tempSkillRating.put("positive", "0");
        tempSkillRating.put("feedback_count", 0);
        tempSkillRating.put("bookmark_count", 0);

        JSONObject tempSkillStars=new JSONObject();
        tempSkillStars.put("one_star", 0);
        tempSkillStars.put("two_star", 0);
        tempSkillStars.put("three_star", 0);
        tempSkillStars.put("four_star", 0);
        tempSkillStars.put("five_star", 0);
        tempSkillStars.put("avg_star", 0);
        tempSkillStars.put("total_star", 0);

        tempSkillRating.put("stars", tempSkillStars);

        result.put("accepted", false);
        result.put("message", "Skill has not been rated yet");
        result.put("skill_rating", tempSkillRating);

        return new ServiceResponse(result);
    }
}
