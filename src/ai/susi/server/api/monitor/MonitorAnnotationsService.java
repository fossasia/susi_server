/**
 *  MonitorAnnotationsService
 *  Copyright 13.01.2018 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.monitor;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

/**
 * Grafana Data endpoint:
 * Service for http://localhost:3000/plugins/grafana-simple-json-datasource/edit
 * 
 * Try this with
 * http://localhost:4000/monitor/annotations
 */
public class MonitorAnnotationsService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 8539122L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/monitor/annotations";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject data = post.getJSONBody();
        // protocol details: https://github.com/grafana/simple-json-datasource/tree/master/dist#annotation-api
        JSONArray json = new JSONArray();
        
        json
            .put(new JSONObject(true)
                .put("annotation", new JSONObject(true).put("name", "annotation name").put("enabled",true).put("datasource", "generic datasource").put("showLine", true))
                .put("title", "Donlad trump is kinda funny")
                .put("time", 1515850521166L)
                .put("text", "teeext")
                .put("tags", "taaags"))
            .put(new JSONObject(true)
                .put("annotation", new JSONObject(true).put("name", "annotation name").put("enabled",true).put("datasource", "generic datasource").put("showLine", true))
                .put("title", "Wow he really won")
                .put("time", 1515849521166L)
                .put("text", "teeext")
                .put("tags", "taaags"))
            .put(new JSONObject(true)
                .put("annotation", new JSONObject(true).put("name", "annotation name").put("enabled",true).put("datasource", "generic datasource").put("showLine", true))
                .put("title", "When is the next ")
                .put("time", 1515848521166L)
                .put("text", "teeext")
                .put("tags", "taaags"))
        ;
        
        // success
        return new ServiceResponse(json).enableCORS();
    }
}

