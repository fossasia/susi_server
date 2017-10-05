/**
 *  Accounting
 *  Copyright 16.06.2017 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonTray;

import javax.annotation.Nonnull;

public class Accounting {

    private JsonTray parent;
    private JSONObject json;
    private UserRequests requests;
    private ClientIdentity identity;
    
    /**
     * create a new authorization object. The given json object must be taken
     * as value from a parent json. If the parent json is a JsonFile, then that
     * file can be handed over as well to enable persistency.
     * @param identity
     * @param parent the parent file or null if there is no parent file (no persistency)
     */
    public Accounting(@Nonnull ClientIdentity identity, JsonTray parent) {

        DAO.severe("new accounting");

        this.parent = parent;
        this.requests = new UserRequests(); // temporary user request space
        this.identity = identity;

        if(parent != null){
            if (parent.has(identity.toString())) {
                json = parent.getJSONObject(identity.toString());
            } else {
                json = new JSONObject();
                parent.put(identity.toString(), json, identity.isPersistent());
            }
        }
        else json = new JSONObject();

    }
    
    public UserRequests getRequests() {
        return this.requests;
    }
    
    public ClientIdentity getIdentity() {
        return identity;
    }
    
    public JSONObject getJSON() {
        return this.json;
    }

    public JsonTray getParent() { return this.parent; }
}
