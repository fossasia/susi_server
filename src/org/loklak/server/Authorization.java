/**
 *  Authorization
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r
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

import org.json.JSONObject;

/**
 * Authorization asks: what is the user allowed to do? This class holds user rights.
 * An object instance of this class is handed to each serivce call to enable that
 * service to work according to granted service level.
 * One part of authorization decisions is the history of the past user action.
 * Therefore an authorization object has the attachment of an Accounting object;  
 */
public class Authorization extends JSONObject {

    private Accounting accounting;

    public Authorization() {
        super();
        this.accounting = null;
    }
    
    public Authorization(JSONObject json) {
        super(json);
        this.accounting = null;
    }
    
    public Accounting setAccounting(Accounting accounting) {
        this.accounting = accounting;
        return this.accounting;
    }
    
    public Accounting getAccounting() {
        return this.accounting;
    }
    
    public Authorization setAdmin() {
        this.put("admin", true);
        return this;
    }
    
    boolean isAdmin() {
        if (!this.has("admin")) return false;
        return this.getBoolean("admin");
    }
    
    public Authorization setRequestFrequency(String path, int reqPerHour) {
        if (!this.has("frequency")) {
            this.put("frequency", new JSONObject());
        }
        JSONObject paths = this.getJSONObject("frequency");
        paths.put(path, reqPerHour);
        return this;
    }
    
    public int getRequestFrequency(String path) {
        if (!this.has("frequency")) return -1;
        JSONObject paths = this.getJSONObject("frequency");
        if (!paths.has(path)) return -1;
        return paths.getInt(path);
    }

}
