package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
   Created by chetankaushik on 31/05/17.
   This Servlet gives a API Endpoint to list all the users and their roles.
   It requires a ADMIN login or a ADMIN session
   Can be tested on 127.0.0.1:4000/aaa/getAllUsers.json
 */
public class GetAllUsers extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4538304346942632187L;

    @Override
    public String getAPIPath() {
        return "/aaa/getAllUsers.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject(true);
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<String> keysList = new ArrayList<String>();
        authorized.forEach(client -> keysList.add(client.toString()));
        String[] keysArray = keysList.toArray(new String[keysList.size()]);
        JSONObject users = new JSONObject();
        authorized.forEach(client -> users.put(client.getClient(), client.toJSON()));
        result.put("users", users);
        result.put("username", keysArray);
        result.put("accepted", true);
        result.put("message", "Success: Fetched all Users and their roles");
        return new ServiceResponse(result);
    }

}
