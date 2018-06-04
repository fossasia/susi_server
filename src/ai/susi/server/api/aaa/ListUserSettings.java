package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to read user setting and connected devices
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
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            JSONObject result = accounting.getJSON();
            result.put("accepted", true);
            result.put("message", "Success: Showing user data");
            accounting.commit();
            return new ServiceResponse(result);
        } else {
            throw new APIException(400, "Specified user data not found, ensure you are logged in");
        }

    }
}
