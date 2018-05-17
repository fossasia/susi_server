/**
 *  DisableSkillService
 *  Copyright by saurabh on 21/8/17.
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
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Accounting;
import ai.susi.server.Authorization;
import ai.susi.server.UserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

import static ai.susi.DAO.severe;

/**
 * Servlet to disable a skill in cms
 * this service accepts 4 parameter, model ,group, language and skill
 * test locally at http://127.0.0.1:4000/cms/disableSkill.json
 */
public class DisableSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1462779958799939359L;

    @Override
    public String getAPIPath() {
        return "/cms/disableSkill.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

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
        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Cannot disable the skill, ensure you are logged in");
        } else {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            if (!accounting.getJSON().has("disabledSkills"))
                accounting.getJSON().put("disabledSkills", new JSONObject());

            JSONObject disableSkills = accounting.getJSON().getJSONObject("disabledSkills");
            JSONObject modelName = new JSONObject();
            JSONObject groupName = new JSONObject();
            JSONArray languageName = new JSONArray();
            if (disableSkills.has(model_name)) {
                modelName = disableSkills.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        languageName = groupName.getJSONArray(language_name);
                       for(int i=0;i<languageName.length();i++) {
                           if (skill_name.equals(languageName.getString(i))) {
                               result.put("accepted", false);
                               result.put("message", "Skill already disabled for user");
                               return new ServiceResponse(result);
                           }
                       }
                    }
                }
            }
            languageName.put(skill_name);
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            disableSkills.put(model_name, modelName);
            result.put("accepted", true);
            result.put("message", "Skill Disabled");
            return new ServiceResponse(result);

        }
    }

}
