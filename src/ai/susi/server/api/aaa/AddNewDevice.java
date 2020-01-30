/**
 * AddNewDevice
 * Copyright by @Akshat-Jain on 24/5/18.
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
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Accounting;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.TimeoutMatcher;

import io.swagger.annotations.*;
import org.json.JSONObject;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import ai.susi.tools.DateParser;


/**
 * Created by @Akshat-Jain on 24/5/18.
 * Servlet to add device information
 * This service accepts 5 parameters - Mac address of device, device name, room, latitude and longitude info of the device - and stores them in the user's accounting object
 * test locally at http://127.0.0.1:4000/aaa/addNewDevice.json?macid=macAddressOfDevice&device=deviceName&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */

@Path("/aaa/addNewDevice.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "AddNewDeviceService", description = "This Endpoint adds new device")
public class AddNewDevice extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -8742102844146051190L;

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to add new device")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "You have successfully added the new device!"),
            @ApiResponse(code = 400, message = "Invalid Mac Address."),
            @ApiResponse(code = 401, message = "Specified user data not found, ensure you are logged in.")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "macid", value = "Mac Address of new device", required = true, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "device", value = "Name of new device", required = true, dataType = "string", paramType = "query")
    })
    @Override
    public String getAPIPath() {
        return "/aaa/addNewDevice.json";
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

        JSONObject value = new JSONObject();
        JSONObject geolocation = new JSONObject();

        String macid = query.get("macid", null);
        String name = query.get("name", null);
        String room = query.get("room", null);
        String latitude = query.get("latitude", null);
        String longitude = query.get("longitude", null);

        if (macid == null || name == null || room == null) {
            throw new APIException(400, "Bad service call, missing arguments");
        }

        String PATTERN = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
        Pattern pattern = Pattern.compile(PATTERN);
        if (!new TimeoutMatcher(pattern.matcher(macid)).matches()) {
            throw new APIException(400, "Invalid Mac Address.");
        }

        if (latitude == null || latitude.isEmpty()) {
            latitude = "Latitude not available.";
        }

        if (longitude == null || longitude.isEmpty()) {
            longitude = "Longitude not available.";
        }

        Date currentTime = new Date();

        geolocation.put("latitude", latitude);
        geolocation.put("longitude", longitude);
        value.put("name", name);
        value.put("room", room);
        value.put("geolocation", geolocation);
        value.put("deviceAddTime", DateParser.formatISO8601(currentTime));

        if (authorization.getIdentity() == null) {
            throw new APIException(401, "Specified user data not found, ensure you are logged in");
        } else {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            if (accounting.getJSON().has("devices")) {
                accounting.getJSON().getJSONObject("devices").put(macid, value);
            } else {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(macid, value);
                accounting.getJSON().put("devices", jsonObject);
            }
            accounting.commit();

            JSONObject result = new JSONObject(true);
            result.put("accepted", true);
            result.put("message", "You have successfully added the device!");
            return new ServiceResponse(result);
        }

    }
}
