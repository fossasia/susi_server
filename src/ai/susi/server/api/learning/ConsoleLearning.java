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

package ai.susi.server.api.learning;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonPath;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import ai.susi.server.api.susi.ConsoleService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * rule learning servlet
 * The user shall be able to submit new console rules. This servlet supports the
 * submission of new console rules and takes the user input to store these rules.
 * 
 * IMPORTANT: this servlet only works if the user is logged in. To log in, first use the login app here:
 * http://localhost:4000/apps/loginpage/
 * If you do not have an account, first sign in.
 * 
 * Example:
 * 
 * == (1) Testing a service URL and discovery of the path attribute ==
 * Test a console url and compute the path to the data object - the following attributes must be given:
 * action = test - tells the servlet that this is not a storage process, just a dry-run test
 * name = <name> - the future name of the new rule
 * url = <url>   - the url to the remote json service which will be used to retrieve information. It must contain a $query$ string.
 * test = <parameter> - the parameter that will replace the $query$ string inside the given url. It is required to test the service.
 * i.e.:
 * http://127.0.0.1:4000/learning/rule.json?action=test&name=loklak&url=http://api.loklak.org/api/search.json?q=$query$&test=fossasia
 * 
 * The resulting json will contain information about the success of the test, in this case:
 * {
 * "accepted": true,
 * "project": "default",
 * "tags": [""],
 * "action": "test",
 * "name": "loklak",
 * "path_computed": ["$.statuses"]
 * }
 * This contains default values for the not given parameters "project" and "tags" which may be further useful to find
 * the rule after storage. The "project" name is actually the file name where the rule will be stored and the "tags"
 * option is a list of tags to categorize the rule.
 * The "accepted": true tells us, that everything went fine so far.
 * A very important information is the "path_computed" list. It tells us where inside the json of the remote service an
 * array of attribute values is detected. One of there paths must be given when testing the rule again to discover the
 * Attributes of the data object
 * 
 * == (2) Testing the jsonpath to read a table from the service url ==
 * As a second step, the test service can be called again with the jsonpath inside the path parameter:
 * path = <jsonpath to data table>
 * i.e.
 * http://127.0.0.1:4000/learning/rule.json?action=test&name=loklak&url=http://api.loklak.org/api/search.json?q=$query$&test=fossasia&path=$.statuses
 * This url has only one attribute appended, path=$.statuses
 * Now the servlet is able to show all attributes in the resulting json which can be found in an example rule
 * {
  "accepted": true,
  "project": "default",
  "tags": [""],
  "action": "test",
  "name": "loklak",
  "path_computed": ["$.statuses"],
  "console": {"loklak": {
    "example": "http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20loklak%20WHERE%20query=%27fossasia%27;%22",
    "url": "http://api.loklak.org/api/search.json?q=$query$",
    "test": "fossasia",
    "parser": "json",
    "path": "$.statuses",
    "attributes": [
      "timestamp",
      "created_at",
      "screen_name",
      "text",
      "link",
      "id_str",
      "canonical_id",
      "parent",
      "source_type",
      "provider_type",
      "retweet_count",
      "favourites_count",
      "place_name",
      "place_id",
      "text_length",
      "place_context",
      "hosts",
      "hosts_count",
      "links",
      "links_count",
      "unshorten",
      "images",
      "images_count",
      "audio",
      "audio_count",
      "videos",
      "videos_count",
      "mentions",
      "mentions_count",
      "hashtags",
      "hashtags_count",
      "classifier_emotion",
      "classifier_emotion_probability",
      "classifier_language",
      "classifier_language_probability",
      "without_l_len",
      "without_lu_len",
      "without_luh_len",
      "user"
    ]
  }}
  }
 * In this json you find the attributes in console.loklak.attributes. Therefore the path is always console.<name>.attributes.
 * The rule is now ready for storage. Just change the action parameter to 'leran'.
 * 
 * == (3) Learning the rule ==
 * The rule is now ready for storage. Just change the action parameter to 'leran'.
 * i.e.
 * http://127.0.0.1:4000/learning/rule.json?action=learn&name=loklak&url=http://api.loklak.org/api/search.json?q=$query$&test=fossasia&path=$.statuses
 *
 * == (4) Listing of all rules ==
 * Just send the following attributes:
 * project = <projectname> (optional, by default 'default')
 * action = list
 * http://127.0.0.1:4000/learning/rule.json?action=list
 * 
 * == (5) Deletion of one rule ==
 * You must give the project name and the rule name. If the project is default, you can omit the project
 * i.e.
 * http://127.0.0.1:4000/learning/rule.json?action=delete&name=loklak
 */
public class ConsoleLearning extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/learning/rule.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject json = new JSONObject(true).put("accepted", true);
        
        // unauthenticated users are rejected
        if (user.getIdentity().isAnonymous()) {
            json.put("accepted", false);
            json.put("reject-reason", "you must be logged in");
            return new ServiceResponse(json);
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
            return new ServiceResponse(json);
        }
        json.put("action", action);

        if (action.equals("list")) {
            Set<String> projectnames = DAO.susi_memory.getIntentsetNames(client);
            JSONObject projects = new JSONObject(true);
            for (String p: projectnames) {
                try {
                    JsonTray t = DAO.susi_memory.getIntentset(client, p);
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
                return new ServiceResponse(json);
            }
            json.put("name", name);

            if (action.equals("delete")) {
                try {
                    JsonTray tray = DAO.susi_memory.getIntentset(client, project);
                    if (!tray.has("console")) tray.put("console", new JSONObject(), true);
                    JSONObject console = tray.getJSONObject("console");
                    console.remove(name);
                    tray.commit();                    
                } catch (IOException e) {
                    e.printStackTrace();
                    json.put("accepted", false);
                    json.put("reject-reason", "deletion the console rule causes an error: " + e.getMessage());
                }
                return new ServiceResponse(json);
            }
            
            String serviceURL = post.get("url", ""); // i.e. url=http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20wikigeodata%20WHERE%20place='$query$';
            if (serviceURL.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "url parameter required");
                return new ServiceResponse(json);
            }
            if (!serviceURL.contains("$query$")) {
                json.put("accepted", false);
                json.put("reject-reason", "the url must contain a string $query$");
                return new ServiceResponse(json);
            }
            
            String test = post.get("test", "");
            if (test.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "a testquery parameter is required");
                return new ServiceResponse(json);
            }
            
            // now test the api call
            byte[] serviceResponse = null;
            try {
                Map<String, String> request_header = new HashMap<>();
                request_header.put("Accept","application/json");
                serviceResponse = ConsoleService.loadDataWithQuery(serviceURL, request_header, test);
            } catch (Exception e) {
                json.put("accepted", false);
                json.put("reject-reason", "the console test load resulted in an error: " + e.getMessage());
                return new ServiceResponse(json);
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
                    return new ServiceResponse(json);
                }
            }
            
            JSONArray data = JsonPath.parse(serviceResponse, path);
            if (data == null || data.length() == 0) {
                json.put("accepted", false);
                json.put("reject-reason", "the jsonPath from data object did not recognize an array object");
                return new ServiceResponse(json);
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
                return new ServiceResponse(json);
            }
            
            if (action.equals("learn")) {
                try {
                    JsonTray tray = DAO.susi_memory.getIntentset(client, project);
                    if (!tray.has("console")) tray.put("console", new JSONObject(), true);
                    JSONObject console = tray.getJSONObject("console");
                    console.put(name, consolerule);
                    tray.commit();                    
                } catch (IOException e) {
                    e.printStackTrace();
                    json.put("accepted", false);
                    json.put("reject-reason", "storing the console rule causes an error: " + e.getMessage());
                }
                return new ServiceResponse(json);
            }
        }

        // fail case
        json.put("accepted", false);
        json.put("reject-reason", "no valid action given");
        return new ServiceResponse(json);
    }
    
}
