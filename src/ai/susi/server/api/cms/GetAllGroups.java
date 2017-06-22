package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Set;

/**
 * This Servlets returns all the group details of all the groups created.
 * Accepts NO, GET or POST parameters.
 * Can be tested on
 * http://127.0.0.1:4000/cms/getAllGroups.json
 *
 */
public class GetAllGroups extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -179412273153306443L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
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
        allUsers.put("success", true);
        return new ServiceResponse(allUsers);
    }

}
