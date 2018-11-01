/**
 *  CheckRegistrationServlet
 *  Copyright 16.06.2018 by Arundhati Gupta, @arundhati24
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

/**
 * This endpoint accepts 1 parameter check_email.
 * http://127.0.0.1:4000/aaa/checkRegistration.json?check_email=abc@email.com
 */
public class CheckRegistrationService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 7402102212787839019L;

	@Override
	public UserRole getMinimalUserRole() {
		return UserRole.ANONYMOUS;
	}

	@Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }
	
	public String getAPIPath() {
		return "/aaa/checkRegistration.json";
	}

	@Override
	public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization auth, final JsonObjectWithDefault permissions)
			throws APIException {

        String checkEmail = post.get("check_email", null);

        JSONObject result = new JSONObject();
        
        if (checkEmail == null) {
            throw new APIException(422, "Email not provided.");
        }

		// check if id exists already

		ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, checkEmail);
		Authentication authentication = DAO.getAuthentication(credential);

		if (authentication.getIdentity() != null) {
			result.put("exists", true);
		} else {
			result.put("exists", false);
		}
		
		result.put("accepted", true);
		result.put("check_email", checkEmail);

		return new ServiceResponse(result);
	}
	
}
