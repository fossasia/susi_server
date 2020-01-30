package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonTray;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;

/**
* Fetch reCaptcha config used by SUSI
* It requires user role to be ADMIN or above
* key can be: 'login', 'signUp', 'changePassword'
* example:
* http://localhost:4000/aaa/captchaConfigService.json?value=false&key=signUp
*/

public class CaptchaConfigService extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -7872551914189898030L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/captchaConfigService.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {
        JsonTray CaptchaConfig = DAO.captchaConfig;
        JSONObject result = new JSONObject();
        JSONObject captchaObj = CaptchaConfig.has("config") ? CaptchaConfig.getJSONObject("config") : new JSONObject();
        String key = call.get("key", null);
        String value = call.get("value", null);
        if (key == null || value == null) {
            throw new APIException(400, "Bad Request. No parameter present");
        }
        captchaObj.put(key, value);
        CaptchaConfig.put("config", captchaObj, true);
        try {
            result.put("accepted", true);
            result.put("message", "Success : Updated reCaptcha config!");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed : Unable to fetch reCaptcha config!");
        }
    }
}
