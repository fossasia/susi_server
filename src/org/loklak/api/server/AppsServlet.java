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

package org.loklak.api.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;

public class AppsServlet extends HttpServlet {

    private static final long serialVersionUID = -2577184683745091648L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);

        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        post.setResponse(response, "application/javascript");
        
        // generate json
        File apps = new File(DAO.html_dir, "apps");
        JSONObject json = new JSONObject(true);
        JSONArray app_array = new JSONArray();
        json.put("apps", app_array);
        for (String appname: apps.list()) try {
            File apppath = new File(apps, appname);
            if (!apppath.isDirectory()) continue;
            Set<String> files = new HashSet<>();
            for (String f: apppath.list()) files.add(f);
            if (!files.contains("index.html")) continue;
            if (!files.contains("app.json")) continue;
            File json_ld_file = new File(apppath, "app.json");
            Map<String, Object> json_ld_map = DAO.jsonMapper.readValue(json_ld_file, DAO.jsonTypeRef);
            JSONObject json_ld = new JSONObject(json_ld_map);
            app_array.put(json_ld);
        } catch (Throwable e) {
            Log.getLog().warn(e);
        }
        
        // write json
        response.setCharacterEncoding("UTF-8");
        FileHandler.setCaching(response, 60);
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString(2));
        if (jsonp) sos.println(");");
        sos.println();

        post.finalize();
    }
    
}
