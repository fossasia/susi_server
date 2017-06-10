package ai.susi.server.api.cms;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by dravit on 10/6/17.
 * API to get list of groups for a given model.
 * test locally at http://127.0.0.1:4000/cms/getgroups.json
 */

public class GroupListService extends AbstractAPIHandler implements APIHandler {
    @Override
    public String getAPIPath() {
        return "/cms/getgroups.json";
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
