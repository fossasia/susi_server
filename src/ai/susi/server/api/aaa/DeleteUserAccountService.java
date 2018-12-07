/**
 *  DeleteUserAccountService
 *  Copyright by @Akshat-Jain on 28/7/18.
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
import ai.susi.server.Authorization;
import io.swagger.annotations.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Created by @Akshat-Jain on 28/7/18.
 * Servlet to allow Admin and higher user role to delete account of any user
 * test locally at http://127.0.0.1:4000/aaa/deleteUserAccount.json?email=akjn22@gmail.com&access_token=YLOvLl4zHBEwB77ox2CJkhAymKkNYQ
 */

@Path("/aaa/deleteUserAccount.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "DeleteUserAccountService", description = "This endpoint allows to delete an user account")
public class DeleteUserAccountService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4538356946942632187L;

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to delete an user account")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted user account"),
            @ApiResponse(code = 400, message = "Bad service call, missing arguments"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value = "Email of the user account to be deleted", required = true, dataType = "string", paramType = "query"),
    })
    @Override
    public String getAPIPath() {
        return "/aaa/deleteUserAccount.json";
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        String email = call.get("email", null);

        if(email == null) {
            throw new APIException(400, "Bad service call, missing arguments");
        }

        JSONObject result = new JSONObject(true);
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<String> keysList = new ArrayList<String>();
        authorized.forEach(client -> keysList.add(client.toString()));

            List<JSONObject> userList = new ArrayList<JSONObject>();
            for (Client client : authorized) {
                JSONObject json = client.toJSON();

                if(json.get("name").equals(email)) {
                    ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
                    Authorization authorization = DAO.getAuthorization(identity);

                    ClientCredential clientCredential = new ClientCredential(ClientCredential.Type.passwd_login, identity.getName());
                    Authentication authentication = DAO.getAuthentication(clientCredential);

                    DAO.deleteAuthorization(authorization.getIdentity());
                    DAO.deleteAuthentication(clientCredential);

                    return new ServiceResponse(result);
                }
            }

            result.put("accepted", true);
            result.put("message", "Successfully deleted user account!");
            return new ServiceResponse(result);
    }
}
