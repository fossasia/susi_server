package org.loklak.api.cms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.tools.storage.JSONObjectWithDefault;

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
    public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JSONObjectWithDefault permissions) {
        
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
