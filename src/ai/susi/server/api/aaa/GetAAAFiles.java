package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by dravit on 14/6/17.
 * Servlet to get list of all the files present in data/settings directory
 */
public class GetAAAFiles extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/aaa/getAllFiles";
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
