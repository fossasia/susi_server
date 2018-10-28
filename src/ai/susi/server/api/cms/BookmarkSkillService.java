package ai.susi.server.api.cms;

/**
 *  BookmarkSkillService
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

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.sql.Timestamp;


/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,bookmark.
 * bookmark can be 0 or 1
 * http://localhost:4000/cms/bookmarkSkill.json?model=general&group=Knowledge&skill=aboutsusi&bookmark=1&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class BookmarkSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -1960932190918215684L;

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
        return "/cms/bookmarkSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = DAO.getSkillFileInLanguage(language, skill_name, false);
        String user_bookmark = call.get("bookmark", null);
        Integer skill_bookmark;

        JSONObject result = new JSONObject();
        if (!skill.exists()) {
            throw new APIException(422, "Skill does not exist.");
        }

        if (user_bookmark == null) {
            throw new APIException(422, "Bookmark not provided.");
        }

        skill_bookmark = Integer.parseInt(user_bookmark);

        if(skill_bookmark != 0 && skill_bookmark != 1) {
            throw new APIException(422, "Invalid Bookmark provided.");
        }

        if (authorization.getIdentity().isEmail()) {
            String email = authorization.getIdentity().getName(); //Get email from the access_token

            JsonTray bookmarkSkill = DAO.bookmarkSkill;
            JSONObject userName = new JSONObject();
            JSONObject modelName = new JSONObject();
            JSONObject groupName = new JSONObject();
            JSONObject languageName = new JSONObject();

            Boolean bookmarkUpdated = false;

            if (bookmarkSkill.has(email)) {
                userName = bookmarkSkill.getJSONObject(email);
                if (userName.has(model_name)) {
                    modelName = userName.getJSONObject(model_name);
                    if (modelName.has(group_name)) {
                        groupName = modelName.getJSONObject(group_name);
                        if (groupName.has(language_name)) {
                            languageName = groupName.getJSONObject(language_name);
                            if (languageName.has(skill_name) && skill_bookmark == 0) {
                                languageName.remove(skill_name);
                                // 2nd parameter here indicates reduction in bookmark_count
                                updateSkillRatingJSON(call, 0);
                                bookmarkUpdated = true;
                            }
                        }
                    }
                }
            }

            if (!bookmarkUpdated && skill_bookmark == 1) {
                JSONObject bookmarkObject = new JSONObject();
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                bookmarkObject.put("bookmark", true);
                bookmarkObject.put("timestamp", timestamp.toString());
                languageName.put(skill_name, bookmarkObject);
                // 2nd parameter here indicates increase in bookmark_count
                updateSkillRatingJSON(call, 1);
            }

            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            userName.put(model_name, modelName);
            bookmarkSkill.put(email, userName, true);
            result.put("accepted", true);
            result.put("message", "Skill bookmark updated");
            if (skill_bookmark == 1) {
            	result.put("bookmark", true);
            }
            else {
                result.put("bookmark", false);
            }
            return new ServiceResponse(result);

        } else {
            throw new APIException(422, "Access token not given.");
        }
    }

    // Update skill_rating object to the skillRatingJSON and updates the bookmark
    // update_type=0 for reduction and update_type=1 for increase
    public void updateSkillRatingJSON(Query call, Integer update_type) {
        String model_name = call.get("model", "general");
        String group_name = call.get("group", "Knowledge");
        String language_name = call.get("language", "en");
        String skill_name = call.get("skill", null);

        JsonTray skillRating = DAO.skillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                    }
                }
            }
        }

        if (skillName.has("bookmark_count")) {
            int skillBookmark = skillName.getInt("bookmark_count");
            if(update_type == 0 && skillBookmark > 0) {
                skillName.put("bookmark_count", skillBookmark - 1 );
            }
            else {
                skillName.put("bookmark_count", skillBookmark + 1 );
            }
        } else {
            if (update_type == 1) {
                skillName.put("bookmark_count", 1);
            }
        }

        if (!skillName.has("feedback_count")) {
            skillName.put("feedback_count", 0);
        }

        DAO.putStars(skillName);

        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        return;
    }
}
