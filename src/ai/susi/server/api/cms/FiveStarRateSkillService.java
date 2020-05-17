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
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;


/**
 * This Endpoint accepts 5 parameters. model,group,language,skill,rating.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/fiveStarRateSkill.json?model=general&group=Knowledge&skill=aboutsusi&stars=3&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        SkillQuery skillQuery = SkillQuery.getParser().parse(call).requireOrThrow();

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        String user_rating = call.get("stars", null);
        Integer skill_stars;

        JSONObject result = new JSONObject();
        if (user_rating == null) {
            throw new APIException(422, "Rating not provided.");
        }
        else {
            skill_stars = Integer.parseInt(user_rating);
        }

        if (!authorization.getIdentity().isAnonymous()) {
            String idvalue = authorization.getIdentity().getName(); // Get id from the access_token

            JsonTray fiveStarSkillRating = DAO.fiveStarSkillRating;
            JSONObject modelName = new JSONObject();
            JSONObject groupName = new JSONObject();
            JSONObject languageName = new JSONObject();
            JSONArray skillName = new JSONArray();
            JSONObject resultStars = new JSONObject();
            Boolean alreadyByUser = false;
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            if (fiveStarSkillRating.has(model_name)) {
                modelName = fiveStarSkillRating.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        languageName = groupName.getJSONObject(language_name);
                        if (languageName.has(skill_name)) {

                            skillName = languageName.getJSONArray(skill_name);

                            for (int i = 0; i < skillName.length(); i++) {
                                JSONObject ratingObject = new JSONObject();
                                ratingObject = skillName.getJSONObject(i);
                                if ((authorization.getIdentity().isEmail() && ratingObject.get("email").equals(idvalue)) ||
                                        (authorization.getIdentity().isUuid() && ratingObject.get("uuid").equals(idvalue))) {

                                    Integer previousRating = ratingObject.getInt("stars");

                                    ratingObject.put("stars", skill_stars);
                                    ratingObject.put("timestamp", timestamp.toString());
                                    skillName.put(i, ratingObject);
                                    alreadyByUser = true;

                                    // Update the skillRating.json file that contains overview of the ratings.
                                    resultStars = updateSkillRatingsJSON(skillQuery, user_rating, previousRating);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (!alreadyByUser) {
                JSONObject ratingObject = new JSONObject();
                if (authorization.getIdentity().isEmail()) ratingObject.put("email", idvalue);
                if (authorization.getIdentity().isUuid()) ratingObject.put("uuid", idvalue);
                ratingObject.put("stars", skill_stars);
                ratingObject.put("timestamp", timestamp.toString());
                skillName.put(ratingObject);
                resultStars = addToSkillRatingJSON(skillQuery, user_rating);
            }

            updateRatingsOverTime(skillQuery, skill_stars);

            languageName.put(skill_name, skillName);
            groupName.put(language_name, languageName);
            modelName.put(group_name, groupName);
            fiveStarSkillRating.put(model_name, modelName, true);
            result.put("accepted", true);
            result.put("message", "Skill ratings updated");
            result.put("ratings", resultStars);
            return new ServiceResponse(result);
        } else {
            throw new APIException(422, "Access token not given.");
        }
    }

    private void updateRatingsOverTime(SkillQuery skillQuery, int skill_stars) {
        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JsonTray ratingsOverTime = DAO.ratingsOverTime;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONArray skillName = new JSONArray();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Boolean timePeriodExists = false;

        if (ratingsOverTime.has(model_name)) {
            modelName = ratingsOverTime.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONArray(skill_name);

                        for (int i =0; i < skillName.length(); i++) {
                            JSONObject ratingObject = new JSONObject();
                            ratingObject = skillName.getJSONObject(i);
                            // Fetch overall monthly ratings
                            String ratingTime = ratingObject.get("timestamp").toString().substring(0, 7);
                            if (ratingTime.equals(timestamp.toString().substring(0, 7))) {
                                int ratingCount = ratingObject.getInt("count");
                                float skillRating = ratingObject.getFloat("rating");
                                float totalRating = skillRating * ratingCount;
                                float newAvgRating = (totalRating + skill_stars)/(ratingCount + 1);
                                ratingObject.put("rating", newAvgRating);
                                ratingObject.put("count", ratingCount + 1);
                                skillName.put(i, ratingObject);
                                timePeriodExists = true;
                                break;
                            }
                        }

                    }
                }
            }
        }

        if (!timePeriodExists) {
            JSONObject ratingObject = new JSONObject();
            ratingObject.put("rating", skill_stars);
            ratingObject.put("count", 1);
            ratingObject.put("timestamp", timestamp);
            skillName.put(ratingObject);
        }

        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        ratingsOverTime.put(model_name, modelName, true);
    }


    public JSONObject createRatingObject(String skill_stars) {
        JSONObject skillName = new JSONObject();
        skillName.put("positive", "0");
        skillName.put("negative", "0");
        skillName.put("feedback_count", 0);
        skillName.put("bookmark_count", 0);

        JSONObject skillStars = new JSONObject();

        skillStars.put("one_star", 0);
        skillStars.put("two_star", 0);
        skillStars.put("three_star", 0);
        skillStars.put("four_star", 0);
        skillStars.put("five_star", 0);
        skillStars.put("avg_star", 0);
        skillStars.put("total_star", 0);

        if (skill_stars.equals("1")) {
            skillStars.put("one_star", skillStars.getInt("one_star") + 1);
        } else if (skill_stars.equals("2")) {
            skillStars.put("two_star", skillStars.getInt("two_star") + 1);
        } else if (skill_stars.equals("3")) {
            skillStars.put("three_star", skillStars.getInt("three_star") + 1);
        } else if (skill_stars.equals("4")) {
            skillStars.put("four_star", skillStars.getInt("four_star") + 1);
        } else if (skill_stars.equals("5")) {
            skillStars.put("five_star", skillStars.getInt("five_star") + 1);
        }

        float totalStars = skillStars.getInt("one_star") + skillStars.getInt("two_star") + skillStars.getInt("three_star") + skillStars.getInt("four_star") + skillStars.getInt("five_star");
        float avgStar = (1 * skillStars.getInt("one_star") + 2 * skillStars.getInt("two_star") + 3 * skillStars.getInt("three_star") + 4 * skillStars.getInt("four_star") + 5 * skillStars.getInt("five_star")) / totalStars;

        // Format avgStar to keep 2 places after decimal
        String roundAvgStars = String.format("%.02f", avgStar);

        skillStars.put("total_star", Math.round(totalStars));
        skillStars.put("avg_star", Float.parseFloat(roundAvgStars));
        skillName.put("stars", skillStars);
        return skillName;
    }


    private JSONObject updateSkillRatingsJSON(SkillQuery skillQuery, String skill_stars, Integer previousRating) {

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JsonTray skillRating = DAO.skillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        JSONObject skillStars = new JSONObject();

        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                        skillStars = skillName.getJSONObject("stars");

                        // Remove previous rating of the user.
                        if (previousRating.equals(1)) {
                            skillStars.put("one_star", skillStars.getInt("one_star") - 1);
                        } else if (previousRating.equals(2)) {
                            skillStars.put("two_star", skillStars.getInt("two_star") - 1);
                        } else if (previousRating.equals(3)) {
                            skillStars.put("three_star", skillStars.getInt("three_star") - 1);
                        } else if (previousRating.equals(4)) {
                            skillStars.put("four_star", skillStars.getInt("four_star") - 1);
                        } else if (previousRating.equals(5)) {
                            skillStars.put("five_star", skillStars.getInt("five_star") - 1);
                        }

                        // Add new rating.
                        if (skill_stars.equals("1")) {
                            skillStars.put("one_star", skillStars.getInt("one_star") + 1);
                        } else if (skill_stars.equals("2")) {
                            skillStars.put("two_star", skillStars.getInt("two_star") + 1);
                        } else if (skill_stars.equals("3")) {
                            skillStars.put("three_star", skillStars.getInt("three_star") + 1);
                        } else if (skill_stars.equals("4")) {
                            skillStars.put("four_star", skillStars.getInt("four_star") + 1);
                        } else if (skill_stars.equals("5")) {
                            skillStars.put("five_star", skillStars.getInt("five_star") + 1);
                        }

                        float totalStars = skillStars.getInt("one_star") + skillStars.getInt("two_star") + skillStars.getInt("three_star") + skillStars.getInt("four_star") + skillStars.getInt("five_star");
                        float avgStar = (1 * skillStars.getInt("one_star") + 2 * skillStars.getInt("two_star") + 3 * skillStars.getInt("three_star") + 4 * skillStars.getInt("four_star") + 5 * skillStars.getInt("five_star")) / totalStars;

                        // Format avgStar to keep 2 places after decimal
                        String roundAvgStars = String.format("%.02f", avgStar);

                        skillStars.put("avg_star", Float.parseFloat(roundAvgStars));
                    }
                }
            }
        }

        skillName.put("stars", skillStars);
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        return skillStars;
    }


    private JSONObject addToSkillRatingJSON(SkillQuery skillQuery, String skill_stars) {
        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        JsonTray skillRating = DAO.skillRating;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        JSONObject skillStars = new JSONObject();

        if (skillRating.has(model_name)) {
            modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {

                        skillName = languageName.getJSONObject(skill_name);
                        if (skillName.has("stars")) {
                            skillStars = skillName.getJSONObject("stars");

                            if (skill_stars.equals("1")) {
                                skillStars.put("one_star", skillStars.getInt("one_star") + 1);
                            } else if (skill_stars.equals("2")) {
                                skillStars.put("two_star", skillStars.getInt("two_star") + 1);
                            } else if (skill_stars.equals("3")) {
                                skillStars.put("three_star", skillStars.getInt("three_star") + 1);
                            } else if (skill_stars.equals("4")) {
                                skillStars.put("four_star", skillStars.getInt("four_star") + 1);
                            } else if (skill_stars.equals("5")) {
                                skillStars.put("five_star", skillStars.getInt("five_star") + 1);
                            }

                            float totalStars = skillStars.getInt("one_star") + skillStars.getInt("two_star") + skillStars.getInt("three_star") + skillStars.getInt("four_star") + skillStars.getInt("five_star");
                            float avgStar = (1 * skillStars.getInt("one_star") + 2 * skillStars.getInt("two_star") + 3 * skillStars.getInt("three_star") + 4 * skillStars.getInt("four_star") + 5 * skillStars.getInt("five_star")) / totalStars;

                            // Format avgStar to keep 2 places after decimal
                            String roundAvgStars = String.format("%.02f", avgStar);

                            skillStars.put("total_star", Math.round(totalStars));
                            skillStars.put("avg_star", Float.parseFloat(roundAvgStars));
                            skillName.put("stars", skillStars);
                        } else {

                            skillStars.put("one_star", 0);
                            skillStars.put("two_star", 0);
                            skillStars.put("three_star", 0);
                            skillStars.put("four_star", 0);
                            skillStars.put("five_star", 0);
                            skillStars.put("avg_star", 0);
                            skillStars.put("total_star", 0);

                            if (skill_stars.equals("1")) {
                                skillStars.put("one_star", skillStars.getInt("one_star") + 1);
                            } else if (skill_stars.equals("2")) {
                                skillStars.put("two_star", skillStars.getInt("two_star") + 1);
                            } else if (skill_stars.equals("3")) {
                                skillStars.put("three_star", skillStars.getInt("three_star") + 1);
                            } else if (skill_stars.equals("4")) {
                                skillStars.put("four_star", skillStars.getInt("four_star") + 1);
                            } else if (skill_stars.equals("5")) {
                                skillStars.put("five_star", skillStars.getInt("five_star") + 1);
                            }

                            float totalStars = skillStars.getInt("one_star") + skillStars.getInt("two_star") + skillStars.getInt("three_star") + skillStars.getInt("four_star") + skillStars.getInt("five_star");
                            float avgStar = (1 * skillStars.getInt("one_star") + 2 * skillStars.getInt("two_star") + 3 * skillStars.getInt("three_star") + 4 * skillStars.getInt("four_star") + 5 * skillStars.getInt("five_star")) / totalStars;

                            // Format avgStar to keep 2 places after decimal
                            String roundAvgStars = String.format("%.02f", avgStar);

                            skillStars.put("total_star", Math.round(totalStars));
                            skillStars.put("avg_star", Float.parseFloat(roundAvgStars));
                            skillName.put("stars", skillStars);
                        }

                        languageName.put(skill_name, skillName);
                        groupName.put(language_name, languageName);
                        modelName.put(group_name, groupName);
                        skillRating.put(model_name, modelName, true);
                        return skillStars;
                    }
                }
            }
        }
        skillName = createRatingObject(skill_stars);
        skillStars = skillName.getJSONObject("stars");
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillRating.put(model_name, modelName, true);
        return skillStars;
    }
}
