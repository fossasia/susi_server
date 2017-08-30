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

package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * example:
 * http://localhost:4000/aaa/authorization-demo.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class AuthorizationDemoService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8678478303032749879L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

	@Override
	public JSONObject getDefaultPermissions(UserRole userRole) {
		JSONObject result = new JSONObject();

		switch (userRole) {
            case BUREAUCRAT:
                result.put("download_limit", 1000000000);
                break;
            case ADMIN:
                result.put("download_limit", 100000);
                break;
			case ACCOUNTCREATOR:
				result.put("download_limit", 10000);
				break;
            case REVIEWER:
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
		}

		return result;
	}

	@Override
	public String getAPIPath() {
        return "/aaa/authorization-demo.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
    	
    	JSONObject result = new JSONObject(true);
		result.put("accepted", true);
		result.put("message", "Successfully processed request");
		result.put("user", rights.getIdentity().getName());
        result.put("user role", rights.getUserRole().getName());
		result.put("permissions", rights.getPermission());
		
		return new ServiceResponse(result);
    }
}
