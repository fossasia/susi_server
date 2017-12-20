package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Servlet to load an model from the skill database
 * i.e.
 * http://localhost:4000/cms/getmodel.json
 */
public class ModelListService extends AbstractAPIHandler implements APIHandler  {
	
	private static final long serialVersionUID = -4324399908176445352L;

	@Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getModel.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

            JSONObject json = new JSONObject(true);
            json.put("accepted", false);
            String[] models = DAO.model_watch_dir.list((current, name) -> new File(current, name).isDirectory());
            JSONArray modelsArray = new JSONArray(models);
            json.put("modelsArray", modelsArray);
            json.put("accepted", true);
            return new ServiceResponse(json);
    }
}
