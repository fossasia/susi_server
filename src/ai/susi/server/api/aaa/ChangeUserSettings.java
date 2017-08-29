/**
 *  ChangeUserSettings
 *  Copyright by saurabh on 20/6/17.
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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to write user setting
 * this service accepts two parameter key and value to be stored in User settings
 * test locally at http://127.0.0.1:4000/aaa/changeUserSettings.json?key=theme&value=dark
 */
public class ChangeUserSettings extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -7418883159709458190L;

    @Override
    public String getAPIPath() {
        return "/aaa/changeUserSettings.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

       String countSetting = query.get("count","-1");
       int count = Integer.parseInt(countSetting);
       if(count == -1){
           throw new APIException(400, "Bad Service call, count parameters not provided");
       } else {
           Map<String, String> settings = new HashMap<String, String>();
           for (int i = 1; i <= count; i++) {
               String key = query.get("key" + i, null);
               String value = query.get("value" + i, null);
               if (key == null || value == null) {
                   throw new APIException(400, "Bad Service call, key or value parameters not provided");
               } else {
                   settings.put(key, value);
               }
           }
           if (authorization.getIdentity() == null) {
               throw new APIException(400, "Specified User Setting not found, ensure you are logged in");
           } else {
               Accounting accounting = DAO.getAccounting(authorization.getIdentity());
               for (Map.Entry<String, String> entry : settings.entrySet()) {
                   String key = entry.getKey();
                   String value = entry.getValue();
                   JSONObject jsonObject = new JSONObject();
                   jsonObject.put(key, value);
                   if (accounting.getJSON().has("settings")) {
                       accounting.getJSON().getJSONObject("settings").put(key, value);
                   } else {
                       accounting.getJSON().put("settings", jsonObject);
                   }
               }
               JSONObject result = new JSONObject(true);
               result.put("accepted", true);
               result.put("message", "You successfully changed settings of your account!");
               return new ServiceResponse(result);
           }
       }

    }
}
