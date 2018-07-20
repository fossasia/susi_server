/**
 *  TopMenuService
 *  Copyright 22.05.2016 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package ai.susi.server.api.cms;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

public class TopMenuService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 1839868262296635665L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/topmenu.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        
        JSONObject json = new JSONObject(true);
        JSONArray topmenu = new JSONArray()
            .put(new JSONObject().put("Chat", "https://chat.susi.ai/"))
            .put(new JSONObject().put("Skills", "https://skills.susi.ai/"))
            .put(new JSONObject().put("About", "https://chat.susi.ai/overview"))
            .put(new JSONObject().put("Login", "https://accounts.susi.ai/"));
        json.put("items", topmenu);
        json.put("accepted", true);
        json.put("message", "Request processed successfully");
        
        // modify caching
        json.put("$EXPIRES", 600);
        return new ServiceResponse(json);
    }
}
