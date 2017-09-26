package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


/**
 * Created by dravit on 26/9/17.
 */
public class StorePersonalInfoService extends AbstractAPIHandler implements APIHandler {
    @Override
    public String getAPIPath() {
        return "/aaa/storePersonalInfo.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);

        if (post.get("storeName", null) == null) {
            throw new APIException(422, "Bad store name encountered!");
        }

        if(post.get("value", null) == null) {
            throw new APIException(422, "Bad store name value encountered!");
        }

        String storeName = post.get("storeName", null);
        String value = post.get("value", null);

        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Specified User Setting not found, ensure you are logged in");
        } else {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());

            if (accounting.getJSON().has(storeName)) {
                accounting.getJSON().put(storeName, value);
            } else {
                accounting.getJSON().put(storeName, value);
            }
            System.out.println(accounting.getJSON());
            json.put("accepted", true);
            json.put("message", "You successfully updated your account information!");
            return new ServiceResponse(json);
        }
    }
}
