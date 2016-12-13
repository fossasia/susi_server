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

package org.loklak.api.service;

import org.json.JSONObject;
import org.loklak.EmailHandler;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.tools.storage.JSONObjectWithDefault;

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
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/service/emailsender.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization user, final JSONObjectWithDefault permissions) throws APIException {
        JSONObject json = new JSONObject(true).put("accepted", true);
        
        // unauthenticated users are rejected
        if (user.getIdentity().isAnonymous()) {
            json.put("accepted", false);
            json.put("reject-reason", "you must be logged in");
            return json;
        }
        
        String mail = post.get("mail", "");
        
        if (mail.length() == 0) {
            json.put("accepted", false);
            json.put("reject-reason", "a mail attribute is required");
            return json;
        }
        
        String[] m = mail.split("\\|");
        if (m.length != 3) {
            json.put("accepted", false);
            json.put("reject-reason", "the mail attribute must contain three parts (receiver, subject, text) separated by a | symbol");
            return json;
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
            return json;
        }
        
        // success
        return json;
    }
    
}
