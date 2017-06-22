package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by chetankaushik on 22/06/17.
 */
public class GetAllGroups extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -179412273153306443L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getAllGroups.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        JSONObject success = new JSONObject();
        success.put("success", false);
        JSONObject allUsers;
        allUsers = DAO.group.toJSON();
        String model_name = call.get("group", null);
        allUsers.put("success", true);
        return new ServiceResponse(allUsers);
    }

}
