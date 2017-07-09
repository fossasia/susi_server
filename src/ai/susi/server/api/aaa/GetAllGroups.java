/**
 *  GetAllGroups
 *  Copyright 22.06.2017 by Chetan Kaushik, @dynamitechetan
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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Set;

/**
 * This Servlets returns all the group details of all the groups created.
 * Accepts NO, GET or POST parameters.
 * Can be tested on
 * http://127.0.0.1:4000/aaa/getAllGroups.json
 *
 */
public class GetAllGroups extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -179412273153306443L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/getAllGroups.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        JSONObject result = DAO.group.toJSON();
        result.put("accepted", true);
        result.put("message", "Success: Fetched all groups");
        return new ServiceResponse(result);
    }

}
