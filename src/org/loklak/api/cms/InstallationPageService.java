/**
 *  InstallationPageService
 *  Copyright 05.08.2015 by Robert Mader, @treba13
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
import org.loklak.LoklakEmailHandler;
import org.loklak.LoklakInstallation;
import org.loklak.data.DAO;
import org.loklak.server.*;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.mail.AuthenticationFailedException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public class InstallationPageService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8678478303032749879L;

	private static final String customConfigPath = "data/settings/customized_config.properties";

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		return null;
	}

	@Override
	public String getAPIPath() {
        return "/api/installation.json";
    }

    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization authorization, final JSONObjectWithDefault permissions) throws APIException {

		JSONObject result = new JSONObject();

		if(post.get("restart", false)){
			LoklakInstallation.shutdown();
			return null;
		}

		if(post.get("checkSmtpCredentials", false)){

			String smtpHostName = post.get("smtpHostName", null);
			String smtpUsername = post.get("smtpUsername", null);
			String smtpPassword = post.get("smtpPassword", null);
			String smtpHostEncryption = post.get("smtpHostEncryption", null);
			int smtpHostPort = post.get("smtpHostPort", 0);
			boolean smtpDisableCertificateChecking = post.get("smtpDisableCertificateChecking", false);

			if(smtpHostName == null|| smtpHostEncryption == null || smtpUsername == null || smtpPassword == null){
				throw new APIException(400, "Bad request");
			}

			try {
                LoklakEmailHandler.checkConnection(smtpHostName, smtpUsername, smtpPassword, smtpHostEncryption, smtpHostPort, smtpDisableCertificateChecking);
				return result;
			}
			catch (AuthenticationFailedException e) {
				throw new APIException(422, e.getMessage());
			}
			catch (Throwable e){
                throw new APIException(400, e.getMessage());
            }
		}

		// read from file
		Properties properties = new Properties();
		try {
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(customConfigPath));
			properties.load(stream);
			stream.close();
		}
		catch (Exception e){
			throw new APIException(500, "Server error");
		}

		// admin
		String adminEmail = post.get("adminEmail", null);
		String adminPassword = post.get("adminPassword", null);

		if(adminEmail != null && adminPassword != null){
			// check email pattern
			Pattern pattern = Pattern.compile(LoklakEmailHandler.EMAIL_PATTERN);
			if (!pattern.matcher(adminEmail).matches()) {
				throw new APIException(400, "no valid email address");
			}

			// check password pattern
			pattern = Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,64}$");
			if (adminEmail.equals(adminPassword) || !pattern.matcher(adminPassword).matches()) {
				throw new APIException(400, "invalid password");
			}

			// check if id exists already
			ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, adminEmail);
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
			authentication.put("passwordHash", getHash(adminPassword, salt));
			authentication.put("activated", true);

			// set authorization details
			Authorization adminAuthorization = new Authorization(identity, DAO.authorization, DAO.userRoles);
			adminAuthorization.setUserRole(DAO.userRoles.getDefaultUserRole(BaseUserRole.ADMIN));

			result.put("admin_email", adminEmail);
		}

		String adminLocalOnly = post.get("adminLocalOnly", null);
		if("true".equals(adminLocalOnly) || "false".equals(adminLocalOnly)) properties.setProperty("users.admin.localonly", adminLocalOnly);

		// peername
		String peerName = post.get("peername", null);
		if(peerName != null) properties.setProperty("peername", peerName);

		// backend
		String backendPushEnabled = post.get("backendPushEnabled", null);
		if("true".equals(backendPushEnabled) || "false".equals(backendPushEnabled)) properties.setProperty("backend.push.enabled", backendPushEnabled);
		String backend = post.get("backend", null);
		if(backend != null) properties.setProperty("backend", backend);

		// cert checking
		String trustSelfSignedCerts = post.get("trustSelfSignedCerts", null);
		if(trustSelfSignedCerts != null) properties.setProperty("httpsclient.trustselfsignedcerts", trustSelfSignedCerts);

		// https setting
		String httpsMode = post.get("httpsMode", null);
		if(httpsMode != null) properties.setProperty("https.mode", httpsMode);
		String httpsKeySource = post.get("httpsKeySource", null);
		if(httpsKeySource != null) properties.setProperty("https.keysource", httpsKeySource);
		String httpsKeystore = post.get("httpsKeystore", null);
		if(httpsKeystore != null) properties.setProperty("keystore.name", httpsKeystore);
		String httpsKeystorePassword = post.get("httpsKeystorePassword", null);
		if(httpsKeystorePassword != null) properties.setProperty("keystore.password", httpsKeystorePassword);
		String httpsKey = post.get("httpsKey", null);
		if(httpsKey != null) properties.setProperty("https.key", httpsKey);
		String httpsCert = post.get("httpsCert", null);
		if(httpsCert != null) properties.setProperty("https.cert", httpsCert);

		// smtp
		String smtpEnabled = post.get("smtpEnabled", null);
		if("true".equals(smtpEnabled) || "false".equals(smtpEnabled)) properties.setProperty("smtp.mails.enabled", smtpEnabled);
		String smtpHostName = post.get("smtpHostName", null);
		if(smtpHostName != null) properties.setProperty("smtp.host.name", smtpHostName);
		Integer smtpHostPort = post.get("smtpHostPort", 0);
		if(smtpHostPort > 0 && smtpHostPort < 65535) properties.setProperty("smtp.host.port", smtpHostPort.toString());
		String smtpHostEncryption = post.get("smtpHostEncryption", null);
		if("none".equals(smtpHostEncryption) || "tls".equals(smtpHostEncryption) || "starttls".equals(smtpHostEncryption)) properties.setProperty("smtp.host.encryption", smtpHostEncryption);
        String smtpEmail = post.get("smtpEmail", null);
        if(smtpEmail != null) properties.setProperty("smtp.sender.email", smtpEmail);
		String smtpDisplayname = post.get("smtpDisplayname", null);
		if(smtpDisplayname != null) properties.setProperty("smtp.sender.displayname", smtpDisplayname);
		String smtpUsername = post.get("smtpUsername", null);
		if(smtpUsername != null) properties.setProperty("smtp.sender.username", smtpUsername);
		String smtpPassword = post.get("smtpPassword", null);
		if(smtpPassword != null) properties.setProperty("smtp.sender.password", smtpPassword);
		String smtpDisableCertificateChecking = post.get("smtpDisableCertificateChecking", null);
		if("true".equals(smtpDisableCertificateChecking) || "false".equals(smtpDisableCertificateChecking)) properties.setProperty("smtp.trustselfsignedcerts", smtpDisableCertificateChecking);

		// host url
		String hostUrl = post.get("hostUrl", null);
		if(hostUrl != null) properties.setProperty("host.url", hostUrl);
		String shortLinkStub = post.get("shortLinkStub", null);
		if(shortLinkStub != null) properties.setProperty("shortlink.urlstub", shortLinkStub);

		// user signup
		String publicSignup = post.get("publicSignup", null);
		if(publicSignup != null) properties.setProperty("users.public.signup", publicSignup);

		// write to file
		try{
			FileOutputStream stream = new FileOutputStream(customConfigPath);
			properties.store(stream, "This file can be used to customize the configuration file conf/config.properties");
			stream.close();
		}
		catch (Exception e){
			throw new APIException(500, "Server error");
		}
		
		return result;
    }
}
