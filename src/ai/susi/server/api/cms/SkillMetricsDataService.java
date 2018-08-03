package ai.susi.server.api.cms;
/**
 *  SkillMetricsDataService
 *  Copyright by Akshat Garg, @akshatnitd
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This Servlet gives a API Endpoint to list all the Skills based on standard metrics given its model and language.
 * Can be tested on http://127.0.0.1:4000/cms/getSkillMetricsData.json
 * Other params are - duration, count
 */

public class SkillMetricsDataService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8104628449581898341L;

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
        return "/cms/getSkillMetricsData.json";
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
        JSONArray staffPicks = new JSONArray();
        JSONObject json = new JSONObject(true);
        JSONObject skillObject = new JSONObject();
        String countString = call.get("count", null);
        String metrics_list = call.get("metrics", "Games, Trivia and Accessories");
        String[] metrics_names = metrics_list.split(";");
        Integer count = null;
        String[] language_names = language_list.split(",");

        if(countString != null) {
            if(Integer.parseInt(countString) < 0) {
                throw new APIException(422, "Invalid count value. It should be positive.");
            } else {
                try {
                    count = Integer.parseInt(countString);
                } catch(NumberFormatException ex) {
                    throw new APIException(422, "Invalid count value.");
                }
            }
        } else {
            count = 10;
        }

        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.severe(e.getMessage());
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

                        if(SusiSkill.isStaffPick(model_name, temp_group_name, language_name, skill_name)) {
                            staffPicks.put(skillMetadata);
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
                    JSONObject skillMetadata = SusiSkill.getSkillMetadata(model_name, group_name, language_name, skill_name, duration);

                    jsonArray.put(skillMetadata);
                    skillObject.put(skill_name, skillMetadata);

                    if(SusiSkill.isStaffPick(model_name, group_name, language_name, skill_name)) {
                        staffPicks.put(skillMetadata);
                    }
                }
            }
        }


        JSONObject skillMetrics = new JSONObject(true);
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        List<JSONObject> staffPicksList = new ArrayList<JSONObject>();

        // temporary list to extract objects from skillObject
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonValues.add(jsonArray.getJSONObject(i));
        }

        for (int i = 0; i < staffPicks.length(); i++) {
            staffPicksList.add(staffPicks.getJSONObject(i));
        }

        // Get skills based on creation date - Returns latest skills
        SusiSkill.sortByCreationTime(jsonValues, false);

        JSONArray creationDateData = getSlicedArray(jsonValues, count);
        skillMetrics.put("newest", creationDateData);

         // Get skills based on latest modified
        SusiSkill.sortByModifiedTime(jsonValues, false);

        JSONArray modifiedDateData = getSlicedArray(jsonValues, count);
        skillMetrics.put("latest", modifiedDateData);

        // Get skills based on ratings
        SusiSkill.sortByAvgStar(jsonValues, false);

        JSONArray ratingsData = getSlicedArray(jsonValues, count);
        skillMetrics.put("rating", ratingsData);

        // Get skills based on usage count
        SusiSkill.sortByUsageCount(jsonValues, false);

        JSONArray usageData = getSlicedArray(jsonValues, count);
        skillMetrics.put("usage", usageData);

        // Get skills based on feedback count
        SusiSkill.sortByFeedbackCount(jsonValues, false);

        JSONArray feedbackData = getSlicedArray(jsonValues, count);
        skillMetrics.put("feedback", feedbackData);

        // Get skills based on ratings
        SusiSkill.sortByAvgStar(staffPicksList, false);

        JSONArray staffPicksArray = getSlicedArray(staffPicksList, count);
        skillMetrics.put("staffPicks", staffPicksArray);

        for (String metric_name : metrics_names) {
            try {
                metric_name = metric_name.trim();
                List<JSONObject> groupJsonValues = new ArrayList<JSONObject>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).get("group").toString().equals(metric_name)) {
                        groupJsonValues.add(jsonArray.getJSONObject(i));
                    }
                }
                // Get skills based on ratings of a particular group
                SusiSkill.sortByAvgStar(groupJsonValues, false);

                JSONArray topGroup = new JSONArray();
                topGroup = getSlicedArray(groupJsonValues, count);
                skillMetrics.put(metric_name, topGroup);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }


        json.put("model", model_name)
                .put("group", group_name)
                .put("language", language_list);
        json.put("metrics", skillMetrics);
        json.put("accepted", true);
        json.put("message", "Success: Fetched skill data based on metrics");
        return new ServiceResponse(json);
    }

    private JSONArray getSlicedArray(List<JSONObject> jsonValues, Integer count)
    {
        JSONArray slicedArray = new JSONArray();
        for (int i = 0; i < jsonValues.size(); i++) {
            if(count == 0) {
                break;
            } else {
                count --;
            }
            slicedArray.put(jsonValues.get(i));
        }
        return slicedArray;
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
