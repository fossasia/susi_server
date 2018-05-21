package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.Folder;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 * Can be tested on http://127.0.0.1:4000/cms/getSkillList.json
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
        String group_name = call.get("group", "Knowledge");
        String language_name = call.get("language", "en");
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject(true);
        JSONObject skillObject = new JSONObject();

        // Returns susi skills list of all groups
        if (group_name.equals("All")) {
            File allGroup = new File(String.valueOf(model));
            ArrayList<String> folderList = new ArrayList<String>();
            listFoldersForFolder(allGroup, folderList);
            json.put("accepted", false);

            for (String temp_group_name : folderList){
                File group = new File(model, temp_group_name);
                File language = new File(group, language_name);
                ArrayList<String> fileList = new ArrayList<String>();
                listFilesForFolder(language, fileList);

                for (String skill_name : fileList) {
                    //System.out.println(skill_name);
                    skill_name = skill_name.replace(".txt", "");
                    JSONObject skillMetadata = SusiSkill.getSkillMetadata(model_name, temp_group_name, language_name, skill_name);

                    jsonArray.put(skillMetadata);
                    skillObject.put(skill_name, skillMetadata);
                }
            }

        }
        // Returns susi skills list of a particular group
        else {
            File group = new File(model, group_name);
            File language = new File(group, language_name);
            json.put("accepted", false);
            ArrayList<String> fileList = new ArrayList<String>();
            listFilesForFolder(language, fileList);

            for (String skill_name : fileList) {
                //System.out.println(skill_name);
                skill_name = skill_name.replace(".txt", "");
                JSONObject skillMetadata = SusiSkill.getSkillMetadata(model_name, group_name, language_name, skill_name);

                jsonArray.put(skillMetadata);
                skillObject.put(skill_name, skillMetadata);
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
                if (filter_name.equals("ascending")) {

                } else {

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
                            return valA.compareTo(valB);
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
                                //do nothing
                            }
                            return valB.compareTo(valA);
                        }
                    });
                }
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                filteredData.put(jsonValues.get(i));
            }
            json.put("filteredData", filteredData);
        }


        json.put("model", model_name)
                .put("group", group_name)
                .put("language", language_name);
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
