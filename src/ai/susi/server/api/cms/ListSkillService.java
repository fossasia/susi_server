package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import ai.susi.tools.IO;
import ai.susi.tools.skillqueryparser.SkillQuery;
import ai.susi.tools.skillqueryparser.SkillQueryParser;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * This Servlet gives a API Endpoint to list all the Skills given its model,
 * group and language. Can be tested on
 * http://127.0.0.1:4000/cms/getSkillList.json Other params are - applyFilter,
 * filter_type, filter_name, count This servlet also gives an API endpoint to
 * list all the private skills if the access_token is given. Can be tested on
 * http://127.0.0.1:4000/cms/getSkillList.json?private=1
 * http://127.0.0.1:4000/cms/getSkillList.json?group=All&language=en&applyFilter=true&filter_name=descending&filter_type=rating&reviewed=true&staff_picks=false&q=testtt&search_type=skill_name,%20descriptions,%20examples,%20author
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {

        String userId = null;
        Boolean shouldReturnSkillStats = false;
        Boolean shouldReturnSkillDataOverTime = false;
        String privateSkill = call.get("private", null);
        if (call.get("access_token", null) != null) { // access tokens can be used by api calls, somehow the stateless
                                                      // equivalent of sessions for browsers
            ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, call.get("access_token", null));
            Authentication authentication = DAO.getAuthentication(credential);
            // check if access_token is valid
            if (authentication.getIdentity() != null) {
                ClientIdentity identity = authentication.getIdentity();
                Authorization authorization = DAO.getAuthorization(identity);
                String userRole = authorization.getUserRole().toString().toLowerCase();
                if (userRole.equals("user") || userRole.equals("reviewer")) {
                    shouldReturnSkillStats = false;
                    shouldReturnSkillDataOverTime = false;
                } else {
                    shouldReturnSkillStats = true;
                    shouldReturnSkillDataOverTime = true;
                }
                userId = identity.getUuid();
            }
        }

        if (privateSkill != null) {
            if (userId != null) {
                JsonTray chatbot = DAO.chatbot;
                JSONObject result = new JSONObject();
                JSONObject userObject = new JSONObject();
                JSONArray botDetailsArray = new JSONArray();

                userObject = chatbot.getJSONObject(userId);
                JSONObject groupObject = new JSONObject();
                JSONObject languageObject = new JSONObject();

                Iterator<String> groupNames = userObject.keys();
                List<String> groupnameKeysList = new ArrayList<String>();

                while (groupNames.hasNext()) {
                    String key = (String) groupNames.next();
                    groupnameKeysList.add(key);
                }

                for (String group_name : groupnameKeysList) {
                    groupObject = userObject.getJSONObject(group_name);
                    Iterator<String> languageNames = groupObject.keys();
                    List<String> languagenameKeysList = new ArrayList<String>();

                    while (languageNames.hasNext()) {
                        String key = (String) languageNames.next();
                        languagenameKeysList.add(key);
                    }

                    for (String language_name : languagenameKeysList) {
                        languageObject = groupObject.getJSONObject(language_name);
                        Iterator<String> skillNames = languageObject.keys();
                        List<String> skillnamesKeysList = new ArrayList<String>();

                        while (skillNames.hasNext()) {
                            String key = (String) skillNames.next();
                            skillnamesKeysList.add(key);
                        }

                        for (String skill_name : skillnamesKeysList) {
                            JSONObject botDetails = languageObject.getJSONObject(skill_name);
                            botDetails.put("name", skill_name);
                            botDetails.put("language", language_name);
                            botDetails.put("group", group_name);
                            botDetailsArray.put(botDetails);
                            result.put("chatbots", botDetailsArray);
                        }
                    }
                }

                if (result.length() == 0) {
                    result.put("accepted", false);
                    result.put("message", "User has no chatbots.");
                    return new ServiceResponse(result);
                }

                result.put("accepted", true);
                result.put("message", "All chatbots of user fetched.");
                return new ServiceResponse(result);
            }
        }

        SkillQuery skillQuery = SkillQueryParser.Builder.getInstance().group("All").build().parse(call);
        String language_list = skillQuery.getLanguage();
        String model_name = skillQuery.getModel();
        String group_name = skillQuery.getGroup();

        String search_type_list = call.get("search_type", "");
        int duration = call.get("duration", -1);
        int avg_rating = call.get("avg_rating", -1);
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject(true);
        JSONObject skillObject = new JSONObject();
        String countString = call.get("count", null);
        int offset = call.get("offset", 0);
        String q = call.get("q", null);
        int page = call.get("page", 0);
        Integer count = null;
        Boolean countFilter = false;
        Boolean dateFilter = false;
        Boolean searchFilter = false;
        String reviewed = call.get("reviewed", "false");
        String staff_picks = call.get("staff_picks", "false");
        String[] language_names = language_list.split(","); for (int i = 0; i < language_names.length; i++) language_names[i] = language_names[i].trim();
        String[] search_types = search_type_list.split(","); for (int i = 0; i < search_types.length; i++) search_types[i] = search_types[i].trim();
        // Initialises the stat counters
        initStatCounters();

        if (!(reviewed.equals("true") || reviewed.equals("false"))) {
            throw new APIException(400, "Bad service call.");
        }

        if (!(staff_picks.equals("true") || staff_picks.equals("false"))) {
            throw new APIException(400, "Bad service call.");
        }

        if (countString != null) {
            if (Integer.parseInt(countString) < 0) {
                throw new APIException(422, "Invalid count value. It should be positive.");
            } else {
                try {
                    count = Integer.parseInt(countString);
                    offset = page * count;
                    countFilter = true;
                } catch (NumberFormatException ex) {
                    throw new APIException(422, "Invalid count value.");
                }
            }
        }

        Pattern searchQuery = null;
        if (q != null) q = q.trim().toLowerCase();
        if (q != null && q.length() > 0) {
            searchFilter = true;
            searchQuery = Pattern.compile(".*" + q + ".*");
        }

        // Returns susi skills list of all groups
        if (group_name.equals("All")) {
            File allGroup = skillQuery.getModelPath().toFile();
            ArrayList<String> folderList = new ArrayList<String>();
            listFoldersForFolder(allGroup, folderList);
            json.put("accepted", false);

            for (String temp_group_name : folderList) {
                Path group = IO.resolvePath(skillQuery.getModelPath(), temp_group_name);
                for (String language_name : language_names) {
                    File language = IO.resolvePath(group, language_name).toFile();
                    ArrayList<String> fileList = new ArrayList<String>();
                    listFilesForFolder(language, fileList);
                    for (String skill_name : fileList) {
                        skill_name = skill_name.replace(".txt", "");
                        JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, temp_group_name, language_name,
                                skill_name, duration);

                        if (shouldReturnSkillInResponse(skillMetadata, reviewed, staff_picks)) {
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
            Path group = IO.resolvePath(skillQuery.getModelPath(), group_name);
            for (String language_name : language_names) {
                File language = IO.resolvePath(group, language_name).toFile();
                json.put("accepted", false);
                ArrayList<String> fileList = new ArrayList<String>();
                listFilesForFolder(language, fileList);
                for (String skill_name : fileList) {
                    skill_name = skill_name.replace(".txt", "");
                    JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, group_name, language_name,
                            skill_name, duration);
                    if (shouldReturnSkillInResponse(skillMetadata, reviewed, staff_picks)) {
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
            } else if (filter_type.equals("rating")) {
                DAO.sortByMostRating(jsonValues, filter_name.equals("ascending"));
            } else if (filter_type.equals("usage")) {
                DAO.sortByUsageCount(jsonValues, filter_name.equals("ascending"));
            } else if (filter_type.equals("feedback")) {
                DAO.sortByFeedbackCount(jsonValues, filter_name.equals("ascending"));
            }
            else if (filter_type.equals("top_rated")) {
                DAO.sortByTopRating(jsonValues, filter_name.equals("ascending"));
            }

            int countTillOffset = 0;
            filterloop: for (int i = 0; i < jsonArray.length(); i++) {
                if (avg_rating > 0) {
                    Object skill_rating = jsonValues.get(i).opt("skill_rating");
                    if (skill_rating == null || !((skill_rating instanceof JSONObject)))
                        skill_rating = new JSONObject().put("stars", new JSONObject().put("avg_star", 0.0f));
                    JSONObject starsObject = ((JSONObject) skill_rating).getJSONObject("stars");
                    float avg_stars = starsObject.getFloat("avg_star");
                    if (Float.compare(avg_rating, avg_stars) > 0) {
                        continue;
                    }
                }
                if (dateFilter && duration > 0) {
                    long durationInMillisec = TimeUnit.DAYS.toMillis(duration);
                    long timestamp = System.currentTimeMillis() - durationInMillisec;
                    String startDate = new Timestamp(timestamp).toString().substring(0, 10); // substring is used for
                                                                                             // getting timestamp upto
                                                                                             // date only
                    if (filter_type.equals("creation_date")) {
                        String skillCreationDate = jsonValues.get(i).get("creationTime").toString().substring(0, 10);
                        if (skillCreationDate.compareToIgnoreCase(startDate) < 0)
                            continue;
                    } else if (filter_type.equals("modified_date")) {
                        String skillModifiedDate = jsonValues.get(i).get("lastModifiedTime").toString().substring(0,
                                10);
                        if (skillModifiedDate.compareToIgnoreCase(startDate) < 0)
                            continue;
                    }
                }
                if (searchFilter) {
                    boolean found = false;
                    fieldsloop: for (String searchType: search_types ) {
                        JSONObject skillMetadata = jsonValues.get(i);
                        if (searchType.equals("skill_name") || searchType.equals("author") || searchType.equals("descriptions")) {
                            String skillData = skillMetadata.get(searchType).toString().toLowerCase();
                            if (searchQuery.matcher(skillData).matches()) {found = true; break fieldsloop;}
                            continue fieldsloop;
                        }
                        if (searchType.equals("examples")) {
                            Set<String> examples = (Set<String>) skillMetadata.get("examples");
                            Iterator<String> itr = examples.iterator();
                            while (itr.hasNext()) {
                                if (searchQuery.matcher(itr.next()).matches()) {found = true; break fieldsloop;}
                            }
                            continue fieldsloop;
                        }
                        //continues fieldsloop..
                    }
                    if (!found) continue filterloop;
                }
                if (countTillOffset++ < offset) {
                    continue;
                }
                if (countFilter) {
                    if (count == 0) {
                        break;
                    } else {
                        count--;
                    }
                }
                filteredData.put(jsonValues.get(i));
            }
            if (countFilter) {
                try {
                    count = Integer.parseInt(countString);
                    int pageCount = jsonArray.length() % count == 0 ? (jsonArray.length() / count)
                            : (jsonArray.length() / count) + 1;
                    json.put("pageCount", pageCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            json.put("filteredData", filteredData);
        } else {
            if (countFilter) {
                JSONObject tempSkillObject = new JSONObject();
                tempSkillObject = skillObject;
                for (int i = 0; i < skillObject.length(); i++) {
                    if (i < offset) {
                        continue;
                    }
                    if (count == 0) {
                        break;
                    } else {
                        count--;
                    }
                    String keyName = skillObject.names().getString(i);
                    tempSkillObject.put(keyName, skillObject.getJSONObject(keyName));
                }
                if (countFilter) {
                    try {
                        count = Integer.parseInt(countString);
                        int pageCount = skillObject.length() % count == 0 ? (skillObject.length() / count)
                                : (skillObject.length() / count) + 1;
                        json.put("pageCount", pageCount);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                skillObject = tempSkillObject;
            }
        }

        if (shouldReturnSkillStats) {
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

        if(shouldReturnSkillDataOverTime) {
            JSONObject lastModifiedOverTimeObj = new JSONObject();
            JSONObject creationOverTimeObj = new JSONObject();
            JSONObject lastAccessOverTimeObj = new JSONObject();
            for(String skillKey: Objects.requireNonNull(JSONObject.getNames(skillObject))){
                JSONObject skillObj = skillObject.getJSONObject(skillKey);
                if(skillObj.has("lastModifiedTime")) {
                    String lastModifiedTime = skillObj.get("lastModifiedTime").toString().substring(0, 7);
                    if(lastModifiedOverTimeObj.has(lastModifiedTime)) {
                        lastModifiedOverTimeObj.put(lastModifiedTime, lastModifiedOverTimeObj.getInt(lastModifiedTime) + 1);
                    }
                    else {
                        lastModifiedOverTimeObj.put(lastModifiedTime, 1);
                    }
                }
                if(skillObj.has("creationTime")) {
                    String creationOverTime = skillObj.get("creationTime").toString().substring(0, 7);
                    if(creationOverTimeObj.has(creationOverTime)) {
                        creationOverTimeObj.put(creationOverTime, creationOverTimeObj.getInt(creationOverTime) + 1);
                    }
                    else {
                        creationOverTimeObj.put(creationOverTime, 1);
                    }
                }
                if(skillObj.has("lastAccessTime")) {
                    String lastAccessTime = skillObj.get("lastAccessTime").toString().substring(0, 7);
                    if(lastAccessOverTimeObj.has(lastAccessTime)) {
                        lastAccessOverTimeObj.put(lastAccessTime, lastAccessOverTimeObj.getInt(lastAccessTime) + 1);
                    }
                    else {
                        lastAccessOverTimeObj.put(lastAccessTime, 1);
                    }
                }

            }
            List<JSONObject> lastModifiedOverTimeList = new ArrayList<JSONObject>();
            List<JSONObject> creationOverTimeList = new ArrayList<JSONObject>();
            List<JSONObject> lastAccessOverTimeList = new ArrayList<JSONObject>();
            for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(lastModifiedOverTimeObj))){
              JSONObject timeObj = new JSONObject();
              timeObj.put("timeStamp", timeStamp);
              timeObj.put("count", lastModifiedOverTimeObj.getInt(timeStamp));
              lastModifiedOverTimeList.add(timeObj);
            }
            for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(creationOverTimeObj))){
                JSONObject timeObj = new JSONObject();
                timeObj.put("timeStamp", timeStamp);
                timeObj.put("count", creationOverTimeObj.getInt(timeStamp));
                creationOverTimeList.add(timeObj);
            }
            for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(lastAccessOverTimeObj))){
                JSONObject timeObj = new JSONObject();
                timeObj.put("timeStamp", timeStamp);
                timeObj.put("count", lastAccessOverTimeObj.getInt(timeStamp));
                lastAccessOverTimeList.add(timeObj);
            }
            json.put("lastModifiedOverTime", lastModifiedOverTimeList);
            json.put("creationOverTime", creationOverTimeList);
            json.put("lastAccessOverTime", lastAccessOverTimeList);
        }

        json.put("model", model_name).put("group", group_name).put("language", language_list);
        json.put("skills", skillObject);
        json.put("accepted", true);
        json.put("message", "Success: Fetched skill list");
        return new ServiceResponse(json);

    }

    private static Boolean shouldReturnSkillInResponse(JSONObject skillMetadata, String reviewed, String staff_picks) {
        totalSkills++;

        if (skillMetadata.getBoolean("editable") == true) {
            editableSkills++;
        } else {
            nonEditableSkills++;
        }

        if (skillMetadata.getBoolean("reviewed") == true) {
            reviewedSkills++;
        } else {
            nonReviewedSkills++;
        }

        if (skillMetadata.getBoolean("staffPick") == true) {
            staffPicks++;
        }

        if (skillMetadata.getBoolean("systemSkill") == true) {
            systemSkills++;
        }

        if (reviewed.equals("true") && skillMetadata.getBoolean("reviewed") == false) {
            return false;
        }

        if (staff_picks.equals("true") && skillMetadata.getBoolean("staffPick") == false) {
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
