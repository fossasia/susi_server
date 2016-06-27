/**
 *  PasswordRecoveryServlet
 *  Copyright 08.06.2016 by Shiven Mian, @shivenmian
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.loklak.LoklakEmailHandler;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authentication;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.ClientCredential;
import org.loklak.server.ClientIdentity;
import org.loklak.server.Query;
import org.loklak.tools.IO;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class PasswordRecoveryService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 3515757746392011162L;
	private static String resetLinkPlaceholder = "%RESET-LINK%";

	@Override
	public String getAPIPath() {
		return "/api/recoverpassword.json";
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
	public JSONObject serviceImpl(Query call, Authorization rights, final JSONObjectWithDefault permissions)
			throws APIException {
		JSONObject result = new JSONObject();

		// check if token exists

		if (call.get("getParameters", false)) {
			if (call.get("token", null) != null && !call.get("token", null).isEmpty()) {
				ClientCredential credentialcheck = new ClientCredential(ClientCredential.Type.resetpass_token,
						call.get("token", null));
				if (DAO.passwordreset.has(credentialcheck.toString())) {
					Authentication authentication = new Authentication(credentialcheck, DAO.passwordreset);
					if (authentication.checkExpireTime()) {
						String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");
						String passwordPatternTooltip = DAO.getConfig("users.password.regex.tooltip",
								"Enter a combination of atleast six characters");
						result.put("message", "Email ID: " + authentication.getIdentity().getName());
						result.put("regex", passwordPattern);
						result.put("regexTooltip", passwordPatternTooltip);
						return result;
					}
					authentication.delete();
					throw new APIException(422, "Expired token");
				}
				throw new APIException(422, "Invalid token");
			} else {
				throw new APIException(422, "No token specified");
			}
		}

		String usermail;
		try {
			usermail = URLDecoder.decode(call.get("forgotemail", null), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new APIException(400, "malformed query");
		}

		ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, usermail);
		ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());

		if (!DAO.authentication.has(credential.toString())) {
			throw new APIException(422, "email does not exist");
		}

		String token = createRandomString(30);
		ClientCredential tokenkey = new ClientCredential(ClientCredential.Type.resetpass_token, token);
		Authentication resetauth = new Authentication(tokenkey, DAO.passwordreset);
		resetauth.setIdentity(identity);
		resetauth.setExpireTime(7 * 24 * 60 * 60);
		resetauth.put("one_time", true);

		String subject = "Password Recovery";
		try {
			LoklakEmailHandler.sendEmail(usermail, subject, getVerificationMailContent(token));
			result.put("message", "Recovery email sent to your email ID. Please check");
		} catch (Exception e) {
			result.put("message", e.toString());
		}
		return result;
	}

	private String getVerificationMailContent(String token) {

		String verificationLink = DAO.getConfig("host.name", "http://localhost:9000")
				+ "/apps/resetpass/index.html?token=" + token;
		String result;
		try {
			result = IO.readFileCached(Paths.get(DAO.conf_dir + "/templates/reset-mail.txt"));
		} catch (IOException e) {
			result = "";
		}

		result = result.contains(resetLinkPlaceholder) ? result.replace(resetLinkPlaceholder, verificationLink)
				: verificationLink;

		return result;
	}

}
