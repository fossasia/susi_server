/**
 *  ModifyUserDevices
 *  Copyright by @Akshat-Jain on 25/7/18.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Created by @Akshat-Jain on 25/7/18.
 * Servlet to allow Admin and higher user role to modify config of any device of any user
 * test locally at http://localhost:4000/aaa/modifyUserDevices.json?access_token=mOPR99jx3PPHP6uWrukffxR4vIxOZi&macid=8C-39-45-23-D8-95&name=Testing&email=akjn44@gmail.com
 */
public class ModifyUserDevices extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4538304346942632187L;

    @Override
    public String getAPIPath() {
        return "/aaa/modifyUserDevices.json";
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
        String macid = call.get("macid", null);
        String name = call.get("name", null);
        String room = call.get("room", null);

        if(email == null || macid == null || (name == null && room == null)) {
            throw new APIException(400, "Bad service call, missing arguments");
        }

        JSONObject result = new JSONObject(true);
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<String> keysList = new ArrayList<String>();
        authorized.forEach(client -> keysList.add(client.toString()));
        String[] keysArray = keysList.toArray(new String[keysList.size()]);

            List<JSONObject> userList = new ArrayList<JSONObject>();
            for (Client client : authorized) {
                JSONObject json = client.toJSON();

                if(json.get("name").equals(email)) {
                    ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
                    Authorization authorization = DAO.getAuthorization(identity);

                    ClientCredential clientCredential = new ClientCredential(ClientCredential.Type.passwd_login, identity.getName());
                    Authentication authentication = DAO.getAuthentication(clientCredential);

                    Accounting accounting = DAO.getAccounting(authorization.getIdentity());

                    if(accounting.getJSON().has("devices")) {

                        JSONObject userDevice = accounting.getJSON().getJSONObject("devices");
                        if(userDevice.has(macid)) {
                            JSONObject deviceInfo = userDevice.getJSONObject(macid);

                            if(name != null) {
                                deviceInfo.put("name", name);
                            }
                            if(room != null) {
                                deviceInfo.put("room", room);
                            }
                        }
                        else {
                            throw new APIException(404, "Specified device does not exist.");
                        }

                    } else {
                        json.put("devices", "");
                    }
                    accounting.commit();
                }
            }

            result.put("accepted", true);
            result.put("message", "You have successfully modified the device config!");
            return new ServiceResponse(result);
    }
}
