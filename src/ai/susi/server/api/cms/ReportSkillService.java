package ai.susi.server.api.cms;

/**
 *  ReportSkillService
 *  Copyright by Anup Kumar Panwar, @anupkumarpanwar
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
import java.sql.Timestamp;



/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,feedback.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/reportSkill.json?model=general&group=Knowledge&skill=some_inappropriate_skill&feedback=report_message&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class ReportSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8950170351039942439L;


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
        return "/cms/reportSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        String skill_feedback = call.get("feedback", null);

        JSONObject result = new JSONObject();
        if (skill_feedback == null) {
            throw new APIException(422, "Feedback not provided.");
        }

        if (!authorization.getIdentity().isAnonymous()) {
            String idvalue = authorization.getIdentity().getName(); //Get email from the access_token

            JsonTray reportedSkills = DAO.reportedSkills;
            JSONObject modelName = new JSONObject();
            JSONObject groupName = new JSONObject();
            JSONObject languageName = new JSONObject();
            JSONObject skillName = new JSONObject();
            JSONArray reports = new JSONArray();
            int reportCount = 0;
            Boolean active = true; // status of skill if it has been removed or still active

            if (reportedSkills.has(model_name)) {
                modelName = reportedSkills.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        languageName = groupName.getJSONObject(language_name);
                        if (languageName.has(skill_name)) {
                            skillName = languageName.getJSONObject(skill_name);
                            reports = skillName.getJSONArray("reports");

                            for (int i = 0; i < reports.length(); i++) {
                                JSONObject reportObject = new JSONObject();
                                reportObject = reports.getJSONObject(i);
                                if ((authorization.getIdentity().isEmail() && reportObject.get("email").equals(idvalue)) ||
                                	(authorization.getIdentity().isUuid() && reportObject.get("uuid").equals(idvalue))) {
                                    throw new APIException(422, "Skill already reported by the user.");
                                }
                            }
                        }
                    }
                }
            }

            JSONObject reportObject = new JSONObject();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            if (authorization.getIdentity().isEmail()) reportObject.put("email", idvalue);
            if (authorization.getIdentity().isUuid()) reportObject.put("uuid", idvalue);
            reportObject.put("feedback", skill_feedback);
            reportObject.put("timestamp", timestamp.toString());
            reports.put(reportObject);
            skillName.put("reports", reports);
            try {
                reportCount = Integer.parseInt(skillName.get("count").toString());
                skillName.put("count", reportCount + 1);
            }
            catch (Exception e) {
                skillName.put("count", 1);
            }
            languageName.put(skill_name, skillName);
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            reportedSkills.put(model_name, modelName, true);
            result.put("accepted", true);
            result.put("message", "Skill reported successfully");
            result.put("feedback", skill_feedback);
            return new ServiceResponse(result);
        } else {
            throw new APIException(422, "Access token not given.");
        }
    }
}
