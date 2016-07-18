/**
 *  AppsServlet
 *  Copyright 08.01.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.cms;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.*;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.http.HttpServletResponse;

public class ChangeUserRoleService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5577184683745091648L;

    @Override
    public String getAPIPath() {
        return "/api/change-user-role.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.USER; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public JSONObject serviceImpl(Query query, HttpServletResponse response, Authorization auth, final JSONObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject();

        if(!query.isLocalhostAccess()){
            throw new APIException(403, "Access only from localhost");
        }

        String userRoleName = query.get("userRole", "");
        if(!userRoleName.isEmpty()){
            UserRole userRole = DAO.userRoles.getUserRoleFromString(userRoleName);
            if(userRole != null){
                auth.setUserRole(userRole);
                result.put("message","user now has user-role: " + userRole.getDisplayName());
            }
            else{
                throw new APIException(400, "unknown user-role");
            }
        }
        else{
            throw new APIException(400, "no user-role specified");
        }

        return result;
    }
}
