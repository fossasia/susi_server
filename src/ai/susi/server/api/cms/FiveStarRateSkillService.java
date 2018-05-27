package ai.susi.server.api.cms;

/**
 *  FiveStarRateSkillService
 *  Copyright by Anup Kumar Panwar, @AnupKumarPanwar
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
 * This Endpoint accepts 5 parameters. model,group,language,skill,rating.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/fiveStarRateSkill.json?model=general&group=Knowledge&skill=aboutsusi&stars=3
 */
public class FiveStarRateSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 7947060716231250102L;

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
        return "/cms/fiveStarRateSkill.json";
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
        File skill = SusiSkill.getSkillFileInLanguage(language, skill_name, false);
        String skill_stars = call.get("stars", null);
        String access_token = call.get("access_token", null);

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (!skill.exists()) {
            result.put("message", "skill does not exist");
            return new ServiceResponse(result);

        }



        JsonTray fiveStarSkillRating = DAO.fiveStarSkillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        if (fiveStarSkillRating.has(model_name)) {
            modelName = fiveStarSkillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONArray skillName = languageName.getJSONArray(skill_name);

                        Boolean alreadyByUser=false;

                        for (int i=0; i<skillName.length(); i++) {
                            JSONObject ratingObject=skillName.getJSONObject(i);
                            if (ratingObject.get("access_token").equals(access_token)){

                                String previousRating=ratingObject.get("stars").toString();

                                Timestamp timestamp=new Timestamp(System.currentTimeMillis());
                                ratingObject.put("stars", skill_stars);
                                ratingObject.put("timestamp", timestamp.toString());
                                skillName.put(i, ratingObject);
                                alreadyByUser=true;

                                // Update the skillRating.json file that contains overview of the ratings.
                                updateSkillRatingsJSON(call, previousRating);

                                break;

                            }
                        }

                        if (!alreadyByUser)
                        {
                            JSONObject ratingObject=new JSONObject();
                            Timestamp timestamp=new Timestamp(System.currentTimeMillis());
                            ratingObject.put("access_token", access_token);
                            ratingObject.put("stars", skill_stars);
                            ratingObject.put("timestamp", timestamp.toString());
                            skillName.put(ratingObject);

                            addToSkillRatingJSON(call);

                        }

                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        fiveStarSkillRating.put(model_name, modelName, true);
                        result.put("accepted", true);
                        result.put("message", "Skill ratings updated");
                        result.put("ratings", fiveStarSkillRating);
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        languageName.put(skill_name, createRatingArray(access_token, skill_stars));
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        fiveStarSkillRating.put(model_name, modelName, true);
        result.put("accepted", true);
        result.put("message", "Skill ratings updated");
        result.put("ratings", fiveStarSkillRating);
        return new ServiceResponse(result);


    }

    /* Utility function*/
    public JSONArray createRatingArray(String access_token, String skill_stars) {
        JSONArray skillName = new JSONArray();
        JSONObject ratingObject=new JSONObject();
        Timestamp timestamp=new Timestamp(System.currentTimeMillis());
        ratingObject.put("access_token", access_token);
        ratingObject.put("stars", skill_stars);
        ratingObject.put("timestamp", timestamp.toString());
        skillName.put(ratingObject);

        return skillName;
    }


    public JSONObject createRatingObject(String skill_stars) {
        JSONObject skillName = new JSONObject();
        skillName.put("positive", "0");
        skillName.put("negative", "0");


        JSONObject skillStars=new JSONObject();

        skillStars.put("one_star", "0");
        skillStars.put("two_star", "0");
        skillStars.put("three_star", "0");
        skillStars.put("four_star", "0");
        skillStars.put("five_star", "0");
        skillStars.put("avg_star", "0");
        skillStars.put("total_star", "0");

        if (skill_stars.equals("1")) {
            skillStars.put("one_star", skillStars.getInt("one_star") + 1 + "");
        }
        else if (skill_stars.equals("2")) {
            skillStars.put("two_star", skillStars.getInt("two_star") + 1 + "");
        }
        else if (skill_stars.equals("3")) {
            skillStars.put("three_star", skillStars.getInt("three_star") + 1 + "");
        }
        else if (skill_stars.equals("4")) {
            skillStars.put("four_star", skillStars.getInt("four_star") + 1 + "");
        }
        else if (skill_stars.equals("5")) {
            skillStars.put("five_star", skillStars.getInt("five_star") + 1 + "");
        }

        float totalStars=skillStars.getInt("one_star")+skillStars.getInt("two_star")+skillStars.getInt("three_star")+skillStars.getInt("four_star")+skillStars.getInt("five_star");
        float avgStar=(1*skillStars.getInt("one_star")+2*skillStars.getInt("two_star")+3*skillStars.getInt("three_star")+4*skillStars.getInt("four_star")+5*skillStars.getInt("five_star"))/totalStars;
        skillStars.put("total_star", Math.round(totalStars) + "");
        skillStars.put("avg_star", avgStar + "");
        skillName.put("stars", skillStars);

        return skillName;
    }


    public void updateSkillRatingsJSON(Query call, String previousRating){

        String model_name = call.get("model", "general");
        String group_name = call.get("group", "Knowledge");
        String language_name = call.get("language", "en");
        String skill_name = call.get("skill", null);
        String skill_stars = call.get("stars", null);

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
                        JSONObject skillStars=skillName.getJSONObject("stars");

                        // Remove previous rating of the user.

                        if (previousRating.equals("1")) {
                            skillStars.put("one_star", skillStars.getInt("one_star") - 1 + "");
                        }
                        else if (previousRating.equals("2")) {
                            skillStars.put("two_star", skillStars.getInt("two_star") - 1 + "");
                        }
                        else if (previousRating.equals("3")) {
                            skillStars.put("three_star", skillStars.getInt("three_star") - 1 + "");
                        }
                        else if (previousRating.equals("4")) {
                            skillStars.put("four_star", skillStars.getInt("four_star") - 1 + "");
                        }
                        else if (previousRating.equals("5")) {
                            skillStars.put("five_star", skillStars.getInt("five_star") - 1 + "");
                        }


                        // Add new rating.

                        if (skill_stars.equals("1")) {
                            skillStars.put("one_star", skillStars.getInt("one_star") + 1 + "");
                        }
                        else if (skill_stars.equals("2")) {
                            skillStars.put("two_star", skillStars.getInt("two_star") + 1 + "");
                        }
                        else if (skill_stars.equals("3")) {
                            skillStars.put("three_star", skillStars.getInt("three_star") + 1 + "");
                        }
                        else if (skill_stars.equals("4")) {
                            skillStars.put("four_star", skillStars.getInt("four_star") + 1 + "");
                        }
                        else if (skill_stars.equals("5")) {
                            skillStars.put("five_star", skillStars.getInt("five_star") + 1 + "");
                        }

                        float totalStars=skillStars.getInt("one_star")+skillStars.getInt("two_star")+skillStars.getInt("three_star")+skillStars.getInt("four_star")+skillStars.getInt("five_star");
                        float avgStar=(1*skillStars.getInt("one_star")+2*skillStars.getInt("two_star")+3*skillStars.getInt("three_star")+4*skillStars.getInt("four_star")+5*skillStars.getInt("five_star"))/totalStars;
                        skillStars.put("avg_star", avgStar + "");

                        skillName.put("stars",skillStars);
                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        skillRating.put(model_name, modelName, true);
                    }
                }
            }
        }
        else {
            languageName.put(skill_name, createRatingObject(skill_stars));
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            skillRating.put(model_name, modelName, true);
        }
    }


    public void addToSkillRatingJSON(Query call)
    {
        String model_name = call.get("model", "general");
        String group_name = call.get("group", "Knowledge");
        String language_name = call.get("language", "en");
        String skill_name = call.get("skill", null);
        String skill_stars = call.get("stars", null);

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
                        if (skillName.has("stars")){
                            JSONObject skillStars=skillName.getJSONObject("stars");

                            if (skill_stars.equals("1")) {
                                skillStars.put("one_star", skillStars.getInt("one_star") + 1 + "");
                            }
                            else if (skill_stars.equals("2")) {
                                skillStars.put("two_star", skillStars.getInt("two_star") + 1 + "");
                            }
                            else if (skill_stars.equals("3")) {
                                skillStars.put("three_star", skillStars.getInt("three_star") + 1 + "");
                            }
                            else if (skill_stars.equals("4")) {
                                skillStars.put("four_star", skillStars.getInt("four_star") + 1 + "");
                            }
                            else if (skill_stars.equals("5")) {
                                skillStars.put("five_star", skillStars.getInt("five_star") + 1 + "");
                            }

                            float totalStars=skillStars.getInt("one_star")+skillStars.getInt("two_star")+skillStars.getInt("three_star")+skillStars.getInt("four_star")+skillStars.getInt("five_star");
                            float avgStar=(1*skillStars.getInt("one_star")+2*skillStars.getInt("two_star")+3*skillStars.getInt("three_star")+4*skillStars.getInt("four_star")+5*skillStars.getInt("five_star"))/totalStars;
                            skillStars.put("total_star", Math.round(totalStars) + "");
                            skillStars.put("avg_star", avgStar + "");

                            skillName.put("stars", skillStars);
                        }
                        else {
                            JSONObject skillStars=new JSONObject();

                            skillStars.put("one_star", "0");
                            skillStars.put("two_star", "0");
                            skillStars.put("three_star", "0");
                            skillStars.put("four_star", "0");
                            skillStars.put("five_star", "0");
                            skillStars.put("avg_star", "0");
                            skillStars.put("total_star", "0");

                            if (skill_stars.equals("1")) {
                                skillStars.put("one_star", skillStars.getInt("one_star") + 1 + "");
                            }
                            else if (skill_stars.equals("2")) {
                                skillStars.put("two_star", skillStars.getInt("two_star") + 1 + "");
                            }
                            else if (skill_stars.equals("3")) {
                                skillStars.put("three_star", skillStars.getInt("three_star") + 1 + "");
                            }
                            else if (skill_stars.equals("4")) {
                                skillStars.put("four_star", skillStars.getInt("four_star") + 1 + "");
                            }
                            else if (skill_stars.equals("5")) {
                                skillStars.put("five_star", skillStars.getInt("five_star") + 1 + "");
                            }

                            float totalStars=skillStars.getInt("one_star")+skillStars.getInt("two_star")+skillStars.getInt("three_star")+skillStars.getInt("four_star")+skillStars.getInt("five_star");
                            float avgStar=(1*skillStars.getInt("one_star")+2*skillStars.getInt("two_star")+3*skillStars.getInt("three_star")+4*skillStars.getInt("four_star")+5*skillStars.getInt("five_star"))/totalStars;
                            skillStars.put("total_star", Math.round(totalStars) + "");
                            skillStars.put("avg_star", avgStar + "");

                            skillName.put("stars", skillStars);
                        }

                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        skillRating.put(model_name, modelName, true);
                    }
                }
            }
        }
        else {
            languageName.put(skill_name, createRatingObject(skill_stars));
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            skillRating.put(model_name, modelName, true);
        }
    }

}
