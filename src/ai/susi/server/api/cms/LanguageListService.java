package ai.susi.server.api.cms;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.IO;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Servlet to load languages from the skill database
 * i.e.
 * http://localhost:4000/cms/getLanguage.json
 * http://localhost:4000/cms/getLanguage.json?model=general&group=Knowledge&skill=German-Standalone-aiml2susi
 */
public class LanguageListService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -5176264536025896261L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getLanguage.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        SkillQuery skillQuery = SkillQuery.getParser("wikipedia").parse(call);
        String skill_name = skillQuery.getSkill();
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);

        Path group = skillQuery.getGroupPath();
        String[] languages = group.toFile().list((current, name) -> new File(current, name).isDirectory());
        ArrayList<String> languageList = new ArrayList<>();
        for(String languageName: languages) {
            File language = IO.resolvePath(group, languageName).toFile();
            File[] listOfSkills = language.listFiles();
            for(File skill: listOfSkills) {
                if(skill.getName().equals(skill_name + ".txt")) { // given skill found in the language, add language to language list and continue searching
                    languageList.add(language.getName());
                    break;
                }
            }
        }
        JSONArray languageJsonArray = new JSONArray(languageList);
        json.put("languageJsonArray", languageJsonArray);
        json.put("accepted", true);
        return new ServiceResponse(json);
    }
}
