package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.mind.SusiSkill;
import org.json.JSONObject;
import org.json.JSONException;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import javax.mail.Folder;
import java.io.File;
import ai.susi.json.JsonTray;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by PrP-11 on 19/07/18.
 * This Servlet gives a API Endpoint to list admin statistics.
 * It requires user role to be OPERATOR or above
 * example: http://localhost:4000/aaa/adminStats.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getUsersStats=true
 * example: http://localhost:4000/aaa/adminStats.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getSkillsStats=true
 * example: http://localhost:4000/aaa/adminStats.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getSkillsStats=true&getUsersStats=true
 */

public class AdminStats extends AbstractAPIHandler implements APIHandler {

    @Override
    public String getAPIPath() {
        return "/aaa/adminStats.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.OPERATOR;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        if (call.get("getUsersStats", false) == false && call.get("getSkillsStats", false) == false) {
            throw new APIException(422, "Bad Request. No parameter present");
        }
        JSONObject usersStats = new JSONObject(true);
        JSONObject skillsStats = new JSONObject(true);
        JSONObject result = new JSONObject(true);
        result.put("accepted", false);

	if (call.get("getSkillsStats", false)){
          String model_name = "general";
          File model = new File(DAO.model_watch_dir, model_name);
          String group_name = "All";
          String language_list = "en";
          int duration = call.get("duration", -1);
          String[] language_names = language_list.split(",");
          int totalSkills = 0;
          int reviewedSkills = 0;
          int nonReviewedSkills = 0;
          File allGroup = new File(String.valueOf(model));
          ArrayList<String> folderList = new ArrayList<String>();
          listFoldersForFolder(allGroup, folderList);

          // Count susi skills of all groups
          for (String temp_group_name : folderList){
              File group = new File(model, temp_group_name);
              for (String language_name : language_names) {
                  File language = new File(group, language_name);
                  ArrayList<String> fileList = new ArrayList<String>();
                  listFilesForFolder(language, fileList);
                  for (String skill_name : fileList) {
                      skill_name = skill_name.replace(".txt", "");
                      ++totalSkills;
                        if(SusiSkill.getSkillReviewStatus(model_name, temp_group_name, language_name, skill_name)) {
                          ++reviewedSkills;
			}else{
			  ++nonReviewedSkills;
			}
                    }
                }
            }
            skillsStats.put("totalSkills", totalSkills);
            skillsStats.put("reviewedSkills", reviewedSkills);
            skillsStats.put("nonReviewedSkills", nonReviewedSkills);
	}

	if (call.get("getUsersStats", false)){
          Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
          List<String> keysList = new ArrayList<String>();
          authorized.forEach(client -> keysList.add(client.toString()));
          String[] keysArray = keysList.toArray(new String[keysList.size()]);
          int anonymous = 0;
          int user = 0;
          int reviewer = 0;
          int operator = 0;
          int admin = 0;
          int superAdmin = 0;
          int totalUsers = 0;
          int activeUsers = 0;
          int inactiveUsers = 0;
          for (Client client : authorized) {

              // generate client identity to get user role
              ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
              Authorization authorization = DAO.getAuthorization(identity);
              UserRole userRole = authorization.getUserRole();

              // count user roles
              if (userRole.toString().toLowerCase().equals("anonymous")){
                  ++anonymous;
              }
              if (userRole.toString().toLowerCase().equals("user")){
                  ++user;
              }
              if (userRole.toString().toLowerCase().equals("reviewer")){
                  ++reviewer;
              }
              if (userRole.toString().toLowerCase().equals("operator")){
                  ++operator;
              }
              if (userRole.toString().toLowerCase().equals("admin")){
                  ++admin;
              }
              if (userRole.toString().toLowerCase().equals("superadmin")){
                  ++superAdmin;
              }

              ++totalUsers;

              //count verified status
              ClientCredential clientCredential = new ClientCredential(ClientCredential.Type.passwd_login, identity.getName());
              Authentication authentication = DAO.getAuthentication(clientCredential);
              if (authentication.getBoolean("activated", false)){
		++activeUsers;
              }else{
		++inactiveUsers;
              }
            }
            usersStats.put("anonymous", anonymous);
            usersStats.put("users", user);
            usersStats.put("reviewers", reviewer);
            usersStats.put("operators", operator);
            usersStats.put("admins", admin);
            usersStats.put("superAdmins", superAdmin);
            usersStats.put("activeUsers", activeUsers);
            usersStats.put("inactiveUsers", inactiveUsers);
            usersStats.put("totalUsers", totalUsers);
        }
        try {
            result.put("usersStats", usersStats);
            result.put("skillsStats", skillsStats);
            result.put("accepted", true);
            if (call.get("getUsersStats", false) && call.get("getSkillsStats", false)){
              result.put("message", "Success: Fetched all admin stats!");
            } else if (call.get("getUsersStats", false)){
              result.put("message", "Success: Fetched all users stats!");
            } else {
              result.put("message", "Success: Fetched all skills stats!");
            }
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(422, "Failed: unable to fetch stats!");
        }
        
    }
    private void listFilesForFolder(final File folder, ArrayList<String> fileList) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            Arrays.stream(filesInFolder)
                    .filter(fileEntry -> !fileEntry.isDirectory() && !fileEntry.getName().startsWith("."))
                    .forEach(fileEntry -> fileList.add(fileEntry.getName() + ""));
        }
    }

    private void listFoldersForFolder(final File folder, ArrayList<String> fileList) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            Arrays.stream(filesInFolder)
                    .filter(fileEntry -> fileEntry.isDirectory() && !fileEntry.getName().startsWith("."))
                    .forEach(fileEntry -> fileList.add(fileEntry.getName() + ""));
        }
    }
}
