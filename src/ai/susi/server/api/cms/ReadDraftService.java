/**
 *  StoreDraftService
 *  Copyright by Michael Christen, @0rb1t3r
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

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.DateParser;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Random;


/**
 * This endpoint accepts either one parameter: id or no parameters
 * If only one id is given, then an object with id/drafts pairs is returned with the given draft or none if the draft does not exist.
 * If no id is given, then an object with all drafts are returned as key/draft pairs
 * http://localhost:4000/cms/readDraft.json?id=aabbcc
 */
public class ReadDraftService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -1960932190918215684L;
    private static final Random random = new Random(System.currentTimeMillis());
    
    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS; // temporary, change to USER later
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/readDraft.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        String id = call.get("id", "");
        
        JSONObject json = new JSONObject();
        
        Map<String, DAO.Draft> map = id.length() == 0 ? DAO.readDrafts(authorization.getIdentity()) : DAO.readDrafts(authorization.getIdentity(), id);

        json.put("accepted", true);
        JSONObject drafts = new JSONObject();
        for (Map.Entry<String, DAO.Draft> entry: map.entrySet()) {
        	JSONObject val = new JSONObject();
        	val.put("object", entry.getValue().getObject());
        	val.put("created", DateParser.iso8601Format.format(entry.getValue().getCreated()));
        	val.put("modified", DateParser.iso8601Format.format(entry.getValue().getModified()));
        	drafts.put(entry.getKey(), entry.getValue().getObject());
        }
        
        json.put("drafts", drafts);

        return new ServiceResponse(json);
    }
}
