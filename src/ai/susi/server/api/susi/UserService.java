/**
 *  UserService
 *  Copyright 24.05.2017 by Michael Peter Christen, @0rb1t3r
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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiCognition;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * get information about the user.
 * i.e. it will return the history of conversation with two memories with
 * http://127.0.0.1:4000/susi/memory.json?cognitions=2
 */
public class UserService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303098111L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/memory.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        int cognitionsCount = Math.min(10, post.get("cognitions", 10));
        String client = user.getIdentity().getClient();
        List<SusiCognition> cognitions = DAO.susi.getMemories().getCognitions(client);
        JSONArray coga = new JSONArray();
        for (SusiCognition cognition: cognitions) {
            coga.put(cognition.getJSON());
            if (--cognitionsCount <= 0) break;
        }
        JSONObject json = new JSONObject(true);
        json.put("cognitions", coga);
        return new ServiceResponse(json);
    }
    
}
