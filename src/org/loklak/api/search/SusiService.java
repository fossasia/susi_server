/**
 *  SusiService
 *  Copyright 29.06.2015 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.susi.SusiArgument;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.http.HttpServletResponse;

public class SusiService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/api/susi.json";
    }
    
    public static List<SusiArgument> susi(String query, int maxcount) {
        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }
        return DAO.susi.react(query, maxcount);
    }
    
    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        int count = post.get("count", 1);
        
        List<SusiArgument> dispute = susi(q, count);
        JSONObject json = new JSONObject();
        json.put("count", count);
        JSONArray answers = new JSONArray();
        dispute.forEach(answer -> answers.put(answer.mindstate()));
        json.put("answers", answers);
        return json;
    }
    
}
