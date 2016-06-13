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
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;

/* examples:
 * http://localhost:9000/api/table.json?q=SELECT%20*%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 * http://localhost:9000/api/table.json?q=SELECT%20link,screen_name%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 */
public class TableService extends AbstractAPIHandler implements APIHandler {
   
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
        return "/api/table.json";
    }

    private static LinkedHashMap<String, String> parseColumns(String columnString) {
        String[] column_list = columnString.trim().split(",");
        if (column_list.length == 1 && column_list[0].equals("*")) return null;
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (String column: column_list) {
            String c = column.trim();
            int p = column.indexOf(" AS ");
            if (p < 0) {
                columns.put(c, c);
            } else {
                String c0 = c.substring(0,  p).trim();
                String c1 = c.substring(p + 4).trim();
                if (c1.startsWith("\'")) c1 = c1.substring(1);
                if (c1.endsWith("\'")) c1 = c1.substring(0, c1.length() - 1);
                columns.put(c0, c1);
            }
        }
        return columns;
    }
    
    private final static Map<Pattern, Function<Matcher, JSONArray>> pattern = new HashMap<>();
    static {
        pattern.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?id\\h??=\\h??'(.*?)'\\h??;??"), matcher -> {
            String id = matcher.group(2);
            LinkedHashMap<String, String> columns = parseColumns(matcher.group(1));
            if (columns != null && columns.size() == 0) return null;
            JSONObject message = DAO.messages.readJSON(id);
            if (message == null) return null;
            if (columns == null) return (new JSONArray()).put(message);
            JSONObject json = new JSONObject(true);
            for (Map.Entry<String, String> c: columns.entrySet()) if (message.has(c.getKey())) json.put(c.getValue(), message.get(c.getKey()));
            return (new JSONArray()).put(json);
        });
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {

        // parameters
        String q = post.get("q", "");
        
        JSONArray table = null;
        find_matcher: for (Map.Entry<Pattern, Function<Matcher, JSONArray>> pe: pattern.entrySet()) {
            Pattern p = pe.getKey();
            Matcher m = p.matcher(q);
            if (m.find()) {
                table = pe.getValue().apply(m);
                if (table != null) break find_matcher;
            }
        }
        
        // generate json
        JSONObject json = new JSONObject(true);
        if (table == null) table = new JSONArray();
        json.put("data", table);
        
        return json;
    }
    
}
