package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 * Can be tested on 127.0.0.1:4000/cms/getSkillList.json
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        JSONObject skillObject = new JSONObject();
        ArrayList<String> fileList = new ArrayList<String>();
        fileList = listFilesForFolder(language, fileList);
        JsonTray skillRating = DAO.skillRating;


        JSONArray jsonArray = new JSONArray();

        for (String skill : fileList) {
            System.out.println(skill);
            JSONObject skillMetadata = new JSONObject();
            skill = skill.replace(".txt", "");
            skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
            skillMetadata.put("descriptions", JSONObject.NULL);
            skillMetadata.put("image", JSONObject.NULL);
            skillMetadata.put("author", JSONObject.NULL);
            skillMetadata.put("author_url", JSONObject.NULL);
            skillMetadata.put("skill_name", JSONObject.NULL);
            skillMetadata.put("terms_of_use", JSONObject.NULL);
            skillMetadata.put("dynamic_content", false);
            skillMetadata.put("examples", JSONObject.NULL);
            skillMetadata.put("skill_rating", JSONObject.NULL);
            for (Map.Entry<String, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
                String path = entry.getKey();
                if ((path.indexOf("/" + model_name + "/") > 0)
                        && (path.indexOf("/" + group_name + "/") > 0) &&
                        (path.indexOf("/" + language_name + "/") > 0) &&
                        (path.indexOf("/" + skill + ".txt") > 0)) {
                    skillMetadata.put("developer_privacy_policy", entry.getValue().getDeveloperPrivacyPolicy() == null ? JSONObject.NULL : entry.getValue().getDeveloperPrivacyPolicy());
                    skillMetadata.put("descriptions", entry.getValue().getDescription() == null ? JSONObject.NULL : entry.getValue().getDescription());
                    skillMetadata.put("image", entry.getValue().getImage() == null ? JSONObject.NULL : entry.getValue().getImage());
                    skillMetadata.put("author", entry.getValue().getAuthor() == null ? JSONObject.NULL : entry.getValue().getAuthor());
                    skillMetadata.put("author_url", entry.getValue().getAuthorURL() == null ? JSONObject.NULL : entry.getValue().getAuthorURL());
                    skillMetadata.put("skill_name", entry.getValue().getSkillName() == null ? JSONObject.NULL : entry.getValue().getSkillName());
                    skillMetadata.put("terms_of_use", entry.getValue().getTermsOfUse() == null ? JSONObject.NULL : entry.getValue().getTermsOfUse());
                    skillMetadata.put("dynamic_content", entry.getValue().getDynamicContent());
                    skillMetadata.put("examples", entry.getValue().getExamples() == null ? JSONObject.NULL : entry.getValue().getExamples());
                }
            }

            if (skillRating.has(model_name)) {
                JSONObject modelName = skillRating.getJSONObject(model_name);
                if (modelName.has(group_name)) {
                    JSONObject groupName = modelName.getJSONObject(group_name);
                    if (groupName.has(language_name)) {
                        JSONObject languageName = groupName.getJSONObject(language_name);
                        if (languageName.has(skill)) {
                            JSONObject skillName = languageName.getJSONObject(skill);
                            skillMetadata.put("skill_rating", skillName);
                        }
                    }
                }
            }
            jsonArray.put(skillMetadata);
            skillObject.put(skill, skillMetadata);
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

    ArrayList<String> listFilesForFolder(final File folder, ArrayList<String> fileList) {

        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            for (final File fileEntry : filesInFolder) {
                if (!fileEntry.isDirectory() && !fileEntry.getName().startsWith(".")) {
                    fileList.add(fileEntry.getName() + "");
                }
            }
        }
        return fileList;
    }
}
