package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to read user setting
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
            Accounting accouting = DAO.getAccounting(authorization.getIdentity());
            JSONObject result = accouting.getJSON();
            result.put("accepted", true);
            result.put("message", "Success: Showing User settings");
            return new ServiceResponse(result);
        } else {
            throw new APIException(400, "Specified User Setting not found, ensure you are logged in");
        }

    }
}
