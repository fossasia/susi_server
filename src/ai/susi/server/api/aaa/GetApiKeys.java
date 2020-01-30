/**
 *  GetApiKeys
 *  Copyright by Praduman @PrP-11 on 29/07/18.
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

/**
 * This Servlet gives a API Endpoint to fetch different API keys used by SUSI.
 * It requires user role to be ANONYMOUS or above ANONYMOUS
 * example:
 * http://localhost:4000/aaa/getApiKeys.json
 * User API Key, necessary parameters, access_token, type(user):
 * http://localhost:4000/aaa/getApiKeys.json?type=user&access_token=gzTPX50EfAXrpTZtiZrJNao94H00P5
 */
@Path("/aaa/getApiKeys.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "GetApiKeys", description = "This endpoint to fetch different API keys used by SUSI")
public class GetApiKeys extends AbstractAPIHandler implements APIHandler {

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to fetch different API keys used by SUSI")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success : Fetched all API key successfully !"),
            @ApiResponse(code = 500, message = "Failed : Unable to fetch API keys!"),

    })
    @Override
    public String getAPIPath() {
        return "/aaa/getApiKeys.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {

        JsonTray apiKeys = DAO.apiKeys;
        String type = call.get("type", "public");
        JSONObject configKeys = new JSONObject();
        JSONObject result = new JSONObject();
        if (type.equals("public")) {
            configKeys = apiKeys.getJSONObject("public");
        } else if (type.equals("user")) {
            String access_token = call.get("access_token", null);
            ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, access_token);
            Authentication authentication = DAO.getAuthentication(credential);

            if (authentication.getIdentity() != null) {
                ClientIdentity identity = authentication.getIdentity();
                String userId = identity.getUuid();
                JSONObject userKeys = userId != null && apiKeys.has("user") ? apiKeys.getJSONObject("user") : null;
                configKeys = userKeys != null && userKeys.has(userId) ? userKeys.getJSONObject(userId) : new JSONObject();
            } else {
                throw new APIException(422, "Access token is not valid");
            }
        }
        try {
            result.put("accepted", true);
            result.put("keys", configKeys);
            result.put("message", "Success : Fetched all API key successfully !");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed : Unable to fetch API keys!");
        }
    }
}
