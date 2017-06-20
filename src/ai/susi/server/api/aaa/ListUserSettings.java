package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to read user setting
 * test locally at http://127.0.0.1:4000/aaa/listUserSettings.json
 */
public class ListUserSettings extends AbstractAPIHandler implements APIHandler {
    @Override
    public String getAPIPath() {
        return "/aaa/listUserSettings.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        Accounting accouting = DAO.getAccounting(authorization.getIdentity());
        return new ServiceResponse(accouting.getJSON());

    }
}