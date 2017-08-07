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
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.TimeoutMatcher;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;


import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

/**
 * Created by dravit on 6/7/17.
 * test locally at http://127.0.0.1:4000/aaa/changepassword.json
 * send the following three parameters :
 * changepassword : contains email id of the user who's password has to be changed
 * password : current password
 * newpassword : new password
 */
public class PasswordChangeService extends AbstractAPIHandler implements APIHandler {
    @Override
    public String getAPIPath() {
        return "/aaa/changepassword.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
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
            Log.getLog().info("Invalid password try for user: " + identity.getName() + " from host: " + post.getClientHost() + " : password or salt missing in database");
            result.put("message", "invalid credentials");
            throw new APIException(422, "Invalid credentials");
        }
        if (!passwordHash.equals(getHash(password, salt))) {

            // save invalid login in accounting object
            Accounting accouting = DAO.getAccounting(identity);
            accouting.getRequests().addRequest(this.getClass().getCanonicalName(), "invalid login");

            Log.getLog().info("Invalid change password try for user: " + identity.getName() + " via passwd from host: " + post.getClientHost());
            result.put("message", "invalid credentials");
            throw new APIException(422, "Invalid credentials");
        } else {
            String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");

            Pattern pattern = Pattern.compile(passwordPattern);

            if ((authentication.getIdentity().getName()).equals(newpassword) || !new TimeoutMatcher(pattern.matcher(newpassword)).matches()) {
                // password can't equal email and regex should match
                result.put("message", "invalid password");
                throw new APIException(400, "invalid password");
            }

            if (DAO.hasAuthentication(emailcred)) {
                if(passwordHash.equals(getHash(newpassword, salt))){
                    result.put("message","your current password matches new password");
                    result.put("accepted", false);
                    return new ServiceResponse(result);
                }
                Authentication emailauth = DAO.getAuthentication(emailcred);
                String newsalt = createRandomString(20);
                emailauth.remove("passwordHash");
                emailauth.put("passwordHash", getHash(newpassword, salt));
                Log.getLog().info("password change for user: " + identity.getName() + " via newpassword from host: " + post.getClientHost());
                result.put("message", "Your password has been changed!");
                result.put("accepted", true);
            }
        }

        return new ServiceResponse(result);
    }

}

