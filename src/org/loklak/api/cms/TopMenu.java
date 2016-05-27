package org.loklak.api.cms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;

public class TopMenu extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 1839868262296635665L;


    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(Authorization auth) {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public String getAPIPath() {
        return "/cms/topmenu.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query call, Authorization rights) {
        
        int limited_count = (int) DAO.getConfig("download.limited.count", (long) Integer.MAX_VALUE);
    
        JSONObject json = new JSONObject(true);
        JSONArray topmenu = new JSONArray()
            .put(new JSONObject().put("Home", "index.html"))
            .put(new JSONObject().put("About", "about.html"))
            .put(new JSONObject().put("Showcase", "showcase.html"))
            .put(new JSONObject().put("Architecture", "architecture.html"))
            .put(new JSONObject().put("Download", "download.html"))
            .put(new JSONObject().put("Tutorials", "tutorials.html"))
            .put(new JSONObject().put("API", "api.html"));
        if (limited_count > 0) topmenu.put(new JSONObject().put("Dumps", "dump.html"));
        topmenu.put(new JSONObject().put("Apps", "apps/"));
        json.put("items", topmenu);
        
        // modify caching
        json.put("$EXPIRES", 600);
        return json;
    }
    
}
