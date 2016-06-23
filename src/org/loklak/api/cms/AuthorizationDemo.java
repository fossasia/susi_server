/**
 *  AuthorizationDemo
 *  Copyright 22.06.2015 by Robert Mader, @treba13
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
import org.loklak.server.*;
import org.loklak.server.BaseUserRole;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class AuthorizationDemo extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8678478303032749879L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		JSONObject result = new JSONObject();

		switch(baseUserRole){
			case ADMIN:
				result.put("download_limit", -1);
				break;
			case PRIVILEGED:
				result.put("download_limit", 1000);
				break;
			case USER:
				result.put("download_limit", 100);
				break;
			case ANONYMOUS:
				result.put("download_limit", 10);
				break;
			default:
				result.put("download_limit", 0);
				break;
		}

		return result;
	}

	@Override
	public String getAPIPath() {
        return "/api/authorization-demo.json";
    }

    @Override
    public JSONObject serviceImpl(Query post, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {
    	
    	JSONObject result = new JSONObject();

		result.put("user", rights.getIdentity().getName());
        result.put("user role", rights.getUserRole().getDisplayName());
        result.put("base user role", rights.getBaseUserRole().name());
		result.put("permissions",rights.getPermissions(this));
		
		return result;
    }
}
