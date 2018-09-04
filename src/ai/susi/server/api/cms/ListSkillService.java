package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 * Can be tested on http://127.0.0.1:4000/cms/getSkillList.json
 * Other params are - applyFilter, filter_type, filter_name, count
 * This servlet also gives an API endpoint to list all the private skills if the access_token is given.
 * Can be tested on http://127.0.0.1:4000/cms/getSkillList.json?private=1&access_token=accessTokenHere
 */

public class ListSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8691003678852307876L;

    private static int totalSkills = 0;
    private static int reviewedSkills = 0;
    private static int nonReviewedSkills = 0;
    private static int editableSkills = 0;
    private static int nonEditableSkills = 0;
    private static int staffPicks = 0;
    private static int systemSkills = 0;

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
        return "/cms/getSkillList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        String userId = null;
        Boolean shouldReturnSkillStats = false;
        String privateSkill = call.get("private", null);
        if (call.get("access_token", null) != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
            ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, call.get("access_token", null));
            Authentication authentication = DAO.getAuthentication(credential);
            // check if access_token is valid
            if (authentication.getIdentity() != null) {
                ClientIdentity identity = authentication.getIdentity();
                Authorization authorization = DAO.getAuthorization(identity);
                String userRole = authorization.getUserRole().toString().toLowerCase();
                if (userRole.equals("user") || userRole.equals("reviewer")) {
                    shouldReturnSkillStats = false;
                } else {
                    shouldReturnSkillStats = true;
                }
                userId = identity.getUuid();
            }
        }

        if(privateSkill != null) {
            if(userId != null) {
                JsonTray chatbot = DAO.chatbot;
                JSONObject result = new JSONObject();
                JSONObject userObject = new JSONObject();
                JSONArray botDetailsArray = new JSONArray();

                userObject = chatbot.getJSONObject(userId);
                JSONObject groupObject = new JSONObject();
                JSONObject languageObject = new JSONObject();

                Iterator groupNames = userObject.keys();
                List<String> groupnameKeysList = new ArrayList<String>();

                while(groupNames.hasNext()) {
                    String key = (String) groupNames.next();
                    groupnameKeysList.add(key);
                }

                for(String group_name : groupnameKeysList)
                {
                    groupObject = userObject.getJSONObject(group_name);
                    Iterator languageNames = groupObject.keys();
                    List<String> languagenameKeysList = new ArrayList<String>();

                    while(languageNames.hasNext()) {
                        String key = (String) languageNames.next();
                        languagenameKeysList.add(key);
                    }

                    for(String language_name : languagenameKeysList)
                    {
                        languageObject = groupObject.getJSONObject(language_name);
                        Iterator skillNames = languageObject.keys();
                        List<String> skillnamesKeysList = new ArrayList<String>();

                        while(skillNames.hasNext()) {
                            String key = (String) skillNames.next();
                            skillnamesKeysList.add(key);
                        }

                        for(String skill_name : skillnamesKeysList)
                        {
                            JSONObject botDetails = languageObject.getJSONObject(skill_name);
                            botDetails.put("name", skill_name);
                            botDetails.put("language", language_name);
                            botDetails.put("group", group_name);
                            botDetailsArray.put(botDetails);
                            result.put("chatbots", botDetailsArray);
                        }
                    }
                }

                if(result.length()==0) {
                    result.put("accepted", false);
                    result.put("message", "User has no chatbots.");
                    return new ServiceResponse(result);
                }

                result.put("accepted", true);
                result.put("message", "All chatbots of user fetched.");
                return new ServiceResponse(result);
            }
        }

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "All");
        String language_list = call.get("language", "en");
        int duration = call.get("duration", -1);
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject(true);
        JSONObject skillObject = new JSONObject();
        String countString = call.get("count", null);
        int offset = call.get("offset", 0);
        String searchQuery = call.get("q", null);
        int page = call.get("page", 0);
        Integer count = null;
        Boolean countFilter = false;
        Boolean dateFilter = false;
        Boolean searchFilter = false;
        String reviewed = call.get("reviewed", "false");
        String staff_picks = call.get("staff_picks", "false");
        String[] language_names = language_list.split(",");
        // Initialises the stat counters
        initStatCounters();

        if (!(reviewed.equals("true") || reviewed.equals("false"))) {
            throw new APIException(400, "Bad service call.");
        }

        if (!(staff_picks.equals("true") || staff_picks.equals("false"))) {
            throw new APIException(400, "Bad service call.");
        }

        if(countString != null) {
            if(Integer.parseInt(countString) < 0) {
                throw new APIException(422, "Invalid count value. It should be positive.");
            } else {
                countFilter = true;
                try {
                    count = Integer.parseInt(countString);
                    offset = page*count;
                } catch(NumberFormatException ex) {
                    throw new APIException(422, "Invalid count value.");
                }
            }
        }

        if (searchQuery !=null && !StringUtils.isBlank(searchQuery))
        {
            searchFilter=true;
            searchQuery="(.*)"+searchQuery.toLowerCase()+"(.*)";
        }

        // Returns susi skills list of all groups
        if (group_name.equals("All")) {
            File allGroup = new File(String.valueOf(model));
            ArrayList<String> folderList = new ArrayList<String>();
            listFoldersForFolder(allGroup, folderList);
            json.put("accepted", false);

            for (String temp_group_name : folderList){
                File group = new File(model, temp_group_name);
                for (String language_name : language_names) {
                    File language = new File(group, language_name);
                    ArrayList<String> fileList = new ArrayList<String>();
                    listFilesForFolder(language, fileList);
                    for (String skill_name : fileList) {
                        skill_name = skill_name.replace(".txt", "");
                        JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, temp_group_name, language_name, skill_name, duration);

                        if(shouldReturnSkillInResponse(skillMetadata, reviewed, staff_picks)) {
                            jsonArray.put(skillMetadata);
                            skillObject.put(skill_name, skillMetadata);
                        } else {
                            continue;
                        }
                    }
                }
            }

        }
        // Returns susi skills list of a particular group
        else {
            File group = new File(model, group_name);
            for (String language_name : language_names) {
                File language = new File(group, language_name);
                json.put("accepted", false);
                ArrayList<String> fileList = new ArrayList<String>();
                listFilesForFolder(language, fileList);
                for (String skill_name : fileList) {
                    skill_name = skill_name.replace(".txt", "");
                    JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, group_name, language_name, skill_name, duration);
                    if(shouldReturnSkillInResponse(skillMetadata, reviewed, staff_picks)) {
                        jsonArray.put(skillMetadata);
                        skillObject.put(skill_name, skillMetadata);
                    } else {
                        continue;
                    }
                }
            }
        }
        // if filter is applied, sort the data accordingly
        if (call.get("applyFilter", false)) {

            JSONArray filteredData = new JSONArray();
            List<JSONObject> jsonValues = new ArrayList<JSONObject>();

            // temporary list to extract objects from skillObject
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonValues.add(jsonArray.getJSONObject(i));
            }

            String filter_name = call.get("filter_name", null);
            String filter_type = call.get("filter_type", null);

            if (filter_name.equals(null) || filter_type.equals(null)) {
                throw new APIException(422, "Bad Filters.");
            }

            // Check for empty or null filter
            if (filter_name.trim() == null || filter_type.trim() == null) {
                throw new APIException(422, "Bad Filters.");
            }

            filter_name = filter_name.toLowerCase();
            filter_type = filter_type.toLowerCase();

            if (filter_type.equals("creation_date")) {
                dateFilter = true;
                DAO.sortByCreationTime(jsonValues, filter_name.equals("ascending"));
            } else if (filter_type.equals("modified_date")) {
                dateFilter = true;
                DAO.sortByModifiedTime(jsonValues, filter_name.equals("ascending"));
            } else if (filter_type.equals("lexicographical")) {
                DAO.sortBySkillName(jsonValues, filter_name.equals("ascending"));
            }
            else if (filter_type.equals("rating")) {
                DAO.sortByAvgStar(jsonValues, filter_name.equals("ascending"));
            }
            else if (filter_type.equals("usage")) {
                DAO.sortByUsageCount(jsonValues, filter_name.equals("ascending"));
            }
            else if (filter_type.equals("feedback")) {
                DAO.sortByFeedbackCount(jsonValues, filter_name.equals("ascending"));
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                if (i < offset ) {
                    continue;
                }

                if(countFilter) {
                    if(count == 0) {
                        break;
                    } else {
                        count --;
                     }
                 }
                 if (dateFilter && duration > 0) {
                     long durationInMillisec = TimeUnit.DAYS.toMillis(duration);
                     long timestamp = System.currentTimeMillis() - durationInMillisec;
                     String startDate = new Timestamp(timestamp).toString().substring(0, 10); //substring is used for getting timestamp upto date only
                    if (filter_type.equals("creation_date")) {
                         String skillCreationDate = jsonValues.get(i).get("creationTime").toString().substring(0,10);
                         if (skillCreationDate.compareToIgnoreCase(startDate) < 0) continue;
                    } else if (filter_type.equals("modified_date")) {
                        String skillModifiedDate = jsonValues.get(i).get("lastModifiedTime").toString().substring(0,10);
                         if (skillModifiedDate.compareToIgnoreCase(startDate) < 0) continue;
                    }
                 }
                 if (searchFilter) {
                     JSONObject skillMetadata = jsonValues.get(i);
                     String skillName = skillMetadata.get("skill_name").toString().toLowerCase();
                     String authorName = skillMetadata.get("author").toString().toLowerCase();
                     String skillDescription = skillMetadata.get("descriptions").toString().toLowerCase();
                     Boolean skillMatches = false;
                     if (!skillName.matches(searchQuery) && !authorName.matches(searchQuery) && !skillDescription.matches(searchQuery)) {
                         try {
                             String skillExamples = skillMetadata.get("examples").toString().toLowerCase();
                             if (!skillExamples.matches(searchQuery))
                             {
                                 continue;
                             }
                         }
                         catch (Exception e)
                         {
                             continue;
                         }
                     }
                 }
                filteredData.put(jsonValues.get(i));
            }
            if (countFilter) {
                try {
                    count = Integer.parseInt(countString);
                    int pageCount = jsonArray.length() % count == 0 ? (jsonArray.length() / count) : (jsonArray.length() / count) + 1;
                    json.put("pageCount", pageCount);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            json.put("filteredData", filteredData);
        } else {
            if(countFilter) {
                JSONObject tempSkillObject = new JSONObject();
                tempSkillObject = skillObject;
                for (int i = 0; i < skillObject.length(); i++) {
                    if (i < offset ) {
                        continue;
                    }
                    if(count == 0) {
                        break;
                    } else {
                        count --;
                    }
                    String keyName = skillObject.names().getString(i);
                    tempSkillObject.put(keyName, skillObject.getJSONObject(keyName));
                }
                if (countFilter) {
                    try {
                        count = Integer.parseInt(countString);
                        int pageCount = skillObject.length() % count == 0 ? (skillObject.length() / count) : (skillObject.length() / count) + 1;
                        json.put("pageCount", pageCount);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                skillObject = tempSkillObject;
            }
        }

        if(shouldReturnSkillStats) {
            JSONObject skillStats = new JSONObject();
            skillStats.put("totalSkills", totalSkills);
            skillStats.put("reviewedSkills", reviewedSkills);
            skillStats.put("nonReviewedSkills", nonReviewedSkills);
            skillStats.put("editableSkills", editableSkills);
            skillStats.put("nonEditableSkills", nonEditableSkills);
            skillStats.put("staffPicks", staffPicks);
            skillStats.put("systemSkills", systemSkills);
            json.put("skillStats", skillStats);
        }

        json.put("model", model_name)
                .put("group", group_name)
                .put("language", language_list);
        json.put("skills", skillObject);
        json.put("accepted", true);
        json.put("message", "Success: Fetched skill list");
        return new ServiceResponse(json);

    }

    private static Boolean shouldReturnSkillInResponse(JSONObject skillMetadata, String reviewed, String staff_picks) {
        totalSkills++;

        if(skillMetadata.getBoolean("editable") == true) {
            editableSkills++;
        } else {
            nonEditableSkills++;
        }

        if(skillMetadata.getBoolean("reviewed") == true) {
            reviewedSkills++;
        } else {
            nonReviewedSkills++;
        }

        if(skillMetadata.getBoolean("staffPick") == true) {
            staffPicks++;
        }

        if(skillMetadata.getBoolean("systemSkill") == true) {
            systemSkills++;
        }

        if(reviewed.equals("true") && skillMetadata.getBoolean("reviewed") == false) {
            return false;
        }

        if(staff_picks.equals("true") && skillMetadata.getBoolean("staffPick") == false) {
            return false;
        }
        return true;
    }

    private static void initStatCounters() {
        totalSkills = 0;
        editableSkills = 0;
        nonEditableSkills = 0;
        reviewedSkills = 0;
        nonReviewedSkills = 0;
        staffPicks = 0;
        systemSkills = 0;
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
