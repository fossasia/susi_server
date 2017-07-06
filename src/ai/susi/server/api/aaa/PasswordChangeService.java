/**
 *  PasswordResetService
 *  Copyright 6/7/17 by Dravit Lochan, @DravitLochan
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
import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;


import javax.servlet.http.HttpServletResponse;

/**
 * Created by dravit on 6/7/17.
 */
public class PasswordChangeService extends AbstractAPIHandler implements APIHandler{
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

        String useremail = post.get("changepassword", null);
        String password = post.get("password", null);
        String newpassword = post.get("newpassword",null);

        ClientCredential credential = new ClientCredential(ClientCredential.Type.change_pass, useremail);
        ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());
        Authentication authentication = DAO.getAuthentication(credential);

        String passwordHash;
        String salt;

        try {
            passwordHash = authentication.getString("passwordHash");
            salt = authentication.getString("salt");
        } catch (Throwable e) {
            Log.getLog().info("Invalid password try for user: " + identity.getName() + " from host: " + post.getClientHost() + " : password or salt missing in database");
            throw new APIException(422, "Invalid credentials");
        }
        if (!passwordHash.equals(getHash(password, salt))) {

            // save invalid login in accounting object
            Accounting accouting = DAO.getAccounting(identity);
            accouting.getRequests().addRequest(this.getClass().getCanonicalName(), "invalid login");

            Log.getLog().info("Invalid change password try for user: " + identity.getName() + " via passwd from host: " + post.getClientHost());
            throw new APIException(422, "Invalid credentials");
        } else {
            String newSalt = createRandomString(20);
            authentication.remove("salt");
            authentication.remove("passwordHash");
            authentication.put("salt", salt);
            authentication.put("passwordHash", getHash(newpassword, salt));
        }

        return null;
    }

}
