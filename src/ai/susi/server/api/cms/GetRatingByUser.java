package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import ai.susi.tools.skillqueryparser.SkillQueryParser;
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

        SkillQuery skillQuery = SkillQueryParser.Builder.getInstance().group("All").build().parse(call);

        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();
        String language_name = skillQuery.getLanguage();
        String skill_name = skillQuery.getSkill();

        if (!authorization.getIdentity().isAnonymous()) {
        	String idvalue = authorization.getIdentity().getName(); // Get id from the access_token
            
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
                                if ((authorization.getIdentity().isEmail() && ratingObject.get("email").equals(idvalue)) ||
                                    (authorization.getIdentity().isUuid() && ratingObject.get("uuid").equals(idvalue))) {
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
