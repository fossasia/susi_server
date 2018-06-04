/**
 *  AddNewDevice
 *  Copyright by @Akshat-Jain on 24/5/18.
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
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Accounting;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
/**
 * Created by @Akshat-Jain on 24/5/18.
 * Servlet to add device information
 * This service accepts two parameter - Mac address of device and device name - and stores them in User data
 * test locally at http://127.0.0.1:4000/aaa/addNewDevice.json?macid=macAddressOfDevice&device=deviceName&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class AddNewDevice extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8742102844146051190L;

    @Override
    public String getAPIPath() {
        return "/aaa/addNewDevice.json";
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

               JSONObject value = new JSONObject();
           
               String key = query.get("macid", null);
               String name = query.get("name", null);
               String device = query.get("device", null);
               
               if (key == null || name == null || device == null) {
                   throw new APIException(400, "Bad service call, missing arguments");
               } else {
                   value.put(name, device);
               }
           
           if (authorization.getIdentity() == null) {
               throw new APIException(400, "Specified user data not found, ensure you are logged in");
           } else {
                Accounting accounting = DAO.getAccounting(authorization.getIdentity());
                if (accounting.getJSON().has("devices")) {
                    accounting.getJSON().getJSONObject("devices").put(key, value);
                } else {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(key, value);
                    accounting.getJSON().put("devices", jsonObject);
               }
                accounting.commit();
               
               JSONObject result = new JSONObject(true);
               result.put("accepted", true);
               result.put("message", "You have successfully added the device!");
               return new ServiceResponse(result);
           }
       

    }
}
