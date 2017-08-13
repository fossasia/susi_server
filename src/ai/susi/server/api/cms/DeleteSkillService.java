package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by chetankaushik on 06/06/17.
 * This Service deletes a skill as per given query.
 * http://localhost:4000/cms/deleteSkill.txt?model=general&group=Knowledge&language=en&skill=whois
 */
public class DeleteSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1755374387315534691L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/deleteSkill.txt";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", "whois");
        File skill = new File(language, skill_name + ".txt");
        String SkillName = skill.getName();
        JSONObject json = new JSONObject(true);

        json.put("accepted", false);
        if (skill.exists()) {
            skill.delete();
            json.put("deleted_file", SkillName);

            //Add to git
            try (Git git = DAO.getGit()) {
                git.add()
                        .addFilepattern(skill_name)
                        .call();
                // and then commit the changes
                git.commit()
                        .setMessage("Deleted " + skill_name)
                        .call();

                json.put("accepted", true);
                json.put("message", "Deleted " + skill_name);
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();
            }
        } else {
            json.put("message", "Cannot find '" + skill + "' ('" + skill.getAbsolutePath() + "')");
        }

        return new ServiceResponse(json);

    }

}
