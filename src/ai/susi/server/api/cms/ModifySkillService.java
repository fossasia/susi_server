package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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
 * http://localhost:4000/cms/modifySkill.json?model=general&group=Knowledge&skill=who&content=What%20is%20stock%20price%20of%20*%3F|What%20is%20stock%20price%20of%20*|stock%20price%20of%20*|*%20stock%20price%20of%20*%0A!console%3A%24l%24%0A{%0A%20%22url%22%3A%22http%3A%2F%2Ffinance.google.com%2Ffinance%2Finfo%3Fclient%3Dig%26q%3DNASDAQ%3A%241%24%22%2C%0A%20%22path%22%3A%22%24.[0]%22%0A}%0Aeol%0A&changelog=testing
 */
public class ModifySkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1834363513093189312L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.USER; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/modifySkill.json";
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
        File skill = new File(language, skill_name + ".txt");

        String commit_message = call.get("changelog", null);

        String path = skill.getPath().replace(DAO.model_watch_dir.toString(), "models");

        response.setHeader("Access-Control-Allow-Origin", "*"); // enable CORS

        if(commit_message==null){
            JSONObject error = new JSONObject();
            error.put("accepted", false);
            return new ServiceResponse(error);
        }

        // Checking for file existence
        JSONObject json = new JSONObject();
        json.put("accepted", false);
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

        } catch (IOException e) {
            e.printStackTrace();
            json.put("message", "error: " + e.getMessage());
        }
        //Add to git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        try {
            repository = builder.setGitDir((DAO.susi_skill_repo))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            try (Git git = new Git(repository)) {
                Status status = git.status().call();

                git.add()
                        .addFilepattern(".")
                        .call();
                // and then commit the changes
                git.commit()
                        .setMessage(commit_message)
                        .call();
                String remote = "origin";
                String branch = "refs/heads/master";
                String trackingBranch = "refs/remotes/" + remote + "/master";
                RefSpec spec = new RefSpec(branch + ":" + branch);

                PushCommand push=git.push();
                push.setForce(true);
                push.setCredentialsProvider(new UsernamePasswordCredentialsProvider( DAO.getConfig("github.username", ""),DAO.getConfig("github.password","")));
                push.call();

                json.put("accepted", true);
                json.put("message","Skill Modified Successfully");
                return new ServiceResponse(json);
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResponse(json);
    }

}
