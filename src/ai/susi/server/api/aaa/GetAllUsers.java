package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.UserRole;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 Created by chetankaushik on 31/05/17.
 This Servlet gives a API Endpoint to list all the users and their roles.
 It requires user role to be ADMIN or above ADMIN
 example:
 http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ
 Necessary parameters : access_token
 Other parameters (one out of two is necessary):
 getPageCount -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getPageCount=true
 getUserCount -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getUserCount=true
 page         -> integer http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&page=2
 */
public class GetAllUsers extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4538304346942632187L;

    @Override
    public String getAPIPath() {
        return "/aaa/getUsers.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        if(call.get("getPageCount", false) ==false && call.get("page", null) == null
                                                                && call.get("getUserCount", null) == null) {
            throw new APIException(422, "Bad Request. No parameter present");
        }
        JSONObject result = new JSONObject(true);
        result.put("accepted", false);
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<String> keysList = new ArrayList<String>();
        authorized.forEach(client -> keysList.add(client.toString()));
        String[] keysArray = keysList.toArray(new String[keysList.size()]);
        if(call.get("getPageCount", false) == true) {
            int pageCount = keysArray.length % 50 ==0 ? (keysArray.length/50) :  (keysArray.length/50) + 1;
            result.put("pageCount", pageCount);
            result.put("accepted", true);
            result.put("message", "Success: Fetched count of pages");
            return new ServiceResponse(result);
        }
        if(call.get("getUserCount", false) == true) {
            result.put("userCount", keysArray.length);
            result.put("accepted", true);
            result.put("message", "Success: Fetched count of users");
            return new ServiceResponse(result);
        }
        else {
            int page = call.get("page", 0);
            page = (page - 1) * 50;
            List<JSONObject> userList = new ArrayList<JSONObject>();
            authorized.forEach(client -> userList.add(client.toJSON()));
            List<JSONObject> currentPageUsers = new ArrayList<JSONObject>();
            int length = userList.size() - page > 50 ? 50 : (userList.size() - page)  ;
            String[] currentKeysArray = Arrays.copyOfRange(keysArray, page, page + length);
            try {
                for(int i = 0; i < length; ++i) {
                    currentPageUsers.add(userList.get(page + i));
                }
            } catch (IndexOutOfBoundsException e) {
                throw new APIException(422, "Requested List does not exists!");
            }
            result.put("users", currentPageUsers);
            result.put("username", currentKeysArray);
            result.put("accepted", true);
            result.put("message", "Success: Fetched all Users!");
            return new ServiceResponse(result);
        }
    }

}
