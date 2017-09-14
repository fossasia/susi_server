/**
 * PasswordResetService
 * Copyright 25/7/17 by Dravit Lochan, @DravitLochan
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */
package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.EmailHandler;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.IO;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by dravit on 25/7/17.
 * parameter : emailId
 *
 * sample request :
 *
 * http://127.0.0.1:4000/aaa/resendVerificationLink.json?emailId=test@fossasia.com
 */
public class ResendVerificationLinkService extends AbstractAPIHandler implements APIHandler {

    public static String verificationLinkPlaceholder = "%VERIFICATION-LINK%";

    @Override
    public String getAPIPath() {
        return "/aaa/resendVerificationLink.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        String emailId = post.get("emailId", null);

        // Check for null or case where emailId is only spaces
        if (emailId == null)
            throw new APIException(422, "Bad Request. Not Enough parameters");

        if (emailId.trim().length() == 0)
            throw new APIException(422, "No email id provided!");

        // generate client credentials using email id ( this is required to get authentication object) and check if user id exists or not
        ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, emailId);
        Authentication authentication = DAO.getAuthentication(credential);
        if (authentication.getIdentity() == null) {
            throw new APIException(422, "Invalid email id. Please Sign up!");
        }

        // check if user is already verified or not
        if (authentication.getBoolean("activated", false)) {
            throw new APIException(422, "User already verified!");
        } else {
            String token = createRandomString(30);
            ClientCredential access_token = new ClientCredential(ClientCredential.Type.access_token, token);
            Authentication tokenAuthentication = DAO.getAuthentication(access_token);
            ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());
            authentication.setIdentity(identity);
            tokenAuthentication.setIdentity(identity);
            tokenAuthentication.setExpireTime(7 * 24 * 60 * 60);
            tokenAuthentication.put("one_time", true);

            try {
                EmailHandler.sendEmail(emailId, "SUSI AI verification", getVerificationMailContent(token, identity.getName()));
                json.put("message",
                        "Your request for a new verification link has been processed! An email with a verification link was sent to your address.");
                json.put("accepted", true);
            } catch (Throwable e) {
                json.put("accepted", false);
                json.put("message", "Please try again! Error : " + e.getMessage());
            }
        }
        return new ServiceResponse(json);
    }

    /**
     * Read Email template and insert variables
     *
     * @param token - login token
     * @return Email String
     */
    private String getVerificationMailContent(String token, String userId) throws APIException {

        String hostUrl = DAO.getConfig("host.url", null);
        if (hostUrl == null) throw new APIException(500, "No host url configured");

        // redirect user to accounts verify-account route
        String verificationLink = "http://accounts.susi.ai/verify-account?access_token=" + token
                + "&validateEmail=" + userId + "&request_session=true";

        // get template file
        String result;
        try {
            result = IO.readFileCached(Paths.get(DAO.conf_dir + "/templates/resend-verification-link.txt"));
        } catch (IOException e) {
            throw new APIException(500, "No verification email template");
        }
        result = result.replace(verificationLinkPlaceholder, verificationLink);
        return result;
    }
}
