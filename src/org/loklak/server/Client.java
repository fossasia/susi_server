/**
 *  Client
 *  Copyright 03.06.2016 by Michael Peter Christen, @0rb1t3r
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

public class Client {

    private final static char SEPARATOR = ':';
    
    private String id;
    private int separatorPos;

    protected Client(String rawIdString) {
        this.separatorPos = rawIdString.indexOf(SEPARATOR);
        assert this.separatorPos >= 0;
        this.id = rawIdString;
    }

    protected Client(String typeName, String untypedId) {
        this.id = typeName + SEPARATOR + untypedId;
        this.separatorPos = typeName.length();
    }
    
    protected String getKey() {
        return id.substring(0, this.separatorPos);
    }
    
    public String getName() {
        return this.id.substring(this.separatorPos + 1);
    }
    
    public String toString() {
        return this.id;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        json.put("type", getKey());
        json.put("name", this.getName());
        return json;
    }
}
