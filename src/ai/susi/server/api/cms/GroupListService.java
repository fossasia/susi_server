package ai.susi.server.api.cms;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by DravitLochan on 6/6/17.
 */
public class GroupListService extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return null;
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return null;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        return null;
    }
}
