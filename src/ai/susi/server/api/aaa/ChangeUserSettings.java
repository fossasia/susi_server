/**
 *  ChangeUserSettings
 *  Copyright by saurabh on 20/6/17.
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
import ai.susi.server.Authorization;
import io.swagger.annotations.*;

import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by saurabh on 20/6/17.
 * Servlet to write user setting
 * this service accepts parameter to be stored in User settings
 * test locally at http://127.0.0.1:4000/aaa/changeUserSettings.json?theme=dark
 */

@Path("/aaa/changeUserSettings.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "ChangeUserSettings", description = "This Endpoint is used to write user setting")
public class ChangeUserSettings extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -7418883159709458190L;

    private String[] possibleKeys = {"server", "enterAsSend", "micInput", "speechOutput", "speechOutputAlways", "speechRate", "speechPitch", "ttsLanguage", "userName", "prefLanguage", "timeZone", "countryCode", "countryDialCode", "phoneNo", "checked", "serverUrl", "theme", "backgroundImage", "messageBackgroundImage", "customThemeValue", "avatarType"};

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to write user setting")
    @ApiResponses(value = {
    		 @ApiResponse(code = HttpStatus.OK_200, message = "You have successfully changed the settings of your account"),
             @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Bad Service call, count parameters not provided/key or value parameters not provided."),
             @ApiResponse(code = HttpStatus.UNAUTHORIZED_401, message = "Specified user settings not found, ensure you are logged in."),
             @ApiResponse(code = HttpStatus.METHOD_NOT_ALLOWED_405, message = "the targeted resource does not support the requested HTTP method")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "Key name of the setting to be written", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "value", value = "Value of the setting specified", required = true, dataType = "string", paramType = "query")
    })
    @Override
    public String getAPIPath() {
        return "/aaa/changeUserSettings.json";
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
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {
        Map<String, String> settings = new HashMap<String, String>();
        boolean flag = false;
        for (int i = 0; i < possibleKeys.length; i++) {
            String value = query.get(possibleKeys[i], null);
            if(possibleKeys[i].equals("userName") && value != null){
                String usernamePattern = DAO.getConfig("users.username.regex", "^(.{5,51})$");
                String usernamePatternTooltip = DAO.getConfig("users.username.regex.tooltip",
                "Enter atleast 5 character, upto 51 character");
                Pattern pattern = Pattern.compile(usernamePattern);
                if(!pattern.matcher(value).matches()) {
                    throw new APIException(400, usernamePatternTooltip);
                }
            }
            if(value != null){
                settings.put(possibleKeys[i], value);
                flag = true;
            }
        }
        if(flag == false){
            throw new APIException(400, "Bad Service call, key or value parameters not provided");
        }
        if (authorization.getIdentity() == null) {
            throw new APIException(401, "Specified User Setting not found, ensure you are logged in");
        }
        else {
            String email = query.get("email", null);
            Accounting accounting;
            UserRole userRole = authorization.getUserRole();
            if((userRole.getName().equals("admin") || userRole.getName().equals("superadmin")) && email != null) {
                ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, email);
                Authorization userAuthorization = DAO.getAuthorization(identity);
                accounting = DAO.getAccounting(userAuthorization.getIdentity());
            } else {
                accounting = DAO.getAccounting(authorization.getIdentity());
            }
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(key, value);
                if (accounting.getJSON().has("settings")) {
                    accounting.getJSON().getJSONObject("settings").put(key, value);
                } else {
                    accounting.getJSON().put("settings", jsonObject);
                }
            }
            accounting.commit();
            JSONObject result = new JSONObject(true);
            result.put("accepted", true);
            result.put("message", "You successfully changed settings of your account!");
            return new ServiceResponse(result);
        }
    }
}
