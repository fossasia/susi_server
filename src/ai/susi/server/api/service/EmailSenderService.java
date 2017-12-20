/**
 *  EmailSenderService
 *  Copyright 26.11.2016 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.service;

import ai.susi.EmailHandler;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * send an email in the name of the registered user
 * example:
 * http://127.0.0.1:4000/service/emailsender.json?mail=you@xyz.abc|test|123
 *
 */
public class EmailSenderService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/service/emailsender.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject json = new JSONObject(true).put("accepted", true);
        
        // unauthenticated users are rejected
        if (user.getIdentity().isAnonymous()) {
            json.put("accepted", false);
            json.put("reject-reason", "you must be logged in");
            return new ServiceResponse(json);
        }
        
        String mail = post.get("mail", "");
        
        if (mail.length() == 0) {
            json.put("accepted", false);
            json.put("reject-reason", "a mail attribute is required");
            return new ServiceResponse(json);
        }
        
        String[] m = mail.split("\\|");
        if (m.length != 3) {
            json.put("accepted", false);
            json.put("reject-reason", "the mail attribute must contain three parts (receiver, subject, text) separated by a | symbol");
            return new ServiceResponse(json);
        }
        
        // thats it, send the email
        String sender = user.getIdentity().getName();
        String addressTo = m[0];
        String subject = m[1];
        String text = m[2];
        
        try {
            EmailHandler.sendEmail(sender, sender, addressTo, subject, text);
        } catch (Exception e) {
            json.put("accepted", false);
            json.put("reject-reason", "cannot send mail: " + e.getMessage());
            return new ServiceResponse(json);
        }
        
        // success
        return new ServiceResponse(json);
    }
    
}
