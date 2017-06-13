package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by dravit on 14/6/17.
 * Servlet to get list of all the files present in data/settings directory
 * test locally at http://127.0.0.1:4000/aaa/getAllFiles
 */
public class GetAAAFiles extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/aaa/getAllFiles";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        String path = DAO.data_dir.getPath()+"/settings/";
        File settings = new File(path);
        String[] files = settings.list();
        System.out.println(files.length);
        JSONArray fileArray = new JSONArray(files);
        return new ServiceResponse(fileArray);
    }
}
