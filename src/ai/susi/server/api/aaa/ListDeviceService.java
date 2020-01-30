package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.DateParser;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Necessary parameters : access_token, example:
 * http://localhost:4000/aaa/getDeviceList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 * Other parameter, (not necessary) search:
 * http://localhost:4000/aaa/getDeviceList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3&search=bob@builder.com
 */

public class ListDeviceService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -5000669108778105134L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.OPERATOR;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/getDeviceList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject();
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();
        List<JSONObject> deviceList = new ArrayList<JSONObject>();

        if (call.get("search",null) != null) {
            JSONObject json = new JSONObject();
            String email = call.get("search",null);
            ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, email);
            Authorization authorization = DAO.getAuthorization(identity);
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            if (accounting.getJSON().has("lastLoginIP")) {
                json.put("lastLoginIP", accounting.getJSON().getString("lastLoginIP"));
            }
            if (accounting.getJSON().has("lastLoginTime")) {
                String lastLoginTime = accounting.getJSON().getString("lastLoginTime");
                if (lastLoginTime.endsWith("0000")) { try { // time is in RFC1123, it should be in ISO8601: patching here; remove code later
                    Date d = DateParser.FORMAT_RFC1123.parse(lastLoginTime);
                    lastLoginTime = DateParser.formatISO8601(d);
                    accounting.getJSON().put("lastLoginTime", lastLoginTime);
                } catch (ParseException e) {e.printStackTrace();}}
                json.put("lastActive", lastLoginTime);
            }
            if (accounting.getJSON().has("devices")) {
                json.put("name", email);
                json.put("devices", accounting.getJSON().getJSONObject("devices"));
                deviceList.add(json);
            }
        } else {
            for (Client client : authorized) {
                JSONObject json = client.toJSON();
                String email = client.toString().substring(6);
                ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
                Authorization authorization = DAO.getAuthorization(identity);
                Accounting accounting = DAO.getAccounting(authorization.getIdentity());
                if (accounting.getJSON().has("lastLoginIP")) {
                    json.put("lastLoginIP", accounting.getJSON().getString("lastLoginIP"));
                }
                if (accounting.getJSON().has("lastLoginTime")) {
                    String lastLoginTime = accounting.getJSON().getString("lastLoginTime");
                    if (lastLoginTime.endsWith("0000")) { try { // time is in RFC1123, it should be in ISO8601: patching here; remove code later
                        Date d = DateParser.FORMAT_RFC1123.parse(lastLoginTime);
                        lastLoginTime = DateParser.formatISO8601(d);
                        accounting.getJSON().put("lastLoginTime", lastLoginTime);
                    } catch (ParseException e) {e.printStackTrace();}}
                    json.put("lastActive", lastLoginTime);
                }
                if (accounting.getJSON().has("devices")) {
                    json.put("devices", accounting.getJSON().getJSONObject("devices"));
                    deviceList.add(json);
                }
            }
        }
        try {
            result.put("devices", deviceList);
            result.put("accepted", true);
            result.put("message", "Success: Fetched all devices");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed to fetch the requested devices!");
        }
    }
}
