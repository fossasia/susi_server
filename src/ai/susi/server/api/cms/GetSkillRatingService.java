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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;


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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = new File(language, skill_name + ".txt");

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (!skill.exists()) {
            result.put("message", "skill does not exist");
            return new ServiceResponse(result);

        }
        JsonTray skillRating = DAO.skillRating;
        if (skillRating.has(model_name)) {
            JSONObject modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                JSONObject groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    JSONObject  languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONObject skillName = languageName.getJSONObject(skill_name);
                        result.put("skill_name", skill_name);
                        result.put("skill_rating", skillName);
                        result.put("accepted", true);
                        result.put("message", "Skill ratings fetched");
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        result.put("accepted", false);
        result.put("message", "Skill has not been rated yet");
        return new ServiceResponse(result);
    }
}