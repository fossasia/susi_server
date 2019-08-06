/**
 *  PasswordResetService
 *  Copyright 17.06.2016 by Shiven Mian, @shivenmian
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
import ai.susi.EmailHandler;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.TimeoutMatcher;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

public class PasswordResetService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -8893457607971788891L;

	@Override
	public String getAPIPath() {
		return "/aaa/resetpassword.json";
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
	public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions)
			throws APIException {
		JSONObject result = new JSONObject(true);
		result.put("accepted", false);
		result.put("message", "Error: Unable to reset Password");

		String newpass = call.get("newpass", null);

		ClientCredential credential = new ClientCredential(ClientCredential.Type.resetpass_token,
				call.get("token", null));
		Authentication authentication = new Authentication(credential, DAO.passwordreset);
		ClientCredential emailcred = new ClientCredential(ClientCredential.Type.passwd_login,
				authentication.getIdentity().getName());

		String passwordPattern = DAO.getConfig("users.password.regex", "^((?=.*\\d)(?=.*[A-Z])(?=.*\\W).{8,64})$");

		Pattern pattern = Pattern.compile(passwordPattern);

		if ((authentication.getIdentity().getName()).equals(newpass) || !new TimeoutMatcher(pattern.matcher(newpass)).matches()) {
			// password can't equal email and regex should match
			throw new APIException(422, "invalid password");
		}

		if (DAO.hasAuthentication(emailcred)) {
			Authentication emailauth = DAO.getAuthentication(emailcred);
			String salt = createRandomString(20);
			emailauth.remove("salt");
			emailauth.remove("passwordHash");
			emailauth.put("salt", salt);
			emailauth.put("passwordHash", getHash(newpass, salt));
		}

		if (authentication.has("one_time") && authentication.getBoolean("one_time")) {
			authentication.delete();
		}

		String subject = "Password Reset";
		try {
			EmailHandler.sendEmail(authentication.getIdentity().getName(), subject, "Your password has been reset successfully!");
			result.put("accepted", true);
			result.put("message", "Your password has been changed!");
		} catch (Exception e) {
			result.put("message", e.getMessage());
		}

		return new ServiceResponse(result);
	}

}
