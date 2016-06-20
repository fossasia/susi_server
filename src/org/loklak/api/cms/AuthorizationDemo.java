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

package org.loklak.api.cms;

import org.json.JSONObject;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;

public class AuthorizationDemo extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8678478303032749879L;

    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.ANONYMOUS;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(Authorization rights) {
        if(rights.isAdmin()){
        	return APIServiceLevel.ADMIN;
        } else if(rights.getIdentity() != null){
        	return APIServiceLevel.USER;
        }
        return APIServiceLevel.ANONYMOUS;
    }

    public String getAPIPath() {
        return "/api/authorization-demo.json";
    }
    
    @Override
    public JSONObject getDefaultUserRights(APIServiceLevel serviceLevel){
    	JSONObject result = new JSONObject();
    	
    	switch(serviceLevel){
    		case ADMIN:
    		case MODERATOR:
    		case SERVICE_MANAGER:
    		case USER:
    			result.put("download_allowed", true);
    			break;
    		default:
    			result.put("download_allowed", false);
    			break;
    	}
    	
    	return result;
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {
    	
    	JSONObject result = new JSONObject();
    	
		
		return result;
    }
}
