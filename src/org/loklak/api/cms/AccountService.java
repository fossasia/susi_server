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

package org.loklak.api.cms;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.objects.AccountEntry;
import org.loklak.objects.UserEntry;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;

public class AccountService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ADMIN; }

    @Override
    public String getAPIPath() {
        return "/api/account.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {

        // parameters
        boolean update = "update".equals(post.get("action", ""));
        String screen_name = post.get("screen_name", "");
        
        String data = post.get("data", "");
        if (update) {
            if  (data == null || data.length() == 0) {
                throw new APIException(400, "your request does not contain a data object.");
             }
        
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
                    throw new APIException(400, "submitted data is not well-formed: " + e.getMessage());
                }
            }
            if (accounts.length() == 1) {
                screen_name = (String) ((JSONObject) accounts.iterator().next()).get("screen_name");
            }
        }

        UserEntry userEntry = DAO.searchLocalUserByScreenName(screen_name);
        AccountEntry accountEntry = DAO.searchLocalAccount(screen_name);
        
        
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
        
        return m;
    }
}
