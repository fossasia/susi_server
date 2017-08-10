/**
 * ShowAdminService
 * Copyright 10/08/17 by Dravit Lochan, @DravitLochan
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by dravit on 10/8/17.
 * To show or not to show admin option to the user who is logged in,
 * this servlet will return a boolean flag on attribute showAdmin
 */
public class ShowAdminService extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/aaa/showAdminService.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        UserRole userRole = rights.getUserRole();

        switch (userRole) {
            case BUREAUCRAT:
            case ADMIN:
                json.put("accepted", true);
                json.put("showAdmin", true);
                break;
            case BOT:
            case ANONYMOUS:
            case USER:
            case REVIEWER:
            case ACCOUNTCREATOR:
            default:
                json.put("accepted", true);
                json.put("showAdmin", false);
        }

        return new ServiceResponse(json);
    }
}
