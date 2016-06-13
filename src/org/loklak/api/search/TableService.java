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
    
    private final static Map<Pattern, Function<Matcher, JSONArray>> pattern = new HashMap<>();
    static {
        pattern.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?id\\h??=\\h??'(.*?)'\\h??;??"), matcher -> {
            String columns = matcher.group(1);
            String id = matcher.group(2);
            boolean all = columns.equals("*");
            String[] columns_list = all ? new String[0] : columns.split(",");
            if (!all && columns_list.length == 0) return null;
            JSONObject message = DAO.messages.readJSON(id);
            if (message == null) return null;
            if (all) return (new JSONArray()).put(message);
            JSONObject json = new JSONObject(true);
            for (String c: columns_list) if (message.has(c)) json.put(c, message.get(c));
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
