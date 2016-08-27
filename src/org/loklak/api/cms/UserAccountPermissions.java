/**
 *  UserAccountPermissions
 *  Copyright 11.08.2015 by Robert Mader, @treba13
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.LoklakServer;
import org.loklak.server.*;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;

public class UserAccountPermissions extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8678478303032749879L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.USER; }

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		return null;
	}

	@Override
	public String getAPIPath() {
        return "/api/account-permissions.json";
    }

    @Override
    public JSONObject serviceImpl(Query query, HttpServletResponse response, Authorization authorization, final JSONObjectWithDefault permissions) throws APIException {
    	
    	JSONObject result = new JSONObject();

		if(query.get("getServicePermissions", null) != null){
			String serviceString = query.get("getServicePermissions", null);

			Class<?> serviceClass;
			try{
				serviceClass = Class.forName(serviceString);
			} catch (ClassNotFoundException e){
				throw new APIException(400, "Bad service name (no class)");
			}
			Constructor<?> constructor;
			try{
				constructor = serviceClass.getConstructors()[0];
			} catch (Throwable e){
				throw new APIException(400, "Bad service name (no constructor)");
			}
			Object service;
			try{
				service = constructor.newInstance();
			} catch (Throwable e){
				throw new APIException(400, "Bad service name (no instance possible)");
			}

			if(service instanceof AbstractAPIHandler){
				result.put("servicePermissions", authorization.getPermissions((AbstractAPIHandler) service));
				return result;
			}
			else{
				throw new APIException(400, "Bad service name (no instance of AbstractAPIHandler)");
			}
		} else if(query.get("getServiceList", false)) {
			JSONArray serviceList = new JSONArray();
			for(Class<? extends Servlet> service: LoklakServer.services){
				serviceList.put(service.getCanonicalName());
			}
			result.put("serviceList", serviceList);
			return result;
		}else if (query.get("getUserRolePermission", false)) {
			result.put("userRolePermissions", authorization.getUserRole().getPermissionOverrides());
			return result;
		} else {
			result.put("userName", authorization.getIdentity().getName());
			result.put("userSpecificPermissions", authorization.getPermissionOverrides());
			result.put("userRole", authorization.getUserRole().getDisplayName());
			result.put("userRoleSpecificPermissions", authorization.getUserRole().getPermissionOverrides());
			result.put("parentUserRole", authorization.getUserRole().getParent());
			return result;
		}
    }
}
