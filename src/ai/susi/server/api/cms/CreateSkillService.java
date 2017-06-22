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
import java.io.IOException;

/**
 * Created by saurabh on 7/6/17.
 * This Service creates an skill as per given query.
 * The skill name given in the query should not exist in the SUSI intents Folder
 * Can be tested on :-
 * http://localhost:4000/cms/createSkill.txt?model=general&group=knowledge&language=en&skill=whois
 */
public class CreateSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 2461878194569824151L;

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
        return "/cms/createSkill.txt";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", "whois");
        File skill = new File(language, skill_name + ".txt");
        String skillName = skill.getName();
        JSONObject json = new JSONObject(true);

        json.put("accepted", false);
        if (skill.exists()) {
            json.put("message", "The '" + skill + "' already exists.");
        } else {
            try {
                skill.createNewFile();
                json.put("created_file", skillName);
                json.put("accepted", true);
                // Add to git
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                Repository repository = null;
                try {

                    repository = builder.setGitDir((DAO.susi_skill_repo))
                            .readEnvironment() // scan environment GIT_* variables
                            .findGitDir() // scan up the file system tree
                            .build();

                    try (Git git = new Git(repository)) {
                        git.add()
                                .addFilepattern(skill_name)
                                .call();
                        // commit the changes
                        git.commit()
                                .setMessage("Created " + skill_name)
                                .call();

                    } catch (GitAPIException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                json.put("message", "Unable to create skill.");
            }
        }

        return new ServiceResponse(json);

    }
}