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
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
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
 */

public class GetApiKeys extends AbstractAPIHandler implements APIHandler {

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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        JsonTray apiKeys = DAO.apiKeys;
        JSONObject publicKeys = apiKeys.getJSONObject("public");
        JSONObject result = new JSONObject();
        JSONObject keys = new JSONObject();

        for (String key : JSONObject.getNames(publicKeys)) {
            JSONObject values =  (JSONObject)publicKeys.get(key);
            keys.put(key, values.get("value"));
        }

        try {
            result.put("accepted", true);
            result.put("keys", keys);
            result.put("message", "Success : Fetched all API key successfully !");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(422, "Failed : Unable to fetch API keys!" );
        }
    }
}
