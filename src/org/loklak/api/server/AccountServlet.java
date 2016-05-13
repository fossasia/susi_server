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
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.AccountEntry;
import org.loklak.objects.UserEntry;

public class AccountServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        if (!post.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost, your request comes from " + post.getClientHost()); return;} // danger! do not remove this!
        process(request, response, post);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        if (!post.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost, your request comes from " + post.getClientHost()); return;} // danger! do not remove this!
        post.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, post);
    }
    
    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {

        // parameters
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        boolean update = "update".equals(post.get("action", ""));
        String screen_name = post.get("screen_name", "");
        
        String data = post.get("data", "");
        if (update) {
            if  (data == null || data.length() == 0) {
                response.sendError(400, "your request does not contain a data object.");
                return;
             }
        
            // parse the json data
            try {
                JSONObject json = new JSONObject(data);
                Object accounts_obj = json.has("accounts") ? json.get("accounts") : null;
                JSONArray accounts;
                if (accounts_obj != null && accounts_obj instanceof JSONArray) {
                    accounts = (JSONArray) accounts_obj;
                } else {
                    accounts = new JSONArray();
                    accounts.put(json);
                }
                for (Object account_obj: accounts) {
                    if (account_obj == null) continue;
                    try {
                        AccountEntry a = new AccountEntry((JSONObject) account_obj);
                        DAO.writeAccount(a, true);
                    } catch (IOException e) {
                        response.sendError(400, "submitted data is not well-formed: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                if (accounts.length() == 1) {
                    screen_name = (String) ((JSONObject) accounts.iterator().next()).get("screen_name");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        UserEntry userEntry = DAO.searchLocalUserByScreenName(screen_name);
        AccountEntry accountEntry = DAO.searchLocalAccount(screen_name);
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        JSONObject m = new JSONObject(true);
        JSONObject metadata = new JSONObject(true);
        metadata.put("count", userEntry == null ? "0" : "1");
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);

        // create a list of accounts. Why a list? Because the same user may have accounts for several services.
        JSONArray accounts = new JSONArray();
        if (accountEntry == null) {
            if (userEntry != null) accounts.put(AccountEntry.toEmptyAccountJson(userEntry));
        } else {
            accounts.put(accountEntry.toJSON(userEntry));
        }
        m.put("accounts", accounts);
        
        // write json
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(m.toString(minified ? 0 : 2));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
