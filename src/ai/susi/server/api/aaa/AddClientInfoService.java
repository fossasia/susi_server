/**
 *  AddClientInfoService
 *  Copyright by @Akshat-Jain on 05/06/18.
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
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Accounting;
import ai.susi.server.Authorization;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by @Akshat-Jain on 05/06/18.
 * Servlet to add Client information to the particular User whose Bot is being used by the Client
 * This service accepts 3 parameters - Client Email, Client Name, and Client Message
 * Test locally at http://127.0.0.1:4000/aaa/addClientInfo.json?clientEmail=clientEmail@gmail.com&clientName=ClientName&clientMessage=ClientMessage&access_token=D0eBycAurNHTsi1BRUg0zXCyN7zRNm
 */
public class AddClientInfoService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 6707275834657921794L;

    @Override
    public String getAPIPath() {
        return "/aaa/addClientInfo.json";
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        String clientEmail = call.get("clientEmail", null);
        String clientName = call.get("clientName", null);
        String clientMessage = call.get("clientMessage", null);

        if (clientEmail == null || clientName == null || clientMessage == null) {
            throw new APIException(400, "Bad service call, missing arguments");
        }

        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Specified user data not found, ensure you are logged in");
        }

        String userEmail = authorization.getIdentity().getName();

        JsonTray chatbotClients = DAO.chatbotClients;

        JSONObject client = new JSONObject();
        JSONObject clientInfo = new JSONObject();
        JSONObject allClientsJSONObject = new JSONObject();
        JSONArray allClients = new JSONArray();

        clientInfo.put("email",clientEmail);
        clientInfo.put("name",clientName);
        clientInfo.put("message",clientMessage);

        if(chatbotClients.has(userEmail)) {
            JSONObject user = new JSONObject();
            user = chatbotClients.getJSONObject(userEmail);
            if(user.has("clients")) {
                allClients = user.getJSONArray("clients");
            }
        }

        boolean alreadyPresent = false;

        for (int i = 0 ; i < allClients.length() ; i++ ) {
            if(allClients.getJSONObject(i).get("email").equals(clientEmail)) {
                allClients.getJSONObject(i).put("name", clientName);
                allClients.getJSONObject(i).put("message", clientMessage);
                alreadyPresent = true;
                break;
            }
        }

        if(!alreadyPresent) {
            allClients.put(clientInfo);
        }

        allClientsJSONObject.put("clients",allClients);
        chatbotClients.put(userEmail,allClientsJSONObject,true);

        JSONObject result = new JSONObject();
        result.put("accepted", true);
        result.put("message", "You have successfully added the client information.");

        return new ServiceResponse(result);
    }
}
