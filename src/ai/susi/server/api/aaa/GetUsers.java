/**
 *  GetUsers
 *  Created by chetankaushik on 31/05/17.
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
import ai.susi.mind.SusiCognition;
import ai.susi.server.*;
import ai.susi.tools.DateParser;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Iterator;

/**
 * Created by chetankaushik on 31/05/17.
 * This Servlet gives a API Endpoint to list all the users and their roles.
 * It requires user role to be OPERATOR or above ADMIN
 * example:
 * http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ
 * Necessary parameters : access_token
 * Other parameters (one out of two is necessary):
 * getPageCount -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getPageCount=true
 * getUserCount -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getUserCount=true
 * getUserStats -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getUserStats=true
 * getDeviceStats -> boolean http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&getDeviceStats=true
 * search       -> string http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&search=mario
 * page         -> integer http://localhost:4000/aaa/getUsers.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&page=2
 */
public class GetUsers extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4538304346942632187L;

    @Override
    public String getAPIPath() {
        return "/aaa/getUsers.json";
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        if (call.get("getPageCount", false) == false
                && call.get("getUserStats", false) == false
                && call.get("search", null) == null
                && call.get("page", null) == null
                && call.get("getUserCount", null) == null
                && call.get("getDeviceStats", null) == null) {
            throw new APIException(400, "Bad Request. No parameter present");
        }
        JSONObject result = new JSONObject(true);
        JSONObject userStats = new JSONObject(true);
        JSONObject deviceStats = new JSONObject(true);
        result.put("accepted", false);
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<String> keysList = new ArrayList<String>();
        authorized.forEach(client -> keysList.add(client.toString()));
        String[] keysArray = keysList.toArray(new String[keysList.size()]);
        String searchTerm = call.get("search", null);
        if (call.get("getPageCount", false) == true) {
            int pageCount = keysArray.length % 50 == 0 ? (keysArray.length / 50) : (keysArray.length / 50) + 1;
            result.put("pageCount", pageCount);
            result.put("accepted", true);
            result.put("message", "Success: Fetched count of pages");
            return new ServiceResponse(result);
        }
        if (call.get("getUserCount", false) == true) {
            result.put("userCount", keysArray.length);
            result.put("accepted", true);
            result.put("message", "Success: Fetched count of users");
            return new ServiceResponse(result);
        } else {
            int page = call.get("page", 0);
            int anonymous = 0;
            int user = 0;
            int reviewer = 0;
            int operator = 0;
            int admin = 0;
            int superAdmin = 0;
            int activeUsers = 0;
            int inactiveUsers = 0;

            // device stats
            int connectedDevices =0;
            int deviceUsers = 0;

            page = (page - 1) * 50;
            List<JSONObject> userList = new ArrayList<JSONObject>();
            JSONObject lastLoginOverTimeObj = new JSONObject();
            JSONObject signupOverTimeObj = new JSONObject();
            JSONObject deviceAddedOverTimeObj = new JSONObject();
  
            //authorized.forEach(client -> userList.add(client.toJSON()));
            for (Client client : authorized) {
                String email = client.toString().substring(6); 
                if (searchTerm == null || email.contains(searchTerm)){
                  JSONObject json = client.toJSON();
                  // generate client identity to get user role
                  ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
                  Authorization authorization = DAO.getAuthorization(identity);
                  UserRole userRole = authorization.getUserRole();

                  // count user roles
                  if (userRole.toString().toLowerCase().equals("anonymous")){
                    ++anonymous;
                  }
                  if (userRole.toString().toLowerCase().equals("user")){
                    ++user;
                  }
                  if (userRole.toString().toLowerCase().equals("reviewer")){
                    ++reviewer;
                  }
                  if (userRole.toString().toLowerCase().equals("operator")){
                    ++operator;
                  }
                  if (userRole.toString().toLowerCase().equals("admin")){
                    ++admin;
                  }
                  if (userRole.toString().toLowerCase().equals("superadmin")){
                    ++superAdmin;
                  }

                  // put user role in response
                  json.put("userRole", userRole.toString().toLowerCase());

                  //generate client credentials to get status whether verified or not
                  ClientCredential clientCredential = new ClientCredential(ClientCredential.Type.passwd_login, identity.getName());
                  Authentication authentication = DAO.getAuthentication(clientCredential);

                  //count verified status
                  if (authentication.getBoolean("activated", false)){
                    ++activeUsers;
                  }else{
                    ++inactiveUsers;
                  }

                  //put verified status in response
                  json.put("confirmed", authentication.getBoolean("activated", false));

                  /* Generate accounting object to get details like last login IP,
                   * signup time and last login time and put it in the response
                   * */
                  Accounting accounting = DAO.getAccounting(authorization.getIdentity());
                  if (accounting.getJSON().has("lastLoginIP")) {
                      json.put("lastLoginIP", accounting.getJSON().getString("lastLoginIP"));
                  } else {
                      json.put("lastLoginIP", "");
                  }

                  if (!accounting.getJSON().has("signupTime")) {
                      // the time was missing because of an bug. We patch that here.
                      // after all missing signupTime values have been patched, the code can be removed again.
                      // we reconstruct the signupTime using the first entry in the chatlog.
                      SusiCognition cog = DAO.susi_memory.firstCognition(authorization.getIdentity().getClient());
                      if (cog != null) {
                          accounting.getJSON().put("signupTime", DateParser.formatISO8601(cog.getQueryDate()));
                      }
                  }

                  if (accounting.getJSON().has("signupTime")) {
                      String signupTime = accounting.getJSON().getString("signupTime");
                      if (signupTime.endsWith("0000")) { try { // time is in RFC1123, it should be in ISO8601: patching here; remove code later
                          Date d = DateParser.FORMAT_RFC1123.parse(signupTime);
                          signupTime = DateParser.formatISO8601(d);
                          accounting.getJSON().put("signupTime", signupTime);
                      } catch (ParseException e) {e.printStackTrace();}}
                      json.put("signupTime", signupTime);
                      signupTime = signupTime.substring(0, 7);
                      if (signupOverTimeObj.has(signupTime)){
                          int count = signupOverTimeObj.getInt(signupTime);
                          signupOverTimeObj.put(signupTime, count + 1);
                      } else {
                          signupOverTimeObj.put(signupTime, 1);
                      }
                  } else {
                      json.put("signupTime", "");
                  }

                    if (accounting.getJSON().has("lastLoginTime")) {
                        String lastLoginTime = accounting.getJSON().getString("lastLoginTime");
                        if (lastLoginTime.endsWith("0000")) { try { // time is in RFC1123, it should be in ISO8601: patching here; remove code later
                            Date d = DateParser.FORMAT_RFC1123.parse(lastLoginTime);
                            lastLoginTime = DateParser.formatISO8601(d);
                            accounting.getJSON().put("lastLoginTime", lastLoginTime);
                        } catch (ParseException e) {e.printStackTrace();}}
                        json.put("lastLoginTime", lastLoginTime);
                        lastLoginTime = lastLoginTime.substring(0, 7);
                        if (lastLoginOverTimeObj.has(lastLoginTime)) {
                            int count = lastLoginOverTimeObj.getInt(lastLoginTime);
                            lastLoginOverTimeObj.put(lastLoginTime, count + 1);
                        } else {
                            lastLoginOverTimeObj.put(lastLoginTime, 1);
                        }
                  } else {
                      json.put("lastLoginTime", "");
                  }

                  if(accounting.getJSON().has("devices")) {
                      JSONObject devices = accounting.getJSON().getJSONObject("devices");
                      json.put("devices", devices);
                      Iterator<?> keys = devices.keySet().iterator();
                      while(keys.hasNext() ) {
                        String key = (String)keys.next();
                        if (devices.get(key) instanceof JSONObject) {
                            JSONObject device = new JSONObject(devices.get(key).toString());
                            String deviceAddTime = device.optString("deviceAddTime");
                            if (deviceAddTime != null && deviceAddTime.length() > 0) {
                                try {
                                    Date d = DateParser.FORMAT_RFC1123.parse(deviceAddTime);
                                    deviceAddTime = DateParser.formatISO8601(d).substring(0,7);
                                } catch (ParseException e) {
                                    try {
                                    Date d = DateParser.iso8601MillisFormat.parse(deviceAddTime);
                                    deviceAddTime = DateParser.formatISO8601(d).substring(0,7);
                                    } catch (ParseException ee) {
                                        ee.printStackTrace();
                                    }
                                }
                                if(deviceAddedOverTimeObj.has(deviceAddTime)) {
                                    int count = deviceAddedOverTimeObj.getInt(deviceAddTime);
                                    deviceAddedOverTimeObj.put(deviceAddTime, count + 1);
                                } else {
                                    deviceAddedOverTimeObj.put(deviceAddTime, 1);
                                }
                            }
                        }
                      }
                      connectedDevices = connectedDevices + devices.length();
                      ++deviceUsers;
                  } else {
                      json.put("devices", "");
                  }

                  if(accounting.getJSON().has("settings")) {
                      JSONObject settings = accounting.getJSON().getJSONObject("settings");
                      if(settings.has("userName")) {
                        json.put("userName", settings.get("userName"));
                      }
                      else {
                          json.put("userName", "");
                      }
                  } else {
                      json.put("userName", "");
                  }

                  //add the user details in the list
                  userList.add(json);
              }
            }

            List<JSONObject> currentPageUsers = new ArrayList<JSONObject>();
            int length = userList.size() - page > 50 ? 50 : (userList.size() - page);
            userStats.put("anonymous", anonymous);
            userStats.put("users", user);
            userStats.put("reviewers", reviewer);
            userStats.put("operators", operator);
            userStats.put("admins", admin);
            userStats.put("superAdmins", superAdmin);
            userStats.put("activeUsers", activeUsers);
            userStats.put("inactiveUsers", inactiveUsers);
            userStats.put("totalUsers", keysArray.length);

            deviceStats.put("connectedDevices", connectedDevices);
            deviceStats.put("deviceUsers", deviceUsers);

            if (call.get("getUserStats", false) == true) {
              try {
                List<JSONObject> lastLoginOverTimeList = new ArrayList<JSONObject>();
                for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(lastLoginOverTimeObj))) {
                  JSONObject timeObj = new JSONObject();
                  timeObj.put("timeStamp", timeStamp);
                  timeObj.put("count", lastLoginOverTimeObj.getInt(timeStamp));
                  lastLoginOverTimeList.add(timeObj);
                }
                List<JSONObject> signupOverTimeList = new ArrayList<JSONObject>();
                for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(lastLoginOverTimeObj))) {
                  JSONObject timeObj = new JSONObject();
                  timeObj.put("timeStamp", timeStamp);
                  timeObj.put("count", signupOverTimeObj.optInt(timeStamp, 0));
                  signupOverTimeList.add(timeObj);
                }
                  result.put("lastLoginOverTime", lastLoginOverTimeList);
                  result.put("signupOverTime", signupOverTimeList);
                  result.put("userStats", userStats);
                  result.put("accepted", true);
                  result.put("message", "Success: Fetched all users stats!");
                  return new ServiceResponse(result);
              } catch (Exception e) {
                  throw new APIException(500, "Failed to fetch the requested list!");
              }
            } else if (call.get("search", null) != null) {
                try {
                  result.put("users", userList);
                  result.put("accepted", true);
                  result.put("message", "Success: Fetched all users with " + call.get("search", null) + " !");
                  return new ServiceResponse(result);
              } catch (Exception e) {
                  throw new APIException(500, "Failed to fetch the requested users!");
              }
            } else if (call.get("getDeviceStats", null) != null) {
                try {
                  List<JSONObject> deviceAddedOverTimeList = new ArrayList<JSONObject>();
                  for(String timeStamp: Objects.requireNonNull(JSONObject.getNames(deviceAddedOverTimeObj))){
                    JSONObject timeObj = new JSONObject();
                    timeObj.put("timeStamp", timeStamp);
                    timeObj.put("count", deviceAddedOverTimeObj.getInt(timeStamp));
                    deviceAddedOverTimeList.add(timeObj);
                  }
                  deviceStats.put("deviceAddedOverTime", deviceAddedOverTimeList);

                  result.put("deviceStats", deviceStats);
                  result.put("accepted", true);
                  result.put("message", "Success: Fetched all device stats!");
                  return new ServiceResponse(result);
                } catch (Exception e) {
                  throw new APIException(500, "Failed to fetch the device stats!");
              }
            } else {
                try {
                  String[] currentKeysArray = Arrays.copyOfRange(keysArray, page, page + length);
                  for (int i = 0; i < length; ++i) {
                      currentPageUsers.add(userList.get(page + i));
                  }
                  result.put("users", currentPageUsers);
                  result.put("username", currentKeysArray);
                  result.put("accepted", true);
                  result.put("message", "Success: Fetched all Users with their User Roles and connected devices!");
                  return new ServiceResponse(result);
                } catch (Exception e) {
                    throw new APIException(500, "Failed to fetch the requested list!");
              }
            }
        }
    }

}
