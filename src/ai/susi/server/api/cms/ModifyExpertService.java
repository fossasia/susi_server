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
import java.util.Objects;

/**
 * Created by chetankaushik on 07/06/17.
 * This Endpoint accepts 5 parameters. model,group,language,expert,content, changelog.
 * changelog is the commit message that you want to set for the versioning system.
 * before modifying an expert the expert must exist in the directory.
 * !IMPORTANT! --> Content must be URL Encoded
 * http://localhost:4000/cms/modifyExpert.json?model=general&group=knowledge&expert=who&content=What%20is%20stock%20price%20of%20*%3F|What%20is%20stock%20price%20of%20*|stock%20price%20of%20*|*%20stock%20price%20of%20*%0A!console%3A%24l%24%0A{%0A%20%22url%22%3A%22http%3A%2F%2Ffinance.google.com%2Ffinance%2Finfo%3Fclient%3Dig%26q%3DNASDAQ%3A%241%24%22%2C%0A%20%22path%22%3A%22%24.[0]%22%0A}%0Aeol%0A&changelog=testing
 */
public class ModifyExpertService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1834363513093189312L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/modifyExpert.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String expert_name = call.get("expert", null);
        File expert = new File(language, expert_name + ".txt");

        String commit_message = call.get("changelog", null);
        if(commit_message==null){
            JSONObject error = new JSONObject();
            error.put("accepted", false);
            return new ServiceResponse(error);
        }
        // Checking for file existence
        if(!expert.exists()){
            JSONObject error = new JSONObject();
            error.put("accepted", false);
            return new ServiceResponse(error);
        }
        // Reading Content for expert
        String content = call.get("content", "");
        if(Objects.equals(content, "")){
            JSONObject error = new JSONObject();
            error.put("accepted", false);
            return new ServiceResponse(error);
        }
        // Writing to File
        try (FileWriter file = new FileWriter(expert)) {
            file.write(content);
            JSONObject success = new JSONObject();
            success.put("accepted", true);

            //Add to git
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = null;
            try {
                repository = builder.setGitDir((DAO.susi_skill_repo))
                        .readEnvironment() // scan environment GIT_* variables
                        .findGitDir() // scan up the file system tree
                        .build();

                try (Git git = new Git(repository)) {
                    git.add()
                            .addFilepattern(expert_name)
                            .call();
                    // and then commit the changes
                    git.commit()
                            .setMessage(commit_message)
                            .call();

                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new ServiceResponse(success);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
