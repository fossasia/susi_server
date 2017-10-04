/**
 *  SkillLogService
 *  Copyright 01.10.2017 by Michael Peter Christen, @0rb1t3r
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

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiAwareness;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * call:
 * http://localhost:4000/susi/skilllog.json?skill=/susi_server/conf/susi/en_0090_fail.json
 */
public class SkillLogService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ADMIN; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/skilllog.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        String skill = post.get("skill", "").trim();
        int count = post.get("count", 1000);

        JSONArray cognitions = new JSONArray();
        try {
            SusiAwareness awareness = new SusiAwareness(DAO.susi.getMemories().getSkillLogPath(skill), count);
            awareness.forEach(cognition -> cognitions.put(cognition.getJSON()));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResponse(cognitions);
    }
    
}
