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
 * Created by chetankaushik on 31/05/17.
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
    public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions)
            throws APIException {
        if (rights.getBaseUserRole().toString().equals("ADMIN")) {
            JSONObject result = new JSONObject();

            Iterator<?> keys = DAO.authorization.toJSON().keys();
            Iterator keysToCopyIterator = DAO.authorization.toJSON().keys();
            List<String> keysList = new ArrayList<String>();
            while (keysToCopyIterator.hasNext()) {
                String key = (String) keysToCopyIterator.next();
                keysList.add(key);
            }
            String[] kesyArray = keysList.toArray(new String[keysList.size()]);
            result.put("users", DAO.authorization.toJSON());
            result.put("username", kesyArray);
            return result;

        }
        else
            throw new APIException(403, "The server understood the request but refuses to authorize it");
    }




}
