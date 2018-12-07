/**
 * DeleteGroupService
 * Copyright 27/6/17 by Dravit Lochan, @DravitLochan
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
 * Created by dravit on 27/6/17.
 * http://127.0.0.1:4000/aaa/deleteGroup.json?group=groupName
 */

@Path("/aaa/deleteGroup.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "DeleteGroupService", description = "This endpoint deletes a group")
public class DeleteGroupService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -6460356959547369940L;

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to delete a group")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Group deleted successfully"),
            @ApiResponse(code = 400, message = "Group Name parameter not specified"),
            @ApiResponse(code = 401, message = "Group doesn't exists")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "group", value = "Group name to be deleted", required = true, dataType = "string", paramType = "query"),
    })
    @Override
    public String getAPIPath() {
        return "/aaa/deleteGroup.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject();
        String groupName = post.get("group", null);

        if (groupName != null) {
            if (DAO.group.has(groupName)) {
                DAO.group.remove(groupName);
                result.put("accepted", true);
                result.put("message", "Group deleted successfully");
                return new ServiceResponse(result);
            } else {
                throw new APIException(422, "Group doesn't exists");
            }
        } else {
            throw new APIException(400, "Group name parameter not specified");
        }
    }
}
