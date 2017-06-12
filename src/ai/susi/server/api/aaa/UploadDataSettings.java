package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by dravit on 12/6/17.
 * Servlet to upload file to data/settings folder
 */
public class UploadDataSettings extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/data/settings/upload";
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
        try {
            doPost(post.getRequest(), response);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
