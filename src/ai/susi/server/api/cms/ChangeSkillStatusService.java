/**
 *  ChangeSkillStatusService
 *  Copyright by @Akshat-Jain on 10/07/18.
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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * This endpoint allows Admin and higher userroles to change review status of any skill
 * http://127.0.0.1:4000/cms/changeSkillStatus.json?model=general&group=Knowledge&language=en&skill=aboutsusi&reviewed=true&access_token=zdasIagg71NF9S2Wu060ZxrRdHeFAx
 */

public class ChangeSkillStatusService extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = 7926060917231250102L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/changeSkillStatus.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        String group_name = call.get("group", "All");
        String language_name = call.get("language", "en");
        String skill_name = call.get("skill", null);
        String reviewed = call.get("reviewed", null);

        if (authorization.getIdentity() == null) {
            throw new APIException(422, "Bad access token.");
        }

        if (skill_name == null || !(reviewed.equals("true") || reviewed.equals("false"))) {
            throw new APIException(400, "Bad service call, missing arguments.");
        }

        JSONObject result = new JSONObject();
        JsonTray skillStatus = DAO.skillStatus;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();

        if(reviewed.equals("true")) {
            JSONObject reviewStatus = new JSONObject();
            reviewStatus.put("reviewed", true);
            if (skillStatus.has(model_name)) {
                modelName = skillStatus.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        languageName = groupName.getJSONObject(language_name);
                        if (languageName.has(skill_name)) {
                            skillName = languageName.getJSONObject(skill_name);
                            skillName.put("reviewed", true);
                            result.put("accepted", true);
                            result.put("message", "Skill review status changed successfully.");
                            return new ServiceResponse(result);
                        }
                    }
                }
            }
            languageName.put(skill_name, reviewStatus);
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            skillStatus.put(model_name, modelName, true);
            result.put("accepted", true);
            result.put("message", "Skill review status changed successfully.");
            return new ServiceResponse(result);
            }

            else {
                if (skillStatus.has(model_name)) {
                    modelName = skillStatus.getJSONObject(model_name);
                    if (modelName.has(group_name)) {
                        groupName = modelName.getJSONObject(group_name);
                        if (groupName.has(language_name)) {
                            languageName = groupName.getJSONObject(language_name);
                            if (languageName.has(skill_name)) {
                                languageName.remove(skill_name);
                                if(languageName.length() == 0) {
                                    groupName.remove(language_name);
                                    if(groupName.length() == 0) {
                                        modelName.remove(group_name);
                                        if(modelName.length() == 0) {
                                            skillStatus.remove(model_name);
                                        }
                                    }
                                }
                                skillStatus.commit();
                            }
                        }
                    }
                }
                result.put("accepted", true);
                result.put("message", "Skill review status changed successfully.");
                return new ServiceResponse(result);
            }
        }
    }
