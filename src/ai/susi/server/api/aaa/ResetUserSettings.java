package ai.susi.server.api.cms;

/**
 *  ResetUserSettings
 *  Copyright by Akshat Garg, @akshatnitd
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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to reset user settings
 * example:
 * http://localhost:4000/aaa/resetUserSettings.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */

public class ResetUserSettings extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -4980350840211454420L;

    @Override
    public String getAPIPath() {
        return "/aaa/resetUserSettings.json";
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
        if ( authorization.getIdentity()!=null ) {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            JSONObject userSettings = accounting.getJSON();
            if (userSettings.has("settings")) {
                userSettings.remove("settings");                       
            }
            JSONObject result = new JSONObject();
            result.put("accepted", true);
            result.put("message", "Success: User data was reset");
            accounting.commit();
            return new ServiceResponse(result);
        } else {
            throw new APIException(400, "Specified user data not found, ensure you are logged in");
        }

    }
}
