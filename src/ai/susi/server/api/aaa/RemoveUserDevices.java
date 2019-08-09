package ai.susi.server.api.aaa;

/**
 *  RemoveUserDevices
 *  Copyright by @Akshat-Jain on 31/5/18.
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
 * Servlet to remove user devices
 * example:
 * http://localhost:4000/aaa/removeUserDevices.json?access_token=goONOeHpeYTUrHSm5PJfOJVtMs6ktG
 */

@Path("/aaa/removeUserDevices.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "RemoveUserDevices",
        description = "Servlet to remove user devices")

public class RemoveUserDevices extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 76744278782974471L;

    @Override
    public String getAPIPath() {
        return "/aaa/removeUserDevices.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @GET
    @ApiOperation(httpMethod = "GET", value = "Servlet to remove user devices")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Success: User devices were removed"),
            @ApiResponse(code = 401,
                    message = "Specified user data not found, ensure you are logged in.")  })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "macid", value = "The MacId of the device", dataType = "string", paramType = "query")
    })


    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        String key = query.get("macid", null);
        String email = query.get("email", null);

        if (authorization.getIdentity() != null) {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            UserRole userRole = authorization.getUserRole();
            if((userRole.getName().equals("admin") || userRole.getName().equals("superadmin")) && email != null){
                ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, email);
                Authorization rights = DAO.getAuthorization(identity);
                accounting = DAO.getAccounting(rights.getIdentity());
            }
            JSONObject userSettings = accounting.getJSON();

            if(key == null) {
                if (userSettings.has("devices")) {
                    userSettings.remove("devices");
                }
                JSONObject result = new JSONObject();
                result.put("accepted", true);
                result.put("message", "Success: All User devices were removed.");
                accounting.commit();
                return new ServiceResponse(result);
            }
            else {
                if (userSettings.has("devices")) {
                    JSONObject devices = userSettings.getJSONObject("devices");
                    if(devices.has(key)){
                        devices.remove(key);
                    }
                }
                JSONObject result = new JSONObject();
                result.put("accepted", true);
                result.put("message", "Success: User device was removed.");
                accounting.commit();
                return new ServiceResponse(result);
            }
        } else {
            throw new APIException(401, "Specified user data not found, ensure you are logged in.");
        }
    }
}
