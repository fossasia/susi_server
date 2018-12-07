/**
 * GetAllGroups
 * Copyright 22.06.2017 by Chetan Kaushik, @dynamitechetan
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;

import ai.susi.server.Authorization;
import io.swagger.annotations.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This Servlets returns all the group details of all the groups created.
 * Accepts NO, GET or POST parameters.
 * Can be tested on
 * http://127.0.0.1:4000/aaa/getAllGroups.json
 *
 */

@Path("/aaa/getAllGroups.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "GetAllGroups",
        description = "This Endpoint returns all the group details of all the groups created.")
public class GetAllGroups extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -179412273153306443L;

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to get the details of all groups created.")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Success: Fetched all groups"),
            @ApiResponse(code = 401,
                    message = "Base user role not sufficient.")})
    @Override
    public String getAPIPath() {
        return "/aaa/getAllGroups.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        JSONObject result = DAO.group.toJSON();
        result.put("accepted", true);
        result.put("message", "Success: Fetched all groups");
        return new ServiceResponse(result);
    }

}
