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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.AccountEntry;
import org.loklak.data.DAO;
import org.loklak.data.UserEntry;
import org.loklak.harvester.TwitterAPI;

import twitter4j.TwitterException;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        if (!post.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost, your request comes from " + post.getClientHost()); return;} // danger! do not remove this!
        
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
                Map<String, Object> map = DAO.jsonMapper.readValue(data, DAO.jsonTypeRef);
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

        UserEntry userEntry = DAO.searchLocalUserByScreenName(screen_name);
        AccountEntry accountEntry = DAO.searchLocalAccount(screen_name);
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("count", userEntry == null ? "0" : "1");
        metadata.put("client", post.getClientHost());
        m.put("search_metadata", metadata);

        // create a list of accounts. Why a list? Because the same user may have accounts for several services.
        List<Object> accounts = new ArrayList<>();
        if (accountEntry == null) {
            if (userEntry != null) accounts.add(AccountEntry.toEmptyAccount(userEntry));
        } else {
            accounts.add(accountEntry.toMap(userEntry));
        }
        m.put("accounts", accounts);
        
        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print((minified ? new ObjectMapper().writer() : new ObjectMapper().writerWithDefaultPrettyPrinter()).writeValueAsString(m));
        if (jsonp) sos.println(");");
        sos.println();
    }
    
}
