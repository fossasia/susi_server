/**
 *  DeleteDraftService
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

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

import java.util.Random;


/**
 * This endpoint accepts one parameter: id
 * If only one id is given, then an object with id is flushed
 * http://localhost:4000/cms/deleteDraft.json?id=aabbcc
 */
public class DeleteDraftService extends AbstractAPIHandler implements APIHandler {


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
        return "/cms/deleteDraft.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        String id = call.get("id", "");
        JSONObject json = new JSONObject();
        if (id.length() > 0) DAO.deleteDraft(authorization.getIdentity(), id);
        json.put("accepted", true);
        return new ServiceResponse(json);
    }
}
