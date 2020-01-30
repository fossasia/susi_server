package ai.susi.server.api.cms;

/**
* GetReportSkillService
* Copyright by Shubham Gupta, @fragm3
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program in the file lgpl21.txt
* If not, see <http://www.gnu.org/licenses/>.
*/

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Necessary parameters : access_token, example:
 * http://localhost:4000/cms/getReportSkill.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 * Other parameter, (not necessary) search:
 * http://localhost:4000/cms/getReportSkill.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3&search=EnglishAIMLL
 */

public class GetReportSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.OPERATOR;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getReportSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject();

        if (authorization.getIdentity() == null) {
            throw new APIException(422, "Bad access token.");
        }
        List<JSONObject> reportList = new ArrayList<JSONObject>();

        JsonTray reportedSkills = DAO.reportedSkills;
        JSONObject reportedSkillsListObj = reportedSkills.toJSON();
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        JSONArray reports = new JSONArray();

        for (String key : JSONObject.getNames(reportedSkillsListObj)) {
            modelName = reportedSkillsListObj.getJSONObject(key);
            if (reportedSkillsListObj.has(key)) {
                for (String group_name : JSONObject.getNames(modelName)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (modelName.has(group_name)) {
                        for (String language_name : JSONObject.getNames(groupName)) {
                            languageName = groupName.getJSONObject(language_name);
                            if (groupName.has(language_name)) {
                                if (call.get("search", null) != null) {
                                    String skill_name = call.get("search", null);
                                    if (languageName.has(skill_name)) {
                                        JSONObject skillMetadata = DAO.susi.getSkillMetadata(key, group_name,
                                                language_name, skill_name);
                                        skillName = languageName.getJSONObject(skill_name);
                                        reports = skillName.getJSONArray("reports");
                                        List<JSONObject> updatedReportList = new ArrayList<JSONObject>();
                                        JSONObject skillObj = new JSONObject();
                                        for (int i = 0; i < reports.length(); i++) {
                                            JSONObject reportObject = new JSONObject();
                                            reportObject.put("feedback", reports.getJSONObject(i).get("feedback"));
                                            reportObject.put("email", reports.getJSONObject(i).get("email"));
                                            updatedReportList.add(reportObject);
                                        }
                                        skillObj.put("skill_name", skill_name);
                                        skillObj.put("group", group_name);
                                        skillObj.put("language", language_name);
                                        skillObj.put("model", key);
                                        skillObj.put("editable", skillMetadata.getBoolean("editable"));
                                        skillObj.put("reviewed", skillMetadata.getBoolean("reviewed"));
                                        skillObj.put("author", skillMetadata.get("author"));
                                        skillObj.put("type", "Public");
                                        skillObj.put("reports", updatedReportList);
                                        reportList.add(skillObj);
                                    }
                                } else {
                                    for (String skill_name : JSONObject.getNames(languageName)) {
                                        skillName = languageName.getJSONObject(skill_name);
                                        if (languageName.has(skill_name)) {
                                            JSONObject skillMetadata = DAO.susi.getSkillMetadata(key, group_name,
                                                    language_name, skill_name);
                                            JSONObject skillObj = new JSONObject();
                                            reports = skillName.getJSONArray("reports");
                                            List<JSONObject> updatedReportList = new ArrayList<JSONObject>();
                                            for (int i = 0; i < reports.length(); i++) {
                                                JSONObject reportObject = new JSONObject();
                                                reportObject.put("feedback", reports.getJSONObject(i).get("feedback"));
                                                reportObject.put("email", reports.getJSONObject(i).get("email"));
                                                updatedReportList.add(reportObject);
                                            }
                                            skillObj.put("skill_name", skill_name);
                                            skillObj.put("group", group_name);
                                            skillObj.put("language", language_name);
                                            skillObj.put("model", key);
                                            skillObj.put("editable", skillMetadata.getBoolean("editable"));
                                            skillObj.put("reviewed", skillMetadata.getBoolean("reviewed"));
                                            skillObj.put("author", skillMetadata.get("author"));
                                            skillObj.put("type", "Public");
                                            skillObj.put("reports", updatedReportList);
                                            reportList.add(skillObj);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        try {
            result.put("list", reportList);
            result.put("accepted", true);
            result.put("message", "Success: Fetched all Reported Skills");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed to fetch the requested list!");
        }

    }
}