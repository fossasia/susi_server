package ai.susi.server.api.cms;

/**
 *  UpdateSupportedLanguages
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
import java.io.File;

/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,rating.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/updateSupportedLanguages.json?model=general&group=Knowledge&language=en&skill=aboutsusi&new_language=hi&new_skill=susi_ke_bare_me
 */
public class UpdateSupportedLanguages extends AbstractAPIHandler implements APIHandler {


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
        return "/cms/updateSupportedLanguages.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call);
        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();
        File skill = skillQuery.getSkillFile();

        SkillQuery newSkillQuery = skillQuery
                .language(call.get("new_language", null))
                .skill(call.get("new_skill", null));
        String new_language_name = newSkillQuery.getLanguage();
        String new_skill_name = newSkillQuery.getSkill();
        File new_skill = newSkillQuery.getSkillFile();

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (!skill.exists() || !new_skill.exists()) {
            result.put("message", "skill does not exist");
            return new ServiceResponse(result);
        }

        JsonTray skillRating = DAO.skillSupportedLanguages;
        JSONObject modelName = new JSONObject();
        JSONArray groupName = new JSONArray();
        JSONArray languageNames = new JSONArray();
        Boolean alreadyExixts = false;
        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONArray(group_name);
                for (int i = 0; i < groupName.length(); i++) {
                    JSONArray supportedLanguages = groupName.getJSONArray(i);
                    for (int j = 0; j < supportedLanguages.length(); j++) {
                        JSONObject languageObject = supportedLanguages.getJSONObject(j);
                        String supportedLanguage = languageObject.get("language").toString();
                        String skillName = languageObject.get("name").toString();
                        if (supportedLanguage.equalsIgnoreCase(language_name) && skillName.equalsIgnoreCase(skill_name)) {

                            for (int k = 0; k < supportedLanguages.length(); k++) {
                                JSONObject tempLanguageObject = supportedLanguages.getJSONObject(k);
                                String tempSupportedLanguage = tempLanguageObject.get("language").toString();
                                String tempSkillName = tempLanguageObject.get("name").toString();
                                if (tempSupportedLanguage.equals(new_language_name) && tempSkillName.equals(new_skill_name)) {
                                    result.put("message", "Language already added");
                                    return new ServiceResponse(result);
                                }
                            }

                            JSONObject newLanguageObject = new JSONObject();
                            newLanguageObject.put("language", new_language_name);
                            newLanguageObject.put("name", new_skill_name);
                            supportedLanguages.put(newLanguageObject);
                            groupName.put(i, supportedLanguages);
                            alreadyExixts = true;
                        }
                    }
                }
            }
        }

        if (!alreadyExixts) {
            groupName.put(createSupportedLanguagesArray(language_name, skill_name, new_language_name, new_skill_name));
        }
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        result.put("language", new_language_name);
        result.put("name", new_skill_name);
        result.put("accepted", true);
        result.put("message", "New supported language added");
        return new ServiceResponse(result);

    }

    /* Utility function*/
    public JSONArray createSupportedLanguagesArray(String language_name, String skill_name, String new_language_name, String new_skill_name) {
        JSONArray supportedLanguages =  new JSONArray();

        JSONObject languageObject = new JSONObject();
        languageObject.put("language", language_name);
        languageObject.put("name", skill_name);
        supportedLanguages.put(languageObject);

        JSONObject newLanguageObject = new JSONObject();
        newLanguageObject.put("language", new_language_name);
        newLanguageObject.put("name", new_skill_name);
        supportedLanguages.put(newLanguageObject);

        return supportedLanguages;
    }
}
