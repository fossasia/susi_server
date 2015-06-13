/**
 *  AccountServlet
 *  Copyright 27.05.2015 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.data.AccountEntry;
import org.loklak.data.DAO;
import org.loklak.data.UserEntry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

public class AccountServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
     
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
        if (!post.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost"); return;}
        
        // security
        boolean local = post.isLocalhostAccess();
        if (!local) {response.sendError(503, "access from localhost only"); return;}
        
        // parameters
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        boolean update = local && "update".equals(post.get("action", ""));
        String screen_name = post.get("screen_name", "");
        
        String data = post.get("data", "");
        if (update) {
            if  (data == null || data.length() == 0) {
                response.sendError(400, "your request does not contain a data object.");
                return;
             }
        
            // parse the json data
            try {
                XContentParser parser = JsonXContent.jsonXContent.createParser(data);
                Map<String, Object> map = parser == null ? null : parser.map();
                Object accounts_obj = map.get("accounts");
                List<Map<String, Object>> accounts;
                if (accounts_obj instanceof List<?>) {
                    accounts = (List<Map<String, Object>>) accounts_obj;
                } else {
                    accounts = new ArrayList<Map<String, Object>>(1);
                    accounts.add(map);
                }
                for (Map<String, Object> account: accounts) {
                    if (account == null) continue;
                    try {
                        AccountEntry a = new AccountEntry(account);
                        DAO.writeAccount(a, true);
                    } catch (IOException e) {
                        response.sendError(400, "submitted data is not well-formed: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                if (accounts.size() == 1) {
                    screen_name = (String) accounts.iterator().next().get("screen_name");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        UserEntry userEntry = DAO.searchLocalUser(screen_name);
        AccountEntry accountEntry = local ? DAO.searchLocalAccount(screen_name) : null; // DANGER! without the local we do not protet the account data
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        final StringWriter s = new StringWriter();
        JsonGenerator json = DAO.jsonFactory.createGenerator(s);
        json.setPrettyPrinter(minified ? new MinimalPrettyPrinter() : new DefaultPrettyPrinter());

        json.writeStartObject();

        json.writeObjectFieldStart("search_metadata");
        json.writeObjectField("count", userEntry == null ? "0" : "1");
        json.writeObjectField("client", post.getClientHost());
        json.writeEndObject(); // of search_metadata

        json.writeArrayFieldStart("accounts");
        if (accountEntry == null) {
            if (userEntry != null) AccountEntry.toEmptyAccountJSON(json, userEntry);
        } else {
            accountEntry.toJSON(json, userEntry);
        }
        json.writeEndArray(); // of users
        json.writeEndObject(); // of root
        json.close();
        
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(s.toString());
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
