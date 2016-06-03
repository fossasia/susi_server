/**
 *  Authentication
 *  Copyright 24.05.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.server;

import java.time.Instant;

import org.json.JSONObject;
import org.loklak.tools.storage.JsonFile;

/**
 * Authentication asks: who is the user. This class holds user identification details
 */
public class Authentication {

    private JsonFile parent;
    private JSONObject json;

    /**
     * create a new authentication object. The given json object must be taken
     * as value from a parent json. If the parent json is a JsonFile, then that
     * file can be handed over as well to enable persistency.
     * @param json object for storage of the authorization
     * @param parent the parent file or null if there is no parent file (no persistency)
     */
    public Authentication(JSONObject json, JsonFile parent) {
        this.json = json;
        this.parent = parent;
    }
    
    public Authentication setIdentity(ClientIdentity id) {
        this.json.put("id", id.toString());
        if (this.parent != null) this.parent.commit();
        return this;
    }
    
    public ClientIdentity getIdentity() {
        if (this.json.has("id")) return new ClientIdentity(this.json.getString("id"));
        return null;
    }

    public void setExpireTime(long time){
    	this.json.put("expires_on", Instant.now().getEpochSecond() + time);
    	if (this.parent != null) this.parent.commit();
    }
    
    public boolean checkExpireTime(){
    	if(this.json.has("expires_on") && this.json.getLong("expires_on") > Instant.now().getEpochSecond()) return true;
    	return false;
    }
}
