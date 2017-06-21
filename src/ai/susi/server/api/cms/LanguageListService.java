package ai.susi.server.api.cms;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import java.io.File;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to load languages from the skill database
 * i.e.
 * http://localhost:4000/cms/getLanguage.json
 * http://localhost:4000/cms/getLanguage.json?model=general&group=smalltalk&skill=German-Standalone-aiml2susi
 */
public class LanguageListService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -5176264536025896261L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getLanguage.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String skill_name = call.get("skill", "wikipedia");

        String[] languages = group.list((current, name) -> new File(current, name).isDirectory());
        ArrayList<String> languageList = new ArrayList<>();
        for(String languageName: languages) {
            File language = new File(group, languageName);
            File[] listOfSkills = language.listFiles();
            for(File skill: listOfSkills) {
                if(skill.getName().equals(skill_name + ".txt")) { // given skill found in the language, add language to language list and continue searching
                    languageList.add(language.getName());
                    break;
                }
            }
        }
        JSONArray languageJsonArray = new JSONArray(languageList);
        return new ServiceResponse(languageJsonArray);
    }
}
