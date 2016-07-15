/**
 *  SignUpServlet
 *  Copyright 27.05.2015 by Robert Mader, @treba13
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
import java.nio.file.Paths;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletResponse;

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

public class SignUpService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 8578478303032749879L;
	private static String verificationLinkPlaceholder = "%VERIFICATION-LINK%";

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		JSONObject result = new JSONObject();

		switch(baseUserRole){
			case ADMIN:
			case PRIVILEGED:
				result.put("register", true); // allow to register new users (this bypasses email verification and activation)
				result.put("activate", true); // allow to activate new users
				break;
			case USER:
			case ANONYMOUS:
			default:
				result.put("register", false);
				result.put("activate", false);
		}

		return result;
	}

	public String getAPIPath() {
		return "/api/signup.json";
	}

	@Override
	public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization auth, final JSONObjectWithDefault permissions)
			throws APIException {

		JSONObject result = new JSONObject();

		// if regex is requested
		if (post.get("getParameters", false)) {
			String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");
			String passwordPatternTooltip = DAO.getConfig("users.password.regex.tooltip",
					"Enter a combination of atleast six characters");
			if ("false".equals(DAO.getConfig("users.public.signup", "false"))) {
				throw new APIException(403, "Public signup disabled");
			}
			result.put("regex", passwordPattern);
			result.put("regexTooltip", passwordPatternTooltip);

			return result;
		}

		// is this a verification?
		if (post.get("validateEmail", null) != null) {
			if((auth.getIdentity().getName().equals(post.get("validateEmail", null)) && auth.getIdentity().isEmail()) // the user is logged in via an access token from the email
				|| permissions.getBoolean("activate", false)){ // the user is allowed to activate other users

				ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login,
						auth.getIdentity().getName());
				Authentication authentication = new Authentication(credential, DAO.authentication);

				if (authentication.getIdentity() == null) {
					authentication.delete();
					throw new APIException(400, "Bad request"); // do not leak if user exists or not
				}

				authentication.put("activated", true);

				result.put("message", "You successfully verified your account!");
				return result;
			}
			throw new APIException(400, "Bad request"); // do not leak if user exists or not
		}



		boolean activated;
		boolean sendEmail;
		if (permissions.getBoolean("register", false)) { // if this registration is done by user that is allowed to register new users
			activated = true;
			sendEmail = false;
		}
		else{
			switch (DAO.getConfig("users.public.signup", "false")) {
				case "false":
					throw new APIException(403, "Public signup disabled");
				case "true":
					activated = true;
					sendEmail = false;
					break;
				case "admin":
					activated = false;
					sendEmail = false;
					break;
				case "email":
					activated = false;
					sendEmail = true;
					break;
				default:
					throw new APIException(500, "Invalid value for key: users.public.signup");
			}
		}

		if (post.get("signup", null) == null || post.get("password", null) == null) {
			throw new APIException(400, "signup or password empty");
		}

		// get credentials
		String signup = post.get("signup", null);
		String password = post.get("password", null);

		// check email pattern
		Pattern pattern = Pattern.compile(LoklakEmailHandler.EMAIL_PATTERN);
		if (!pattern.matcher(signup).matches()) {
			throw new APIException(400, "no valid email address");
		}

		// check password pattern
		String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");

		pattern = Pattern.compile(passwordPattern);

		if (signup.equals(password) || !pattern.matcher(password).matches()) {
			throw new APIException(400, "invalid password");
		}

		// check if id exists already

		ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, signup);
		Authentication authentication = new Authentication(credential, DAO.authentication);

		if (authentication.getIdentity() != null) {
			throw new APIException(422, "email already taken");
		}

		// create new id
		ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());
		authentication.setIdentity(identity);

		// set authentication details
		String salt = createRandomString(20);
		authentication.put("salt", salt);
		authentication.put("passwordHash", getHash(password, salt));
		authentication.put("activated", activated);

		// set authorization details
		Authorization authorization = new Authorization(identity, DAO.authorization, DAO.userRoles);
		authorization.setUserRole(DAO.userRoles.getDefaultUserRole(BaseUserRole.USER));

		if (sendEmail) {
			String token = createRandomString(30);
			ClientCredential access_token = new ClientCredential(ClientCredential.Type.access_token, token);
			Authentication tokenAuthentication = new Authentication(access_token, DAO.authentication);
			tokenAuthentication.setIdentity(identity);
			tokenAuthentication.setExpireTime(7 * 24 * 60 * 60);
			tokenAuthentication.put("one_time", true);

			try {
				LoklakEmailHandler.sendEmail(signup, "Loklak verification", getVerificationMailContent(token, identity.getName()));

				result.put("message",
						"You successfully signed-up! An email with a verification link was send to your address.");

			} catch (ConfigurationException e) {
				result.put("message",
						"You successfully signed-up, but no email was sent as it's disabled by the server.");
			} catch (Exception e) {
				result.put("message",
						"You successfully signed-up, but an error occurred while sending the verification mail.");
			}
		} else {
			result.put("message", "You successfully signed-up!");
		}

		return result;
	}

	/**
	 * Read Email template and insert variables
	 * 
	 * @param token
	 *            - login token
	 * @return Email String
	 */
	private String getVerificationMailContent(String token, String userId) {

		String verificationLink = DAO.getConfig("host.name", "http://localhost:9000") + "/api/signup.json?access_token="
				+ token + "&validateEmail=" + userId + "&request_session=true";

		// get template file
		String result;
		try {
			result = IO.readFileCached(Paths.get(DAO.conf_dir + "/templates/verification-mail.txt"));
		} catch (IOException e) {
			result = "";
		}

		result = result.contains(verificationLinkPlaceholder)
				? result.replace(verificationLinkPlaceholder, verificationLink) : verificationLink;

		return result;
	}
}
