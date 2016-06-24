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

package org.loklak.api.cms;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.*;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class PasswordResetService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -8893457607971788891L;

	@Override
	public String getAPIPath() {
		return "/api/resetpassword.json";
	}

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		return null;
	}

	@Override
	public JSONObject serviceImpl(Query call, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {
		JSONObject result = new JSONObject();

		String newpass;
		try {
			newpass = URLDecoder.decode(call.get("newpass", null), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			result.put("success", false);
			result.put("message", "malformed query");
			return result;
		}

		ClientCredential credential = new ClientCredential(ClientCredential.Type.resetpass_token,
				call.get("token", null));
		Authentication authentication = new Authentication(credential, DAO.passwordreset);
		ClientCredential emailcred = new ClientCredential(ClientCredential.Type.passwd_login,
				authentication.getIdentity().getName());

		String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");

		Pattern pattern = Pattern.compile(passwordPattern);

		if ((authentication.getIdentity().getName()).equals(newpass) || !pattern.matcher(newpass).matches()) {
			// password can't equal email and regex should match
			result.put("success", false);
			result.put("message", "Invalid Password");
			return result;
		}

		if (DAO.authentication.has(emailcred.toString())) {
			Authentication emailauth = new Authentication(emailcred, DAO.authentication);
			String salt = createRandomString(20);
			emailauth.remove("salt");
			emailauth.remove("passwordHash");
			emailauth.put("salt", salt);
			emailauth.put("passwordHash", getHash(newpass, salt));
		}

		if (authentication.has("one_time") && authentication.getBoolean("one_time")) {
			authentication.delete();
		}
		result.put("success", true);
		result.put("message", "Your password has been changed!");
		return result;
	}

}
