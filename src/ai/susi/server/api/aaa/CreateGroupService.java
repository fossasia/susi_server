/**
 *  CreateGroups
 *  Copyright 22.06.2017 by saurabh
 *  Contributors: Saurabh Jain @saurabhjn76, Chetan Kaushik @dynamitechetan
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
 *
 */

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.server.Authorization;
import io.swagger.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This Servlets creates a group with default baserole as anonymous.
 * It takes 1 parameter group and other optional parameter role.
 * Can be tested on
 * http://127.0.0.1:4000/aaa/createGroup.json?group=groupName&role=admin&permission=<comma separated values>
 *
 */

@Path("/aaa/createGroup.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "CreateGroupService", description = "This Endpoint creates new group")
public class CreateGroupService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -742269505564698987L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to create new group")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Group Created Successfully"),
            @ApiResponse(code = 400, message = "Group Name Parameter not specified"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "group", value = "Group Name", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "role", value = "Roles given to the group participants", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "permission", value = "Permissions given to the group participants with comma seperated values.", required = true, dataType = "string", paramType = "query")
    })
    @Override
    public String getAPIPath() {
        return "/aaa/createGroup.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions)throws APIException {
        JSONObject result = new JSONObject();
        String groupName = call.get("group", null);
        String grouppBaseRole = call.get("role","anonymous");
        String permission = call.get("permissions","");
        if( groupName!=null ) {
            if( DAO.group.has(groupName)) {
                result.put("message", "Group already exists");
                result.put("accepted", false);
            } else {
                JSONObject groupDetail = new JSONObject();
                groupDetail.put("group_members", new JSONObject());
                groupDetail.put("groupBaseRole", grouppBaseRole);
                ArrayList aList= new ArrayList(Arrays.asList(permission.split(",")));
                JSONArray permissionArray = new JSONArray();
                for(int i=0;i<aList.size();i++)
                {
                    permissionArray.put(i,aList.get(i));
                }
                groupDetail.put("permission", permissionArray);
                result.put("message", "Group created successfully");
                result.put("accepted", true);
                DAO.group.put(groupName,groupDetail,rights.getIdentity().isPersistent());
            }
            return new ServiceResponse(result);
        } else {
            throw new APIException(400, "Group Name Parameter not specified");
        }
    }

}

