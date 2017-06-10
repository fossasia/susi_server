package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by saurabh on 7/6/17.
 * This Service creates an expert as per given query.
 * http://localhost:4000/cms/createExpert.txt?model=general&group=knowledge&language=en&expert=whois
 */
public class ExpertCreateService  extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 2461878194569824151L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/createExpert.txt";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String expert_name = call.get("expert", "whois");
        File expert = new File(language, expert_name + ".txt");
        String expertName = expert.getName();
        JSONObject json = new JSONObject(true);

        if (expert.exists()) {
            json.put("Error","The '" + expert + "' already exists.");
        } else {
            try {
                expert.createNewFile();
                json.put("created_file", expertName);
            } catch (IOException e) {
                e.printStackTrace();
                json.put("Error","Unable to create expert.");
            }
        }

        return new ServiceResponse(json);

    }
}