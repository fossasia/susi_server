/**
 *  JsonPathTestService
 *  Copyright 27.02.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.susi;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonPath;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;

public class JsonPathTestService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847333032749879L;

    @Override
    public String getAPIPath() {
        return "/susi/jsonpath.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        post.setResponse(response, "application/javascript");

        String jsonString = post.get("json", "{}").trim();
        String path = post.get("path", "$").trim();
        
        JSONTokener tokener = new JSONTokener(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));

        JSONObject testresult = new JSONObject(true);
        testresult.put("test", jsonString);
        testresult.put("path", path);
        try {
            JSONArray data = JsonPath.parse(tokener, path);
            testresult.put("data", data == null ? "error" : data);
        } catch (JSONException e) {
            testresult.put("data", "error: " + e.getMessage());
        }
        return testresult;
    }

}
