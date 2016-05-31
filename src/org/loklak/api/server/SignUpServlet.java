/**
 *  SignUpServlet
 *  Copyright 27.05.2015 by Robert Mader, @treba13
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

package org.loklak.api.server;

import java.util.Base64;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Credential;
import org.loklak.server.Identity;
import org.loklak.server.Query;

public class SignUpServlet extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(Authorization rights) {
        return APIServiceLevel.ADMIN;
    }

    public String getAPIPath() {
        return "/api/signup.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {

    	JSONObject result = new JSONObject();
    	
    	if(post.get("email",null) == null || post.get("pass", null) == null){
    		result.put("status", "error");
    		result.put("reason", "email or password empty");
    		return result;
    	}
    	
    	Credential credential = new Credential(Credential.Type.passwdLogin, post.get("email",null));
    	if (DAO.authentication.has(credential.toString())) {
    		result.put("status", "error");
    		result.put("reason", "email already taken");
    		return result;
    	}
    	
    	JSONObject user_obj = new JSONObject();
    	String salt = createRandomSalt();
    	user_obj.put("salt", salt);
    	user_obj.put("password", getHash(new String(Base64.getDecoder().decode(post.get("pass", null))), salt));
    	Identity identity = new Identity(Identity.Type.email, credential.getPayload());
    	user_obj.put("id",identity.toString());
        DAO.authentication.put(credential.toString(), user_obj);
    	
    	result.put("status", "ok");
		result.put("reason", "ok");
		return result;
    }
    
}
