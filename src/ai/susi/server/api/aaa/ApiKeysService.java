/**
 *  APIKeysService
 *  Copyright by Praduman @PrP-11 on 27/07/18.
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
import ai.susi.json.JsonTray;
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
 * This Servlet gives a API Endpoint to add, modify and delete different API
 * keys used by SUSI. It requires user role to be ADMIN or above ADMIN example:
 * http://localhost:4000/aaa/apiKeys.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ
 * Necessary parameters : access_token, keyName Other parameters (one out of two
 * is necessary): keyValue -> string
 * http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&keyVale=jfsdf43jss534fsdjgn
 * type -> string
 * http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&keyVale=jfsdf43jss534fsdjgn&type=private
 * deleteKey -> boolean
 * http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&deleteKey=true
 */

@Path("/aaa/apiKeys.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "ApiKeysService", description = "This Endpoint adds, modify and delete different API keys used by SUSI")
public class ApiKeysService extends AbstractAPIHandler implements APIHandler {

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @GET
    @ApiOperation(httpMethod = "GET", value = "Endpoint to add, modify and delete diffrent API keys used by SUSI")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Removed API key successfully"),
            @ApiResponse(code = 201, message = "Added new API key xxxxxxxx successfully"),
            @ApiResponse(code = 400, message = "Bad Request. No parameter present"),
            @ApiResponse(code = 401, message = "Base user role not sufficient. Your base user role is 'ANONYMOUS', minimum user role required is 'admin'"),
            @ApiResponse(code = 500, message = "Failed: Unable to add xxxxxxxx!"),
            @ApiResponse(code = 501, message = "Failed: xxxxxxxx doesn't exists") })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyName", value = "Name of the API key", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "keyValue", value = "API key", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "Type of API key", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "deleteKey", value = "Parameter to specify to delete API key", dataType = "boolean", paramType = "query") })
    @Override
    public String getAPIPath() {
        return "/aaa/apiKeys.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {
        if (call.get("keyName", null) == null && call.get("keyValue", null) == null) {
            throw new APIException(400, "Bad Request. No parameter present");
        }

        String keyName = call.get("keyName", null);
        String keyValue = call.get("keyValue", null);
        String type = call.get("type", "public");
        boolean deleteKey = call.get("deleteKey", false);
        JsonTray apiKeys = DAO.apiKeys;
        JSONObject result = new JSONObject();
        JSONObject keys = new JSONObject();
        JSONObject userKeyObj = new JSONObject();
        ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token,
                call.get("access_token", null));
        Authentication authentication = DAO.getAuthentication(credential);
        String userId = null;

        if (apiKeys.has(type)) {
            keys = apiKeys.getJSONObject(type);
        }

        if (authentication.getIdentity() != null) {
            ClientIdentity identity = authentication.getIdentity();
            userId = identity.getUuid();
            authorization = DAO.getAuthorization(identity);
            UserRole userRole = authorization.getUserRole();
            if (!type.equals("user")) {
                if ((!userRole.getName().equals("admin") && !userRole.getName().equals("superadmin"))
                        || userId == null) {
                    throw new APIException(401, "Unauthorized");
                }
            } else {
                userKeyObj = keys.has(userId) ? keys.getJSONObject(userId) : new JSONObject();
            }

        } else {
            throw new APIException(422, "Access token is not valid");
        }

        if (!deleteKey) {
            try {
                if (type.equals("public") || type.equals("private")) {
                    JSONObject api = new JSONObject();
                    api.put("value", keyValue);
                    keys.put(keyName, api);
                } else {
                    JSONObject key = new JSONObject();
                    JSONObject apiValueObj = new JSONObject();
                    apiValueObj.put("value", keyValue);
                    key.put(keyName, apiValueObj);
                    userKeyObj.put(keyName, apiValueObj);
                    keys.put(userId, userKeyObj);
                }
                apiKeys.put(type, keys, true);
                result.put("accepted", true);
                result.put("message", "Added new API key " + call.get("keyName") + " successfully !");
                return new ServiceResponse(result);
            } catch (Exception e) {
                throw new APIException(500, "Failed : Unable to add" + call.get("keyName") + " !");
            }
        } else {
            try {
                if (type.equals("public") || type.equals("private")) {
                    keys.remove(keyName);
                } else {
                    userKeyObj.remove(keyName);
                    keys.put(userId, userKeyObj);
                }
                apiKeys.put(type, keys, true);
                result.put("accepted", true);
                result.put("message", "Removed API key " + call.get("keyName") + " successfully !");
                return new ServiceResponse(result);
            } catch (Exception e) {
                throw new APIException(501, "Failed : " + call.get("keyName") + " doesn't exists!");
            }
        }
    }
}
