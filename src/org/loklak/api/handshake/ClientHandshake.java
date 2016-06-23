/**
 *  ClientHandshake
 *  Copyright 16.06.2015 by Robert Mader, @treba13
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

package org.loklak.api.handshake;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authentication;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.ClientCredential;
import org.loklak.server.ClientIdentity;
import org.loklak.server.Query;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class ClientHandshake extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 1111478303032749879L;
    private static long defaultExpireTime = 7 * 24 * 60 * 60;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.USER; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/api/handshake-client.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {
    	JSONObject result = new JSONObject();
    	
    	ClientIdentity identity = rights.getIdentity();
    	
    	if(!identity.isEmail()){
    		result.put("success", false);
    		result.put("message", "Your not logged in");
    		return result;
    	}
    	
    	String token = createRandomString(30);
        ClientCredential accessToken = new ClientCredential(ClientCredential.Type.access_token, token);
        Authentication tokenAuthentication = new Authentication(accessToken, DAO.authentication);
        tokenAuthentication.setIdentity(identity);
        
        long valid_seconds;
        try{
        	valid_seconds = post.get("valid_seconds", defaultExpireTime);
        }catch(NumberFormatException e){
        	valid_seconds = defaultExpireTime;
        }
    	
        if(valid_seconds == -1) {
        	result.put("valid_seconds", "forever");
        } // -1 means forever, don't add expire time
    	else if(valid_seconds == 0 || valid_seconds < -1){// invalid values, set default value
    		tokenAuthentication.setExpireTime(defaultExpireTime);
    		result.put("valid_seconds", defaultExpireTime);
    	}
    	else{
    		tokenAuthentication.setExpireTime(valid_seconds);
    		result.put("valid_seconds", valid_seconds);
    	}
        
        result.put("access_token", token);
    	
		return result;
    }
    
}
