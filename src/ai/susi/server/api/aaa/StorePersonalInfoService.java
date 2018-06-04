package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


/**
 * Created by DravitLochan on 26/9/17.
 * This is a servlet which shall be used only for the purpose to store or fetch user names or profile links
 * to a user's social media platforms. These could be Facebook, Linkedin, GitHub etc.
 * all the details are stored as a JSONObject in "stores". Each of the platform, details of which are being saved
 * is represented as a storeName.
 * To update the value, just make the request with updated values.
 * sample request :
 * http://127.0.0.1:4000/aaa/storePersonalInfo.json?storeName=github&value=https://github.com/fossasia
 * To fetch the list of all the stores :
 * http://127.0.0.1:4000/aaa/storePersonalInfo.json?fetchDetails=true
 */

public class StorePersonalInfoService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 3764395861595488179L;

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

        Accounting accounting = DAO.getAccounting(authorization.getIdentity());
        if (post.get("fetchDetails", false)) {
            if (accounting.getJSON().has("stores")){
                JSONObject jsonObject = accounting.getJSON().getJSONObject("stores");
                json.put("stores", jsonObject);
                json.put("accepted", true);
                json.put("message", "details fetched successfully.");
                accounting.commit();
                return new ServiceResponse(json);
            } else {
                throw new APIException(420, "No personal information is added yet.");
            }
        }


        if (post.get("storeName", null) == null) {
            throw new APIException(422, "Bad store name encountered!");
        }

        String storeName = post.get("storeName", null);
        if (post.get("value", null) == null) {
            throw new APIException(422, "Bad store name value encountered!");
        }

        String value = post.get("value", null);

        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Specified User Setting not found, ensure you are logged in");
        } else {
            if (accounting.getJSON().has("stores")) {
                accounting.getJSON().getJSONObject("stores").put(storeName, value);
            } else {
                JSONObject jsonObject = new JSONObject(true);
                jsonObject.put(storeName, value);
                accounting.getJSON().put("stores", jsonObject);
            }
            accounting.commit();

            json.put("accepted", true);
            json.put("message", "You successfully updated your account information!");
            return new ServiceResponse(json);
        }
    }
}
