package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;

/**
 This Servlet gives a API Endpoint to list all the Skills given its model, group and language.
 Can be tested on 127.0.0.1:4000/cms/getSkillList.json
 */

public class ListSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8691003678852307876L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);

        ArrayList<String> fileList = new ArrayList<String>();
        fileList =  listFilesForFolder(language, fileList);
        JSONArray jsArray = new JSONArray(fileList);

        JSONObject json = new JSONObject(true)
                .put("model", model_name)
                .put("group", group_name)
                .put("language", language_name)
                .put("skills", jsArray);
        return new ServiceResponse(json);

    }

    ArrayList<String> listFilesForFolder(final File folder,  ArrayList<String> fileList) {

        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            for (final File fileEntry : filesInFolder) {
                if (!fileEntry.isDirectory()) {
                    fileList.add(fileEntry.getName()+"");
                }
            }
        }
        return  fileList;
    }
}
