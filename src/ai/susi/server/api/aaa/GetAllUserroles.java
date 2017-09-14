package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dravit on 29/8/17.
 */
public class GetAllUserroles extends AbstractAPIHandler implements APIHandler {
    @Override
    public String getAPIPath() {
        return "/aaa/getAllUserroles.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        List<String> userRoles = new ArrayList<String>();
        for(UserRole userRole : UserRole.values()){
            userRoles.add(userRole.getName());
        }
        json.put("userRoles", userRoles);
        json.put("accepted", true);
        json.put("accepted", "All user roles fetched successfully");
        return new ServiceResponse(json);
    }
}
