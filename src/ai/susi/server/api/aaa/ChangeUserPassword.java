/**
 *  ChangeUserPassword
 *  Copyright by @Akshat-Jain on 26/7/18.
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

/**
 * Created by @Akshat-Jain on 26/7/18.
 * Servlet to allow OPERATOR and higher user roles to change password of any user
 * This service accepts 5 parameters - email of the user whose password is to be changed, new password for the user
 * test locally at http://127.0.0.1:4000/aaa/changeUserPassword.json?email=akjn44@gmail.com&newpassword=testPassword&access_token=XHNzlwdiR04MnJ3d5f0GDMiyyCac1o
 */
public class ChangeUserPassword extends AbstractAPIHandler implements APIHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -8679697848228442818L;

    @Override
    public String getAPIPath() {
        return "/aaa/changeUserPassword.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.OPERATOR;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject();
        result.put("accepted", false);

        String useremail = post.get("email", null);
        String newpassword = post.get("newpassword", null);


        ClientCredential pwcredential = new ClientCredential(ClientCredential.Type.passwd_login, useremail);
        Authentication authentication = DAO.getAuthentication(pwcredential);
        ClientCredential emailcred = new ClientCredential(ClientCredential.Type.passwd_login,
                authentication.getIdentity().getName());
        ClientIdentity identity = authentication.getIdentity();
        String salt = authentication.getString("salt");

        String passwordPattern = DAO.getConfig("users.password.regex", "^(?=.*\\d).{6,64}$");

        Pattern pattern = Pattern.compile(passwordPattern);

        if ((authentication.getIdentity().getName()).equals(newpassword) || !new TimeoutMatcher(pattern.matcher(newpassword)).matches()) {
            // password can't be equal to email and regex should be matched
            result.put("message", "Invalid password.");
            throw new APIException(400, "invalid password.");
        }

        if (DAO.hasAuthentication(emailcred)) {
            Authentication emailauth = DAO.getAuthentication(emailcred);
            String newsalt = createRandomString(20);
            emailauth.remove("passwordHash");
            emailauth.put("passwordHash", getHash(newpassword, salt));
            DAO.log("password change for user: " + identity.getName() + " via newpassword from host: " + post.getClientHost());

            String subject = "Password Changed";
            try {
                EmailHandler.sendEmail(authentication.getIdentity().getName(), subject, "Your password has been changed by the Admins of the system!");
                result.put("message", "Successfully changed user's password!");
                result.put("accepted", true);
            } catch (Exception e) {
                result.put("message", e.getMessage());
            }
        }

        return new ServiceResponse(result);
    }

}

