package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.SkillTransactions;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;


/**
 * Created by chetankaushik on 06/06/17.
 * This Service deletes a skill as per given query.
 * http://localhost:4000/cms/deleteSkill.json?access_token=ANecfA1GjP4Bkgv4PwjL0OAW4kODzW&model=general&group=Knowledge&language=en&skill=whois
 * When someone deletes a skill then it will move a folder delete_skills_dir.
 * When a file is moved to the delete_skills_dir its last modified date is changed to the current date.
 * Then in the caretaker there is a function which checks for files which are older than 30 days by checking the last modified date.
 * If there is any file which is older than 30 days then it deletes them.
 * This API can be used to delete a private skill as well, by sending private=1 parameter
 * http://localhost:4000/cms/deleteSkill.json?access_token=ANecfA1GjP4Bkgv4PwjL0OAW4kODzW&private=1&group=Knowledge&language=en&skill=myskill
 */

public class DeleteSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1755374387315534691L;

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
        return "/cms/deleteSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        String model_name = call.get("model", "general");
        String access_token = call.get("access_token", null);
        String privateSkill = call.get("private", null);
        String uuid = call.get("uuid", null);
        String userId = null;
        String idvalue = rights.getIdentity().getName();
        // extract userid and check for user role
        ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, access_token);
        Authentication authentication = DAO.getAuthentication(credential);
        // check if access_token is valid
        if (authentication.getIdentity() != null) {
            ClientIdentity identity = authentication.getIdentity();
            Authorization authorization = DAO.getAuthorization(identity);
            UserRole userRole = authorization.getUserRole();
            userId = identity.getUuid();
            if((userRole.getName().equals("admin") || userRole.getName().equals("superadmin")) && uuid != null) {
                userId = uuid;
            }
            if (privateSkill == null) {
                // if deleting a public skill, the person should be admin or superadmin
                if (!(userRole.getName().equals("admin") || userRole.getName().equals("superadmin"))) {
                    String group_name = call.get("group", "Knowledge");
                    String language_name = call.get("language", "en");
                    String skill_name = call.get("skill", "whois");
                    JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, group_name, language_name, skill_name);
                    // the skill can be deleted if user is the author of the skill
                    if(!(skillMetadata.get("author_email").equals(idvalue))) {
                        json.put("message", "Must be admin to delete a skill");
                        return new ServiceResponse(json);
                    }  
                }
            }
        }
        else {
        // invalid access token provided
        json.put("message", "Invalid access token");
        return new ServiceResponse(json);
        }
        
        File model = new File(DAO.model_watch_dir, model_name);
        if (privateSkill != null) {
            model = new File(DAO.private_skill_watch_dir, userId);
        }
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", "whois");
        File skill = DAO.getSkillFileInLanguage(language, skill_name, false);
        
        
        if(!DAO.deleted_skill_dir.exists()){
            DAO.deleted_skill_dir.mkdirs();
        }
        String path = skill.getPath();
        if (privateSkill != null) {
            path = path.replace(DAO.private_skill_watch_dir.getPath(),"");
        }
        else {
            path = path.replace(DAO.model_watch_dir.getPath(),"");
        }

        if (skill.exists()) {
            File file = new File(DAO.deleted_skill_dir.getPath()+path);
            file.getParentFile().mkdirs();
            if(skill.renameTo(file)){
                Boolean changed =  new File(DAO.deleted_skill_dir.getPath()+path).setLastModified(System.currentTimeMillis());
                System.out.print(changed);
                System.out.println("Skill moved successfully!");
            }else{
                System.out.println("Skill failed to move!");
            }

            json.put("message","Deleted "+ skill_name);
            json.put("accepted", true);
            //Add to git
            if (privateSkill != null) {
                DAO.deleteChatbot(userId, group_name, language_name, skill_name);
                SkillTransactions.addAndPushCommit(true, "Deleted " + skill_name, !rights.getIdentity().isAnonymous() ? rights.getIdentity().getName() : "anonymous@");
                json.put("message", "Deleted " + skill_name);
            } else {
                SkillTransactions.addAndPushCommit(false, "Deleted " + skill_name, !rights.getIdentity().isAnonymous() ? rights.getIdentity().getName() : "anonymous@");
                json.put("message", "Deleted " + skill_name);
            }
        } else {
            json.put("message", "Cannot find '" + skill + "' ('" + skill.getAbsolutePath() + "')");
        }
        return new ServiceResponse(json);
    }
}
