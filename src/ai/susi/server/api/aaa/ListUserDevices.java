package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by akshatnitd on 21/7/19
 * Servlet to read user devices
 * example:
 * http://localhost:4000/aaa/listUserDevices.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class ListUserDevices extends AbstractAPIHandler implements APIHandler {
	private static final long serialVersionUID = 4348119523547572080L;

	@Override
    public String getAPIPath() {
        return "/aaa/listUserDevices.json";
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
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            JSONObject result = new JSONObject(accounting.getJSON().toMap()); // make a clone
            JSONObject responseObj = new JSONObject();
            if(result.has("devices")) {
              responseObj.put("devices", result.getJSONObject("devices"));
            } else {
              responseObj.put("devices", new JSONObject());
            }
            responseObj.put("accepted", true);
            responseObj.put("message", "Success: Showing user devices");
            return new ServiceResponse(responseObj);
        } else {
            throw new APIException(401, "Specified user data not found, ensure you are logged in");
        }

    }
}
