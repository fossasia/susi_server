package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by AnupKumarPAnwar on 28/05/18.
 * API Endpoint to get rating by a particular user on a skill.
 * http://127.0.0.1:4000/cms/getRatingByUser.json?model=general&group=Knowledge&language=en&skill=aboutsusi&access_token=pUcx5PdbNsqPEJyE0YrOGXm3IYVJD5
 */

public class GetRatingByUser extends AbstractAPIHandler implements APIHandler {
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
        return "/cms/getRatingByUser.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        String group_name = call.get("group", "All");
        String language_name = call.get("language", "en");
        String skill_name = call.get("skill", null);
        String access_token = call.get("access_token", null);

        if (access_token == null) {
            throw new APIException(422, "Bad access_token.");
        }


        if (authorization.getIdentity().isEmail()) {
            String email = authorization.getIdentity().getName();   //Get email from the access_token
            JSONObject result = new JSONObject();
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

                            for (int i = 0; i < skillName.length(); i++) {
                                JSONObject ratingObject = skillName.getJSONObject(i);
                                if (ratingObject.get("email").equals(email)) {
                                    result.put("accepted", true);
                                    result.put("message", "Fetched user rating successfully");
                                    result.put("ratings", ratingObject);
                                    return new ServiceResponse(result);
                                }
                            }
                            result.put("accepted", true);
                            result.put("message", "Skill not yet rated by the user");
                            return new ServiceResponse(result);
                        }
                    }
                }
            }
            result.put("accepted", true);
            result.put("message", "Skill not rated by the user");
            return new ServiceResponse(result);
        } else {
            throw new APIException(422, "Bad access_token.");
        }
    }

}
