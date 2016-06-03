/**
 *  ClientService
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

import java.text.ParseException;
import java.util.Date;

import org.json.JSONObject;
import org.loklak.tools.DateParser;

public class ClientService extends Client {

    private JSONObject json;
    
    public enum Type {
        apiAccess, // service grants access to a specific api
    }
    
    public ClientService(String rawIdString) {
        super(rawIdString);
        this.json = new JSONObject(true);
    }
    
    public ClientService(Type type, String untypedId) {
        super(type.name(), untypedId);
        this.json = new JSONObject(true);
    }
    
    public ClientService setMetadata(JSONObject json) {
        this.json = json;
        return this;
    }

    public ClientService setAccessFrom(Date date) {
        this.json.put("accessFrom", DateParser.iso8601Format.format(date));
        return this;
    }

    public ClientService setAccessUntil(Date date) {
        this.json.put("accessUntil", DateParser.iso8601Format.format(date));
        return this;
    }
    
    public Date getAccessFrom() {
        if (!this.json.has("accessFrom")) return null;
        return parse(this.json.getString("accessFrom"));
    }
    
    public Date getAccessUntil() {
        if (!this.json.has("accessUntil")) return null;
        return parse(this.json.getString("accessUntil"));
    }
    
    private Date parse(String d) {
        try {
            return DateParser.iso8601Format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Type getType() {
        return Type.valueOf(this.getKey());
    }

    public JSONObject toJSON() {
        JSONObject j = super.toJSON();
        j.put("meta", this.json);
        return j;
    }
    
    
}
