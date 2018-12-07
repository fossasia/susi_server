/**
 *  UserAccountPermissions
 *  Copyright 11.08.2015 by Robert Mader, @treba13
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

import ai.susi.SusiServer;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.server.Authorization;
import io.swagger.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Constructor;
@Path("/aaa/account-permissions.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "UserAccountPermissions",
		description = "This Endpoint returns the permissions of a user account")
public class UserAccountPermissions extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8678478303032749879L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.USER; }

	@GET
	@ApiOperation(httpMethod = "GET", value = "Resource to get the permissions of a user account")
	@ApiResponses(value = {
			@ApiResponse(code = 200,
					message = "Success : User Role Permission"),
			@ApiResponse(code = 400,
					message = "Bad service name")  })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "getServicePermissions", value = "Name of the API key", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "getServiceList", value = "List of all services on server", dataType = "boolean", paramType = "query"),
    })
	@Override
	public JSONObject getDefaultPermissions(UserRole baseUserRole) {
		return null;
	}

	@Override
	public String getAPIPath() {
        return "/aaa/account-permissions.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {
    	
    	JSONObject result = new JSONObject(true);
		result.put("accepted", false);
		result.put("message", "Error: Unable to get service permissions");

		if(query.get("getServicePermissions", null) != null){
			String serviceString = query.get("getServicePermissions", null);

			Class<?> serviceClass;
			try{
				serviceClass = Class.forName(serviceString);
			} catch (ClassNotFoundException e){
				throw new APIException(400, "Bad service name (no class)");
			}
			Constructor<?> constructor;
			try{
				constructor = serviceClass.getConstructors()[0];
			} catch (Throwable e){
				throw new APIException(400, "Bad service name (no constructor)");
			}
			Object service;
			try{
				service = constructor.newInstance();
			} catch (Throwable e){
				throw new APIException(400, "Bad service name (no instance possible)");
			}

			if(service instanceof AbstractAPIHandler){
				result.put("servicePermissions", authorization.getPermission());
				return new ServiceResponse(result);
			}
			else{
				throw new APIException(400, "Bad service name (no instance of AbstractAPIHandler)");
			}
		} else if(query.get("getServiceList", false)) {
			JSONArray serviceList = new JSONArray();
			for(Class<? extends Servlet> service: SusiServer.services){
				serviceList.put(service.getCanonicalName());
			}
			result.put("serviceList", serviceList);
			result.put("accepted", true);
			result.put("message", "Success : Service List");
			return new ServiceResponse(result);
		}else if (query.get("getUserRolePermission", false)) {
			result.put("userRolePermissions", authorization.getPermission());
			result.put("accepted", true);
			result.put("message", "Success : User Role Permission");
			return new ServiceResponse(result);
		} else {
			result.put("userName", authorization.getIdentity().getName());
			result.put("userSpecificPermissions", authorization.getPermission());
			result.put("userRole", authorization.getUserRole().getName());
			result.put("userRoleSpecificPermissions", authorization.getPermission());
			result.put("accepted", true);
			result.put("message", "Success : Service Permissions");
			return new ServiceResponse(result);
		}
    }
}
