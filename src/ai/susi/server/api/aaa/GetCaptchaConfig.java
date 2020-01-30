package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonTray;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;

/**
* Fetch reCaptcha config used by SUSI
* It requires user role to be ANONYMOUS or above ANONYMOUS
* example:
* http://localhost:4000/aaa/getCaptchaConfig.json
*/

public class GetCaptchaConfig extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -7872551914189898030L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/getCaptchaConfig.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {
        JsonTray CaptchaConfig = DAO.captchaConfig;
        JSONObject result = new JSONObject();
        JSONObject captchaObj = CaptchaConfig.has("config") ? CaptchaConfig.getJSONObject("config") : new JSONObject();
        try {
            result.put("accepted", true);
            result.put("captchaConfig", captchaObj);
            result.put("message", "Success : Fetched reCaptcha config successfully !");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed : Unable to fetch reCaptcha config!");
        }
    }
}
