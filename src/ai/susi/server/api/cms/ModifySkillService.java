package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by chetankaushik on 07/06/17.
 * This Endpoint accepts 5 parameters. model,group,language,skill,content, changelog.
 * changelog is the commit message that you want to set for the versioning system.
 * before modifying an skill the skill must exist in the directory.
 * !IMPORTANT! --> Content must be URL Encoded
 * http://localhost:4000/cms/modifySkill.json?model=general&group=knowledge&skill=who&content=What%20is%20stock%20price%20of%20*%3F|What%20is%20stock%20price%20of%20*|stock%20price%20of%20*|*%20stock%20price%20of%20*%0A!console%3A%24l%24%0A{%0A%20%22url%22%3A%22http%3A%2F%2Ffinance.google.com%2Ffinance%2Finfo%3Fclient%3Dig%26q%3DNASDAQ%3A%241%24%22%2C%0A%20%22path%22%3A%22%24.[0]%22%0A}%0Aeol%0A&changelog=testing
 */
public class ModifySkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1834363513093189312L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/modifySkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.susi_unreviewed_model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = new File(language, skill_name + ".txt");

        if (!language.isDirectory()) {
            boolean success = language.mkdirs();
            if (success) {
                System.out.println("Created path: " + language.getPath());
            } else {
                System.out.println("Could not create path: " + language.getPath());
            }
        } else {
            System.out.println("Path exists: " + language.getPath());
        }

        // Checking for file existence
        JSONObject json = new JSONObject();

        if (!skill.exists()){
            try {
                skill.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Reading Content for skill
        String content = call.get("content", "");
        if (content.length() == 0) {
            json.put("message", "modification is empty");
            return new ServiceResponse(json);
        }
        
        // Writing to File
        try (FileWriter file = new FileWriter(skill)) {
            file.write(content);
            json.put("accepted",true);

        } catch (IOException e) {
            e.printStackTrace();
            json.put("accepted",false);
            json.put("message", "error: " + e.getMessage());
        }
        json.put("message","Skill Uploaded to Server for reviewing");
        return new ServiceResponse(json);
    }
    
}
