package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by chetankaushik on 07/06/17.
 * This Endpoint accepts 5 parameters. model,group,language,expert,content.
 * before modifying an expert the expert must exist in the directory.
 * !IMPORTANT! --> Content must be URL Encoded
 * http://localhost:4000/cms/modifyExpert.json?model=general&group=knowledge&expert=testing&content=What is stock price of *%3F|What is stock price of *|stock price of *|* stock price of *%0A!console%3A%24l%24%0A{%0A "url"%3A"http%3A%2F%2Ffinance.google.com%2Ffinance%2Finfo%3Fclient%3Dig%26q%3DNASDAQ%3A%241%24"%2C%0A "path"%3A"%24.[0]"%0A}%0Aeol%0A
 */
public class ModifyExpertService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1834363513093189312L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/modifyExpert.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String expert_name = call.get("expert", null);
        File expert = new File(language, expert_name + ".txt");
        // Checking for file existence
        if(!expert.exists()){
            JSONObject error = new JSONObject();
            error.put("response","Expert Does Not Exists");
            return new ServiceResponse(error);
        }
        // Reading Content for expert
        String content = call.get("content", "");
        if(expert_name==null){
            JSONObject error = new JSONObject();
            error.put("response","expert Name not given");
            return new ServiceResponse(error);
        }
        // Writing to File
        try (FileWriter file = new FileWriter(expert)) {
            file.write(content);
            JSONObject success = new JSONObject();
            success.put("response",expert.getName()+" created successfully");
            return new ServiceResponse(success);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
