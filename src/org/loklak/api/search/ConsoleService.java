/**
 *  TableService
 *  Copyright 13.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.search;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.objects.Timeline;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;

/* examples:
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 * http://localhost:9000/api/console.json?q=SELECT%20link,screen_name%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20query=%27loklak%27%20GROUP%20BY%20screen_name;
 */
public class ConsoleService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(Authorization rights) {
        return APIServiceLevel.PUBLIC;
    }

    public String getAPIPath() {
        return "/api/console.json";
    }

    private static class Columns {
        private LinkedHashMap<String, String> columns;
        public Columns(String columnString) {
            String[] column_list = columnString.trim().split(",");
            if (column_list.length == 1 && column_list[0].equals("*")) {
                this.columns = null;
            } else {
                this.columns = new LinkedHashMap<>();
                for (String column: column_list) {
                    String c = column.trim();
                    int p = column.indexOf(" AS ");
                    if (p < 0) {
                        this.columns.put(c, c);
                    } else {
                        String c0 = c.substring(0,  p).trim();
                        String c1 = c.substring(p + 4).trim();
                        if (c1.startsWith("\'")) c1 = c1.substring(1);
                        if (c1.endsWith("\'")) c1 = c1.substring(0, c1.length() - 1);
                        this.columns.put(c0, c1);
                    }
                }
            }
        }
        public JSONObject extract(JSONObject message) {
            if (this.columns == null) return message;
            JSONObject json = new JSONObject(true);
            for (Map.Entry<String, String> c: columns.entrySet()) {
                if (message.has(c.getKey())) json.put(c.getValue(), message.get(c.getKey()));
            }
            return json;
        }
        public JSONArray extract(JSONArray messages) {
            JSONArray a = new JSONArray();
            for (Object json: messages) a.put(extract((JSONObject) json));
            return a;
        }
    }
    
    private final static LinkedHashMap<Pattern, Function<Matcher, JSONObject>> pattern = new LinkedHashMap<>();
    static {
        pattern.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?id\\h??=\\h??'(.*?)'\\h??;??"), matcher -> {
            Columns columns = new Columns(matcher.group(1));
            JSONObject message = DAO.messages.readJSON(matcher.group(2));
            return message == null ? null : new JSONObject()
                    .put("metadata", new JSONObject().put("offset", 0).put("hits", 1).put("count", 1))
                    .put("data", (new JSONArray()).put(columns.extract(message)));
        });
        pattern.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h?+GROUP\\h?+BY\\h?+(.*?)\\h??;"), matcher -> {
            Columns columns = new Columns(matcher.group(1));
            DAO.SearchLocalMessages messages = new DAO.SearchLocalMessages(matcher.group(2), Timeline.Order.CREATED_AT, 0, 0, 10, matcher.group(3));
            JSONArray array = new JSONArray();
            JSONObject aggregation = messages.getAggregations().getJSONObject(matcher.group(3));
            for (String key: aggregation.keySet()) array.put(new JSONObject().put(key, aggregation.get(key)));
            JSONObject json = messages.timeline.toJSON(true, "metadata", "data");
            json.put("data", columns.extract(array));
            return json;
        });
        pattern.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h??;??"), matcher -> {
            Columns columns = new Columns(matcher.group(1));
            DAO.SearchLocalMessages messages = new DAO.SearchLocalMessages(matcher.group(2), Timeline.Order.CREATED_AT, 0, 10, 0);
            JSONObject json = messages.timeline.toJSON(true, "metadata", "data");
            json.put("data", columns.extract(json.getJSONArray("data")));
            return json;
        });
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        
        JSONObject json = null;
        find_matcher: for (Map.Entry<Pattern, Function<Matcher, JSONObject>> pe: pattern.entrySet()) {
            Pattern p = pe.getKey();
            Matcher m = p.matcher(q);
            if (m.find()) {
                json = pe.getValue().apply(m);
                if (json != null) break find_matcher;
            }
        }
        
        // return json
        if (json == null) json = new JSONObject();
        return json;
    }
    
}
