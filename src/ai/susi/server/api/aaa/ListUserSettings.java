package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to read user settings
 * example:
 * http://localhost:4000/aaa/listUserSettings.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class ListUserSettings extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -1972211109199355750L;

    @Override
    public String getAPIPath() {
        return "/aaa/listUserSettings.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {
        if ( authorization.getIdentity()!=null ) {
            String email = query.get("email", null);
            Accounting accounting;
            UserRole userRole = authorization.getUserRole();
            if((userRole.getName().equals("admin") || userRole.getName().equals("superadmin")) && email != null) {
                ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, email);
                Authorization userAuthorization = DAO.getAuthorization(identity);
                accounting = DAO.getAccounting(userAuthorization.getIdentity());
            } else {
                accounting = DAO.getAccounting(authorization.getIdentity());
            }
            JSONObject result = new JSONObject(accounting.getJSON().toMap()); // make a clone
            JSONObject responseObj = new JSONObject();
            if(result.has("settings")) {
                responseObj.put("settings", result.getJSONObject("settings"));
            } else {
                responseObj.put("settings", new JSONObject());
            }
            responseObj.put("accepted", true);
            responseObj.put("message", "Success: Showing user settings");
            return new ServiceResponse(responseObj);
        } else {
            throw new APIException(401, "Specified user data not found, ensure you are logged in");
        }

    }
}
