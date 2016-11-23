/**
 *  RuleLearning
 *  Copyright 22.11.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.learning;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.api.susi.ConsoleService;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.tools.storage.JSONObjectWithDefault;
import org.loklak.tools.storage.JsonTray;

import javax.servlet.http.HttpServletResponse;

public class RuleLearning extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/learning/rule.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization user, final JSONObjectWithDefault permissions) throws APIException {
        JSONObject json = new JSONObject(true).put("accepted", true);
        
        // unauthenticated users are rejected
        if (user.getIdentity().isAnonymous()) {
            json.put("accepted", false);
            json.put("reject-reason", "you must be logged in");
            return json;
        }
        String client = user.getIdentity().getClient();
        
        // to categorize the rule, we use project names and tags. Both can be omitted, in that case the project
        // is named "default" and the tag list is empty. Both methods can later be used to retrieve subsets of the
        // rule set
        String project = post.get("project", "default");
        String[] tagsl  = post.get("tags", "").split(",");
        JSONArray tags = new JSONArray(); for (String t: tagsl) tags.put(t);
        json.put("project", project);
        json.put("tags", tags);
        
        // parameters
        String action = post.get("action", "");
        if (action.length() == 0) {
            json.put("accepted", false);
            json.put("reject-reason", "you must submit a parameter 'action' containing either the word 'list', 'test', 'learn' or 'delete'");
            return json;
        }
        json.put("action", action);

        if (action.equals("list")) {
            Set<String> projectnames = DAO.susi.getRulesetNames(client);
            JSONObject projects = new JSONObject(true);
            for (String p: projectnames) {
                try {
                    JsonTray t = DAO.susi.getRuleset(client, p);
                    projects.put(p, t.toJSON().getJSONObject("console"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            json.put("projects", projects);
        } else {
            String name = post.get("name", "");
            if (name.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "you must submit a parameter 'name' containing the rule name");
                return json;
            }
            json.put("name", name);

            if (action.equals("delete")) {
                try {
                    JsonTray tray = DAO.susi.getRuleset(client, project);
                    if (!tray.has("console")) tray.put("console", new JSONObject(), true);
                    JSONObject console = tray.getJSONObject("console");
                    console.remove(name);
                    tray.commit();                    
                } catch (IOException e) {
                    e.printStackTrace();
                    json.put("accepted", false);
                    json.put("reject-reason", "deletion the console rule causes an error: " + e.getMessage());
                }
                return json;
            }
            
            String serviceURL = post.get("url", ""); // i.e. url=http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20wikigeodata%20WHERE%20place='$query$';
            if (serviceURL.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "url parameter required");
                return json;
            }
            if (!serviceURL.contains("$query$")) {
                json.put("accepted", false);
                json.put("reject-reason", "the url must contain a string $query$");
                return json;
            }
            
            String test = post.get("test", "");
            if (test.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "a testquery parameter is required");
                return json;
            }
            
            // now test the api call
            byte[] serviceResponse = null;
            try {
                serviceResponse = ConsoleService.loadData(serviceURL, test);
            } catch (Exception e) {
                json.put("accepted", false);
                json.put("reject-reason", "the console test load resulted in an error: " + e.getMessage());
                return json;
            }

            // try to find the correct jsonPath automatically
            JSONArray path_computed = new JSONArray();
            Object serviceObject = null;
            try {serviceObject = new JSONObject(new JSONTokener(new ByteArrayInputStream(serviceResponse)));} catch (JSONException e) {}
            if (serviceObject != null && ((JSONObject) serviceObject).length() > 0) {
                for (String s: ((JSONObject) serviceObject).keySet()) {
                    if (((JSONObject) serviceObject).get(s) instanceof JSONArray) path_computed.put("$." + s);
                }
            } else {
                try {serviceObject = new JSONArray(new JSONTokener(new ByteArrayInputStream(serviceResponse)));} catch (JSONException e) {}
                if (serviceObject != null && ((JSONArray) serviceObject).length() > 0) {
                    path_computed.put("$");
                } 
            }
            
            String path = post.get("path", "");
            if (path.length() == 0) {
                if (path_computed.length() == 1) {
                    path = path_computed.getString(0);
                } else {
                    json.put("accepted", false);
                    json.put("reject-reason", "a data parameter containing the jsonpath to the data object is required. " +
                            (path_computed.length() < 1 ? "" : "Suggested paths: " + path_computed.toString(0)));
                    return json;
                }
            }
            
            JSONArray data = ConsoleService.parseJSONPath(new JSONTokener(new ByteArrayInputStream(serviceResponse)), path);
            if (data == null || data.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "the jsonPath from data object did not recognize an array object");
                return json;
            }
            
            // find out the attributes in the data object
            Set<String> a = new LinkedHashSet<>();
            for (int i = 0; i < data.length(); i++) a.addAll(data.getJSONObject(i).keySet());
            for (int i = 0; i < data.length(); i++) {
                Set<String> doa = data.getJSONObject(i).keySet();
                Iterator<String> j = a.iterator();
                while (j.hasNext()) if (!doa.contains(j.next())) j.remove();
            }
            JSONArray attributes = new JSONArray();
            for (String s: a) attributes.put(s);
            json.put("path_computed", path_computed);
            
            // construct a new rule
            JSONObject consolerule = new JSONObject(true);
            consolerule.put("example", "http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20" + name + "%20WHERE%20query=%27" + test + "%27;%22");
            consolerule.put("url", serviceURL);
            consolerule.put("test", test);
            consolerule.put("parser", "json");
            consolerule.put("path", path);
            consolerule.put("attributes", attributes);
            //consolerule.put("author", user.getIdentity().getName());
            //consolerule.put("license", "provided by " + user.getIdentity().getName() + ", licensed as public domain");
            json.put("console", new JSONObject().put(name, consolerule));
            
            // testing was successful, now decide which action to take: report or store
            if (action.equals("test")) {
                json.put("data", data);
                return json;
            }
            
            if (action.equals("learn")) {
                try {
                    JsonTray tray = DAO.susi.getRuleset(client, project);
                    if (!tray.has("console")) tray.put("console", new JSONObject(), true);
                    JSONObject console = tray.getJSONObject("console");
                    console.put(name, consolerule);
                    tray.commit();                    
                } catch (IOException e) {
                    e.printStackTrace();
                    json.put("accepted", false);
                    json.put("reject-reason", "storing the console rule causes an error: " + e.getMessage());
                }
                return json;
            }
        }

        // fail case
        json.put("accepted", false);
        json.put("reject-reason", "no valid action given");
        return json;
    }
    
}
