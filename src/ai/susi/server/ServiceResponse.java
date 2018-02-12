/**
 *  ServiceResponse
 *  Copyright 16.01.2017 by Michael Peter Christen, @0rb1t3r
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

import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * wrapper class for the result type of services: either
 * JSONObject, JSONArray,  String, or byte[]
 */
public class ServiceResponse {

    private Object object;
    private boolean setCORS;
    
    public ServiceResponse(JSONObject json) {
        this.object = json;
        this.setCORS = true;
    }
    
    public ServiceResponse(JSONArray json) {
        this.object = json;
        this.setCORS = true;
    }
    
    public ServiceResponse(String string) {
        this.object = string;
        this.setCORS = true;
    }
    
    public ServiceResponse(byte[] bytes) {
        this.object = bytes;
        this.setCORS = true;
    }
    
    public ServiceResponse enableCORS() {
        this.setCORS = true;
        return this;
    }
    
    public ServiceResponse disableCORS() {
        this.setCORS = false;
        return this;
    }
    
    public boolean hasCORS() {
        return this.setCORS;
    }
    
    public boolean isObject() {
        return this.object instanceof JSONObject;
    }
    
    public boolean isArray() {
        return this.object instanceof JSONArray;
    }
    
    public boolean isString() {
        return this.object instanceof String;
    }
    
    public boolean isByteArray() {
        return this.object instanceof byte[];
    }
    
    public JSONObject getObject() throws JSONException {
        if (!isObject()) throw new JSONException("object type is not JSONObject: " + this.object.getClass().getName());
        return (JSONObject) this.object;
    }
    
    public JSONArray getArray() throws JSONException {
        if (!isArray()) throw new JSONException("object type is not JSONArray: " + this.object.getClass().getName());
        return (JSONArray) this.object;
    }
    
    public String getString() throws JSONException {
        if (!isString()) throw new JSONException("object type is not String: " + this.object.getClass().getName());
        return (String) this.object;
    }
    
    public byte[] getByteArray() throws JSONException {
        if (!isByteArray()) throw new JSONException("object type is not ByteArray: " + this.object.getClass().getName());
        return (byte[]) this.object;
    }
    
    public String getMimeType() {
        if (isObject() || isArray()) return "application/javascript";
        if (isString()) return "text/plain";
        return "application/octet-stream";
    }
    
    public String toString(boolean minified) {
        if (isObject()) return getObject().toString(minified ? 0 : 2);
        if (isArray()) return getArray().toString(minified ? 0 : 2);
        if (isString()) return getString();
        if (isByteArray()) return new String((byte[]) this.object, StandardCharsets.UTF_8);
        return null;
    }
    
    public byte[] toByteArray(boolean minified) {
        if (isObject()) return getObject().toString(minified ? 0 : 2).getBytes(StandardCharsets.UTF_8);
        if (isArray()) return getArray().toString(minified ? 0 : 2).getBytes(StandardCharsets.UTF_8);
        if (isString()) return getString().getBytes(StandardCharsets.UTF_8);
        if (isByteArray()) return (byte[]) this.object;
        return null;
    }
}
