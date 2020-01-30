/**
 * PasswordResetService
 * Copyright 6/7/17 by Dravit Lochan, @DravitLochan
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */
package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.EmailHandler;
import ai.susi.json.JsonTray;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.TimeoutMatcher;
import org.json.JSONObject;
import ai.susi.tools.VerifyRecaptcha;

import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Created by dravit on 6/7/17.
 * test locally at http://127.0.0.1:4000/aaa/changepassword.json
 * send the following three parameters :
 * changepassword : contains email id of the user who's password has to be changed
 * password : current password
 * newpassword : new password
 */
public class PasswordChangeService extends AbstractAPIHandler implements APIHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -8679696048228442818L;

    @Override
    public String getAPIPath() {
        return "/aaa/changepassword.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject();
        result.put("accepted", false);

        String useremail = post.get("changepassword", null);
        String password = post.get("password", null);
        String newpassword = post.get("newpassword", null);

        ClientCredential pwcredential = new ClientCredential(ClientCredential.Type.passwd_login, useremail);
        Authentication authentication = DAO.getAuthentication(pwcredential);
        ClientCredential emailcred = new ClientCredential(ClientCredential.Type.passwd_login,
                authentication.getIdentity().getName());
        ClientIdentity identity = authentication.getIdentity();
        String passwordHash;
        String salt;

        try {
            passwordHash = authentication.getString("passwordHash");
            salt = authentication.getString("salt");
        } catch (Throwable e) {
            DAO.log("Invalid password try for user: " + identity.getName() + " from host: " + post.getClientHost() + " : password or salt missing in database");
            result.put("message", "Invalid credentials.");
            throw new APIException(HttpStatus.UNPROCESSABLE_ENTITY_422, "Invalid credentials");
        }
        if (!passwordHash.equals(getHash(password, salt))) {

            // save invalid login in accounting object
            Accounting accouting = DAO.getAccounting(identity);
            accouting.getRequests().addRequest(this.getClass().getCanonicalName(), "invalid login");

            DAO.log("Invalid change password try for user: " + identity.getName() + " via passwd from host: " + post.getClientHost());
            result.put("message", "Invalid credentials.");
            throw new APIException(HttpStatus.UNPROCESSABLE_ENTITY_422, "Invalid credentials");
        } else {
            String passwordPattern = DAO.getConfig("users.password.regex", "^((?=.*\\d)(?=.*[A-Z])(?=.*\\W).{8,64})$");

            Pattern pattern = Pattern.compile(passwordPattern);

            if ((authentication.getIdentity().getName()).equals(newpassword) || !new TimeoutMatcher(pattern.matcher(newpassword)).matches()) {
                // password can't be equal to email and regex should be matched
                result.put("message", "Invalid password.");
                throw new APIException(HttpStatus.UNPROCESSABLE_ENTITY_422, "invalid password");
            }

            if (DAO.hasAuthentication(emailcred)) {
                JsonTray CaptchaConfig = DAO.captchaConfig;
                JSONObject captchaObj = CaptchaConfig.has("config") ? CaptchaConfig.getJSONObject("config")
                        : new JSONObject();
                Boolean isChangePassowordCaptchaEnabled = captchaObj.has("changePassword")
                        ? captchaObj.getBoolean("changePassword")
                        : true;

                if (isChangePassowordCaptchaEnabled) {
                    String gRecaptchaResponse = post.get("g-recaptcha-response", null);
                    boolean isRecaptchaVerified = VerifyRecaptcha.verify(gRecaptchaResponse);
                    if (!isRecaptchaVerified) {
                        result.put("message", "Please verify recaptcha");
                        result.put("accepted", false);
                        return new ServiceResponse(result);
                    }
                }

                if(passwordHash.equals(getHash(newpassword, salt))){
                    result.put("message","Your current password matches new password.");
                    result.put("accepted", false);
                    return new ServiceResponse(result);
                }
                

                Authentication emailauth = DAO.getAuthentication(emailcred);
                emailauth.remove("passwordHash");
                emailauth.put("passwordHash", getHash(newpassword, salt));
                DAO.log("password change for user: " + identity.getName() + " via newpassword from host: " + post.getClientHost());

                String subject = "Password Change";
                InetAddress address = null;
				try {
					address = InetAddress.getLocalHost();
				} catch (UnknownHostException e1) {
					
				}
                String findIP = address.getHostAddress();
                String IP = "127.0.1.1";
                try {
                    EmailHandler.sendEmail(authentication.getIdentity().getName(), subject, "Your password has been changed successfully!");
                    result.put("message", "Your password has been changed!");
                    result.put("accepted", true);
                } catch (Exception e) {
                    if(findIP.equals(IP)) {
                    	String prepasswordHash = passwordHash;
                    	String postpasswordHash = authentication.getString("passwordHash");
                    	if(!prepasswordHash.equals(postpasswordHash)) {
                    	result.put("message","Your password has sucessfully been changed and stored in the local database!");
                    	result.put("accepted",true);
                    	}
                    	else {
                    		throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR_500,"Failed to update password in the local database!");
                    	}
                }
                    else{
                    	throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR_500, "Failed to change password");}
                    }
                }
        }

        return new ServiceResponse(result);
    }
}
