package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;


/**
 * Created by saurabhjn76 .
 * This Endpoint accepts 5 parameters. model,group,language,skill,content, rating.
 * rating can be postive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/rateSkill.json?model=general&group=knowledge&skill=who&rating=positive
 */
public class RateSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -4437528781864678779L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/rateSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = new File(language, skill_name + ".txt");
        String skill_rate = call.get("rating", null);

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (!skill.exists()) {
            result.put("message", "skill does not exist");
            return new ServiceResponse(result);

        }
        JsonTray skillRating = DAO.skillRating;
        if (skillRating.has(model_name)) {
            JSONObject modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                JSONObject groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    JSONObject languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONObject skillName = languageName.getJSONObject(skill_name);
                        skillName.put(skill_rate, skillName.getInt(skill_name) + 1 + "");
                    } else {
                        languageName.put(skill_name, createRatingObject(skill_rate,skill_name));
                    }
                } else {
                    JSONObject languageName = new JSONObject();
                    languageName.put(skill_name, createRatingObject(skill_rate,skill_name));
                    groupName.put(language_name, languageName);
                }
            } else {
                JSONObject languageName = new JSONObject();
                languageName.put(skill_name, createRatingObject(skill_rate,skill_name));
                JSONObject groupName = new JSONObject();
                groupName.put(language_name, languageName);
                modelName.put(group_name, groupName);
            }
        } else {
            JSONObject languageName = new JSONObject();
            languageName.put(skill_name, createRatingObject(skill_rate,skill_name));
            JSONObject groupName = new JSONObject();
            groupName.put(language_name, languageName);
            JSONObject modelName = new JSONObject();
            modelName.put(group_name, groupName);
            skillRating.put(model_name, modelName, true);
        }
        result.put("accepted", true);
        return new ServiceResponse(result);
    }
   /* Utility function*/
    public JSONObject createRatingObject(String skill_rate, String skill_name) {
        JSONObject skillName = new JSONObject();
        skillName.put("positive", "0");
        skillName.put("negative", "0");
        skillName.put(skill_rate, skillName.getInt(skill_rate) + 1 + "");
        return skillName;
    }
}
