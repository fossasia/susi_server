/**
 *  PasswordRecoveryServlet
 *  Copyright 08.06.2016 by Shiven Mian, @shivenmian
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.json.JSONObject;
import org.loklak.LoklakEmailHandler;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;

import org.loklak.server.Query;

public class PasswordRecoveryService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 3515757746392011162L;

	@Override
	public String getAPIPath() {
		return "/api/recoverpassword.json";
	}

	@Override
	public APIServiceLevel getDefaultServiceLevel() {
		return APIServiceLevel.ANONYMOUS;
	}

	@Override
	public APIServiceLevel getCustomServiceLevel(Authorization auth) {
		return APIServiceLevel.ADMIN;
	}

	@Override
	public JSONObject serviceImpl(Query call, Authorization rights) throws APIException {
		JSONObject result = new JSONObject();

		String usermail;
		try {
			usermail = URLDecoder.decode(call.get("forgotemail", null), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			result.put("status", "error");
			result.put("reason", "malformed query");
			return result;
		}
		
		String subject = "Password Recovery";
		String body = "Recover password using this link";
		try {
			LoklakEmailHandler.sendEmail(usermail, subject, body);
			result.put("status", "ok");
			result.put("reason", "ok");
		} catch(Exception e){
			result.put("status", "error");
			result.put("reason", e.toString());
		}
		return result;
	}

	@Override
	public JSONObject getDefaultUserRights(APIServiceLevel serviceLevel) {
		// TODO Auto-generated method stub
		return null;
	}

}
