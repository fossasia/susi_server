package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by chetankaushik on 07/06/17.
 */
public class ModifyExpertService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 18344224L;

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
        if(!expert.exists()){
            JSONObject error = new JSONObject();
            error.put("response","Expert Does Not Exists");
            return new ServiceResponse(error);
        }
        String content = call.get("content", "");
        if(expert_name==null){
            JSONObject error = new JSONObject();
            error.put("response","expert Name not given");
            return new ServiceResponse(error);
        }
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
