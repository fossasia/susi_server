package ai.susi.server.api.cms;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;

import javax.servlet.http.HttpServletResponse;

public class TopMenuService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 1839868262296635665L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/topmenu.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        
        JSONObject json = new JSONObject(true);
        JSONArray topmenu = new JSONArray()
            .put(new JSONObject().put("Home", "index.html"))
            .put(new JSONObject().put("API", "api.html"))
            .put(new JSONObject().put("Account", "apps/applist/index.html"));
        json.put("items", topmenu);
        
        // modify caching
        json.put("$EXPIRES", 600);
        return json;
    }
}
