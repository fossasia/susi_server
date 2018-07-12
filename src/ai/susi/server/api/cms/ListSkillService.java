package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.Folder;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 * Can be tested on http://127.0.0.1:4000/cms/getSkillList.json
 * Other params are - applyFilter, filter_type, filter_name, count
 */

public class ListSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8691003678852307876L;

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
        Integer count = null;
        Boolean countFilter = false;
        Boolean dateFilter = false;
        Boolean searchFilter = false;
        String[] language_names = language_list.split(",");

        if(countString != null) {
            if(Integer.parseInt(countString) < 0) {
                throw new APIException(422, "Invalid count value. It should be positive.");
            } else {
                countFilter = true;
                try {
                    count = Integer.parseInt(countString);
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
                        JSONObject skillMetadata = SusiSkill.getSkillMetadata(model_name, temp_group_name, language_name, skill_name, duration);

                        jsonArray.put(skillMetadata);
                        skillObject.put(skill_name, skillMetadata);
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
                    JSONObject skillMetadata = SusiSkill.getSkillMetadata(model_name, group_name, language_name, skill_name, duration);

                    jsonArray.put(skillMetadata);
                    skillObject.put(skill_name, skillMetadata);
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

            if (filter_type.equals("date")) {
                dateFilter = true;
                if (filter_name.equals("ascending")) {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {
                        private static final String KEY_NAME = "creationTime";
                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            String valA = new String();
                            String valB = new String();
                            int result = 0;

                            try {
                                valA = a.get(KEY_NAME).toString();
                                valB = b.get(KEY_NAME).toString();
                                result = valA.compareToIgnoreCase(valB);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });

                } else {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {
                        private static final String KEY_NAME = "creationTime";
                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            String valA = new String();
                            String valB = new String();
                            int result = 0;

                            try {
                                valA = a.get(KEY_NAME).toString();
                                valB = b.get(KEY_NAME).toString();
                                result = valB.compareToIgnoreCase(valA);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
            } else if (filter_type.equals("lexicographical")) {
                if (filter_name.equals("ascending")) {

                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        private static final String KEY_NAME = "skill_name";

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            String valA = new String();
                            String valB = new String();

                            try {
                                valA = a.get(KEY_NAME).toString();
                                valB = b.get(KEY_NAME).toString();
                            } catch (JSONException e) {
                                //do nothing
                            }
                            return valA.compareToIgnoreCase(valB);
                        }
                    });

                } else {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        private static final String KEY_NAME = "skill_name";

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            String valA = new String();
                            String valB = new String();

                            try {
                                valA = a.get(KEY_NAME).toString();
                                valB = b.get(KEY_NAME).toString();
                            } catch (JSONException e) {
                                e.getMessage();
                                //do nothing
                            }
                            return valB.compareToIgnoreCase(valA);
                        }
                    });
                }
            }
            else if (filter_type.equals("rating")) {
                if (filter_name.equals("ascending")) {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            float valA;
                            float valB;
                            int result=0;

                            try {
                                valA = a.getJSONObject("skill_rating").getJSONObject("stars").getFloat("avg_star");
                                valB = b.getJSONObject("skill_rating").getJSONObject("stars").getFloat("avg_star");
                                result = Float.compare(valA, valB);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
                else {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            float valA;
                            float valB;
                            int result=0;

                            try {
                                valA = a.getJSONObject("skill_rating").getJSONObject("stars").getFloat("avg_star");
                                valB = b.getJSONObject("skill_rating").getJSONObject("stars").getFloat("avg_star");
                                result = Float.compare(valB, valA);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
            }
            else if (filter_type.equals("usage")) {
                if (filter_name.equals("ascending")) {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            int valA;
                            int valB;
                            int result=0;

                            try {
                                valA = a.getInt("usage_count");
                                valB = b.getInt("usage_count");
                                result = Integer.compare(valA, valB);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
                else {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            int valA;
                            int valB;
                            int result=0;

                            try {
                                valA = a.getInt("usage_count");
                                valB = b.getInt("usage_count");
                                result = Integer.compare(valB, valA);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
            }
            else if (filter_type.equals("feedback")) {
                if (filter_name.equals("ascending")) {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            Integer valA;
                            Integer valB;
                            int result=0;

                            try {
                                valA = a.getJSONObject("skill_rating").getInt("feedback_count");
                                valB = b.getJSONObject("skill_rating").getInt("feedback_count");
                                result = Integer.compare(valA, valB);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
                else {
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            Integer valA;
                            Integer valB;
                            int result=0;

                            try {
                                valA = a.getJSONObject("skill_rating").getInt("feedback_count");
                                valB = b.getJSONObject("skill_rating").getInt("feedback_count");
                                result = Integer.compare(valB, valA);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return result;
                        }
                    });
                }
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
                 if (dateFilter) {
                     long durationInMillisec = TimeUnit.DAYS.toMillis(duration);
                     long timestamp = System.currentTimeMillis() - durationInMillisec;
                     String startDate = new Timestamp(timestamp).toString().substring(0, 10); //substring is used for getting timestamp upto date only
                     String skillCreationDate = jsonValues.get(i).get("creationTime").toString().substring(0,10);
                     if (skillCreationDate.compareToIgnoreCase(startDate) < 0)
                     {
                         continue;
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
                skillObject = tempSkillObject;
            }
        }


        json.put("model", model_name)
                .put("group", group_name)
                .put("language", language_list);
        json.put("skills", skillObject);
        json.put("accepted", true);
        json.put("message", "Success: Fetched skill list");
        return new ServiceResponse(json);

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
