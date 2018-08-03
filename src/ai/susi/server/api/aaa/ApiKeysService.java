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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet gives a API Endpoint to add, modify and delete different API keys used by SUSI.
 * It requires user role to be ADMIN or above ADMIN
 * example:
 * http://localhost:4000/aaa/apiKeys.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ
 * Necessary parameters : access_token, keyName
 * Other parameters (one out of two is necessary):
 * keyValue  -> string http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&keyVale=jfsdf43jss534fsdjgn
 * type  -> string http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&keyVale=jfsdf43jss534fsdjgn&type=private
 * deleteKey -> boolean http://localhost:4000/aaa/apiKeys.json?keyName=MAP_KEY&deleteKey=true
 */

public class ApiKeysService extends AbstractAPIHandler implements APIHandler {

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/apiKeys.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {
        if (call.get("keyName", null) == null
                && call.get("keyValue", null) == null) {
            throw new APIException(422, "Bad Request. No parameter present");
        }

        String keyName = call.get("keyName", null);
        String keyValue = call.get("keyValue", null);
        String type = call.get("type", "public");
        boolean deleteKey = call.get("deleteKey", false);
        JsonTray apiKeys = DAO.apiKeys;
        JSONObject result = new JSONObject();
        JSONObject keys = new JSONObject();

        if (apiKeys.has(type)) {
            keys = apiKeys.getJSONObject(type);
        }

	if(!deleteKey){
            try {
               JSONObject api = new JSONObject();
               api.put("value", keyValue);
               keys.put(keyName, api);
               apiKeys.put(type, keys, true);
               result.put("accepted", true);
               result.put("message", "Added new API key " + call.get("keyName") + " successfully !");
               return new ServiceResponse(result);
            } catch (Exception e) {
               throw new APIException(422, "Failed : Unable to add" + call.get("keyName") + " !" );
            }
	} else {
            try {
               keys.remove(keyName);
               apiKeys.put(type, keys, true);
               result.put("accepted", true);
               result.put("message", "Removed API key " + call.get("keyName") + " successfully !");
               return new ServiceResponse(result);
            } catch (Exception e) {
               throw new APIException(422, "Failed : " + call.get("keyName") + " doesn't exists!" );
            }
	}
    }
}
