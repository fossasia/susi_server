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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

public class UserManagementService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 8578478303032749879L;

	@Override
	public UserRole getMinimalUserRole() {
		return UserRole.ACCOUNTCREATOR;
	}

	@Override
	public JSONObject getDefaultPermissions(UserRole baseUserRole){
		JSONObject result = new JSONObject();

		switch(baseUserRole) {
			case BUREAUCRAT:
				result.put("list_users", true);
				result.put("list_users-roles", true);
				result.put("edit-all", true);
				result.put("edit-less-privileged", true);
				break;
			case ADMIN:
				result.put("list_users", true);
				result.put("list_users-roles", true);
				result.put("edit-all", false);
				result.put("edit-less-privileged", true);
				break;
	        case BOT:
	        case ANONYMOUS:
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
		return "/aaa/user-management.json";
	}

	@Override
	public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

		JSONObject result = new JSONObject(true);
		result.put("accepted", false);
		result.put("message", "Error: Unable to show user management details");
		switch (post.get("show","")){
			case "user-list":
				if(permissions.getBoolean("list_users", false)){
					Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
			        JSONObject users = new JSONObject();
			        authorized.forEach(client -> users.put(client.getClient(), client.toJSON()));
					result.put("user-list", users);
					result.put("accepted", true);
					result.put("message","Success: Showing user list");
				} else throw new APIException(403, "Forbidden");
				break;
			default: throw new APIException(400, "No 'show' parameter specified");
		}

		return new ServiceResponse(result);
	}
}
