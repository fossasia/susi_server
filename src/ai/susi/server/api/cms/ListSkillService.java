package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 Can be tested on 127.0.0.1:4000/cms/getSkillList.json
 */

public class ListSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8691003678852307876L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        JSONObject skillObject = new JSONObject();
        ArrayList<String> fileList = new ArrayList<String>();
        fileList =  listFilesForFolder(language, fileList);
        JsonTray skillRating = DAO.skillRating;
            for (String skill : fileList) {
                JSONObject skillMetadata = new JSONObject();
               skill = skill.replace(".txt", "");
                skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
                skillMetadata.put("descriptions",JSONObject.NULL);
                skillMetadata.put("image", JSONObject.NULL);
                skillMetadata.put("author", JSONObject.NULL);
                skillMetadata.put("author_url", JSONObject.NULL);
                skillMetadata.put("skill_name", JSONObject.NULL);
                skillMetadata.put("terms_of_use", JSONObject.NULL);
                skillMetadata.put("dynamic_content", false);
                skillMetadata.put("examples", JSONObject.NULL);
                skillMetadata.put("skill_rating", JSONObject.NULL);
                for (Map.Entry<String, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
                    String path = entry.getKey();
                    if ((path.indexOf("/" + model_name + "/") > 0)
                            && (path.indexOf("/" + group_name + "/") > 0) &&
                            (path.indexOf("/" + language_name + "/") > 0) &&
                            (path.indexOf("/" + skill + ".txt") > 0)) {
                        skillMetadata.put("developer_privacy_policy", entry.getValue().getDeveloperPrivacyPolicy() ==null ? JSONObject.NULL:entry.getValue().getDeveloperPrivacyPolicy());
                        skillMetadata.put("descriptions", entry.getValue().getDescription() ==null ? JSONObject.NULL:entry.getValue().getDescription());
                        skillMetadata.put("image", entry.getValue().getImage() ==null ? JSONObject.NULL: entry.getValue().getImage());
                        skillMetadata.put("author", entry.getValue().getAuthor()  ==null ? JSONObject.NULL:entry.getValue().getAuthor());
                        skillMetadata.put("author_url", entry.getValue().getAuthorURL() ==null ? JSONObject.NULL:entry.getValue().getAuthorURL());
                        skillMetadata.put("skill_name", entry.getValue().getSkillName() ==null ? JSONObject.NULL: entry.getValue().getSkillName());
                        skillMetadata.put("terms_of_use", entry.getValue().getTermsOfUse() ==null ? JSONObject.NULL:entry.getValue().getTermsOfUse());
                        skillMetadata.put("dynamic_content", entry.getValue().getDynamicContent());
                        skillMetadata.put("examples", entry.getValue().getExamples() ==null ? JSONObject.NULL: entry.getValue().getExamples());
                    }
                }

                if (skillRating.has(model_name)) {
                    JSONObject modelName = skillRating.getJSONObject(model_name);
                    if (modelName.has(group_name)) {
                        JSONObject groupName = modelName.getJSONObject(group_name);
                        if (groupName.has(language_name)) {
                            JSONObject  languageName = groupName.getJSONObject(language_name);
                            if (languageName.has(skill)) {
                                JSONObject skillName = languageName.getJSONObject(skill);
                               skillMetadata.put("skill_rating", skillName);
                            }
                        }
                    }
                }

               skillObject.put(skill, skillMetadata);
            }
                json.put("model", model_name)
                .put("group", group_name)
                .put("language", language_name)
                        .put("skills", skillObject);
        json.put("accepted", true);
        json.put("message","Success: Fetched skill list");
        return new ServiceResponse(json);

    }

    ArrayList<String> listFilesForFolder(final File folder,  ArrayList<String> fileList) {

        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            for (final File fileEntry : filesInFolder) {
                if (!fileEntry.isDirectory() && !fileEntry.getName().startsWith(".")) {
                    fileList.add(fileEntry.getName()+"");
                }
            }
        }
        return  fileList;
    }
}
