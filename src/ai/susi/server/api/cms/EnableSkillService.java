/**
 *  EnableSkillService
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

import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to enable a skill in cms
 * this service accepts 4 parameter, model ,group, language and skill
 * test locally at http://127.0.0.1:4000/cms/enableSkill.json
 */
public class EnableSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -1462779958799939359L;

    @Override
    public String getAPIPath() {
        return "/cms/enableSkill.json";
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

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Cannot enable skill, ensure you are logged in");
        } else {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            if (!accounting.getJSON().has("disabledSkills")) {
                accounting.getJSON().put("disabledSkills", new JSONObject());
                accounting.commit();
            }

            JSONObject disableSkills = accounting.getJSON().getJSONObject("disabledSkills");
            if (disableSkills.has(model_name)) {
             JSONObject   modelName = disableSkills.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    JSONObject groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                       JSONArray languageName = groupName.getJSONArray(language_name);
                        for(int i=0;i<languageName.length();i++) {
                            if (skill_name.equals(languageName.getString(i))) {
                                languageName.remove(i);
                                result.put("accepted", true);
                                result.put("message", "Skill Enabled for user");
                                return new ServiceResponse(result);
                            }
                        }
                    }
                }
            }

            result.put("accepted", false);
            result.put("message", "Skill not disabled for user");
            return new ServiceResponse(result);

        }
    }

}
