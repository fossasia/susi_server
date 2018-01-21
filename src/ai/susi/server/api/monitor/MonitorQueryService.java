/**
 *  MonitorQueryService
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
 * http://localhost:4000/monitor/query
 */
public class MonitorQueryService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 8539122L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/monitor/query";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject data = post.getJSONBody();
        // protocol details: https://github.com/grafana/simple-json-datasource/tree/master/dist#query-api
        // data i.e.: {"timezone":"browser","panelId":1,"range":{"from":"2018-01-12T23:00:00.000Z","to":"2018-01-13T22:02:29.280Z","raw":{"from":"now/d","to":"now"}},"rangeRaw":{"from":"now/d","to":"now"},"interval":"1m","intervalMs":60000,"targets":[{"target":"upper_25","refId":"A","type":"timeserie"}],"format":"json","maxDataPoints":1300,"scopedVars":{"__interval":{"text":"1m","value":"1m"},"__interval_ms":{"text":60000,"value":60000}}}
        JSONObject range = data == null ? null : data.getJSONObject("range");
        String froms = range == null ? null : range.getString("from");
        String tos = range == null ? null : range.getString("to");
        long intervalMs = data == null ? 0 : data.getLong("intervalMs");
        JSONArray targets = data == null ? null : data.getJSONArray("targets");
        long maxDataPoints = data == null ? 0 : data.getLong("maxDataPoints");
        
        
        JSONArray json = new JSONArray();
        
        // success
        return new ServiceResponse(json).enableCORS();
    }
}
