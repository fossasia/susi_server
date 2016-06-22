/**
 *  AppsServlet
 *  Copyright 08.01.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.cms;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;

public class AppsService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -2577184683745091648L;

    @Override
    public String getAPIPath() {
        return "/api/apps.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public JSONObject serviceImpl(Query query, Authorization auth) throws APIException {

        String categorySelection = query.get("category", "");
        
        // generate json
        File apps = new File(DAO.html_dir, "apps");
        JSONObject json = new JSONObject(true);
        JSONArray app_array = new JSONArray();
        json.put("apps", app_array);
        JSONObject categories = new JSONObject(true);
        for (String appname: apps.list()) try {
            // read app and verify the structure of the app
            File apppath = new File(apps, appname);
            if (!apppath.isDirectory()) continue;
            Set<String> files = new HashSet<>();
            for (String f: apppath.list()) files.add(f);
            if (!files.contains("index.html")) continue;
            if (!files.contains("app.json")) continue;
            File json_ld_file = new File(apppath, "app.json");
            String jsonString = new String(Files.readAllBytes(json_ld_file.toPath()), StandardCharsets.UTF_8);
            JSONObject json_ld = new JSONObject(jsonString);
            
            // translate permissions
            if (json_ld.has("permissions")) {
                String p = json_ld.getString("permissions");
                String[] ps = p.split(",");
                JSONArray a = new JSONArray();
                for (String s: ps) a.put(s);
                json_ld.put("permissions", a);
            }
            
            // check category
            if (json_ld.has("applicationCategory") && json_ld.has("name")) {
                String cname = json_ld.getString("applicationCategory");
                if (categorySelection.length() == 0 || categorySelection.equals(cname)) app_array.put(json_ld);
                String aname = json_ld.getString("name");
                if (!categories.has(cname)) categories.put(cname, new JSONArray());
                JSONArray appnames = categories.getJSONArray(cname);
                appnames.put(aname);
            }
        } catch (Throwable e) {
            Log.getLog().warn(e);
        }
        // write categories
        json.put("categories", categories.keySet().toArray(new String[categories.length()]));
        json.put("category", categories);

        return json;
    }
}
