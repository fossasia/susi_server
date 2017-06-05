package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Iterator;
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
        JSONObject result = new JSONObject();
        Iterator<String> keysToCopyIterator = DAO.authorization.toJSON().keys();
        List<String> keysList = new ArrayList<String>();
        while (keysToCopyIterator.hasNext()) {
            String key = (String) keysToCopyIterator.next();
            keysList.add(key);
        }
        String[] keysArray = keysList.toArray(new String[keysList.size()]);
        result.put("users", DAO.authorization.toJSON());
        result.put("username", keysArray);
        return new ServiceResponse(result);
    }

}
