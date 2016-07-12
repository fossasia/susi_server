/**
 *  UserManagement
 *  Copyright 23.06.2015 by Robert Mader, @treba13
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

import java.util.HashMap;
import java.util.Map;

public class UserManagementService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 8578478303032749879L;

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.PRIVILEGED;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole){
		JSONObject result = new JSONObject();

		switch(baseUserRole){
			case ADMIN:
				result.put("list_users", true);
				result.put("list_users-roles", true);
				result.put("edit-all", true);
				result.put("edit-less-privileged", true);
				break;
			case PRIVILEGED:
				result.put("list_users", true);
				result.put("list_users-roles", true);
				result.put("edit-all", false);
				result.put("edit-less-privileged", true);
				break;
			default:
				result.put("list_users", false);
				result.put("list_users-roles", false);
				result.put("edit-all", false);
				result.put("edit-less-privileged", false);
				break;
		}

		return result;

	}

	public String getAPIPath() {
		return "/api/user-management.json";
	}

	@Override
	public JSONObject serviceImpl(Query post, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {

		JSONObject result = new JSONObject();

		switch (post.get("show","")){
			case "user-list":
				if(permissions.getBoolean("list_users", false)){
					result.put("user-list", DAO.authorization.getPersistent());
				} else throw new APIException(403, "Forbidden");
				break;
			case "user-roles":
				JSONObject userRolesObj = new JSONObject();
				Map<String, UserRole> userRoles = DAO.userRoles.getUserRoles();
				for(String key : userRoles.keySet()){
					UserRole userRole = userRoles.get(key);
					JSONObject obj = new JSONObject();
					obj.put("display-name",userRole.getDisplayName());
					obj.put("base-user-role",userRole.getBaseUserRole().name());
					obj.put("permission-overrides",userRole.getPermissionOverrides());
					userRolesObj.put(key,obj);
				}
				result.put("user-roles", userRolesObj);
				break;
			default: throw new APIException(400, "No 'show' parameter specified");
		}

		return result;
	}
}
