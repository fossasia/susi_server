package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by DravitLochan on 6/6/17.
 * Servlet to load list of groups for given model
 * Start the server and type in the below given address to test it.
 *  http://127.0.0.1:4000/cms/getgroup.json
 */
public class GroupListService extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/cms/getgroup.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);

        String[] groups = model.list();
        JSONArray groupsArray = new JSONArray(groups);
        return new ServiceResponse(groupsArray);
    }
}
