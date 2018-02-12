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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
import ai.susi.tools.DateParser;

/**
 * http://localhost:4000/monitor/query
 */
public class MonitorQueryService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 8539122L;
    private static final Random r = new Random(System.currentTimeMillis()); // for testing
    
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
        long from = 0;
        if (froms != null) try {
            Date fromd = DateParser.iso8601MillisFormat.parse(froms);
            from = fromd.getTime();
        } catch (ParseException e) {}
        String tos = range == null ? null : range.getString("to");
        long to = System.currentTimeMillis();
        if (tos != null) try {
            Date tod = DateParser.iso8601MillisFormat.parse(tos);
            to = tod.getTime();
        } catch (ParseException e) {}
        long intervalMs = data == null ? 0 : data.getLong("intervalMs");
        JSONArray targeta = data == null ? null : data.getJSONArray("targets");
        Metric metric = Metric.unknown;
        List<String> targets = new ArrayList<>();
        for (Object o: targeta) {
            targets.add(((JSONObject) o).getString("target"));
            metric = Metric.valueOf(((JSONObject) o).getString("type"));
        };
        long maxDataPoints = data == null ? 0 : data.getLong("maxDataPoints");
        long deltatime = (to - from) / maxDataPoints;
        // here I would expect that intervalMs == deltatime (!)
        
        JSONArray json = new JSONArray();
        
        if (metric == Metric.timeserie) {
            for (String target: targets) {
                JSONArray a = new JSONArray();
                for (int i = 0; i < maxDataPoints; i++) a.put(new JSONArray().put(r.nextInt(1000)).put(from + i * deltatime));
                json.put(new JSONObject(true).put("target", target).put("datapoints", a));
            }
        }

        if (metric == Metric.table) {
            for (String target: targets) {
                JSONObject t = new JSONObject(true).put("target", target);
                json.put(t);
            }
        }
        
        // success
        return new ServiceResponse(json).enableCORS();
    }
    
    public static enum Metric {unknown, timeserie, table}
    public static enum Target {queries}
}

/*
 Example timeserie response

[
  {
    "target":"upper_75", // The field being queried for
    "datapoints":[
      [622,1450754160000],  // Metric value as a float , unixtimestamp in milliseconds
      [365,1450754220000]
    ]
  },
  {
    "target":"upper_90",
    "datapoints":[
      [861,1450754160000],
      [767,1450754220000]
    ]
  }
]


If the metric selected is "type": "table", an example table response:

[
  {
    "columns":[
      {"text":"Time","type":"time"},
      {"text":"Country","type":"string"},
      {"text":"Number","type":"number"}
    ],
    "rows":[
      [1234567,"SE",123],
      [1234567,"DE",231],
      [1234567,"US",321]
    ],
    "type":"table"
  }
]
 */