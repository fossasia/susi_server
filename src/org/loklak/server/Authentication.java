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
import org.loklak.tools.storage.JsonTray;

import javax.annotation.Nonnull;

/**
 * Authentication asks: who is the user. This class holds user identification details
 */
public class Authentication {

    private JsonTray parent;
    private JSONObject json;
    private ClientCredential credential;

    /**
     * create a new authentication object. Creates a JSONObject or reads a from the parent.
     * The parent is used to enable persistency.
     * @param credential a ClientCredential for which the Authentication should created for
     * @param parent the storage object or null if there is no parent file (no persistency)
     */
    public Authentication(@Nonnull ClientCredential credential, JsonTray parent) {
    	if(parent != null){
	    	if(parent.has(credential.toString())){
	        	this.json = parent.getJSONObject(credential.toString());
	        }
	        else{
	        	this.json = new JSONObject();
	        	parent.put(credential.toString(), this.json, credential.isPersistent());
	        }
    	}
    	else this.json = new JSONObject();
    	
        this.parent = parent;
        this.credential = credential;
    }

    /**
     * Associate a ClientIdentity with this Authentication
     * @param id the ClientIdentity to associate with
     * @return this authentication object
     */
    public Authentication setIdentity(@Nonnull ClientIdentity id) {
        this.json.put("id", id.toString());
        if (this.parent != null && this.credential.isPersistent()) this.parent.commit();
        return this;
    }

    /**
     * Get the associated ClientIdentity
     * @return the ClientIdentity associated with this Authentication or null if none is set
     */
    public ClientIdentity getIdentity() {
        if (this.json.has("id")) return new ClientIdentity(this.json.getString("id"));
        return null;
    }

    /**
     * Set an expire time. Useful for anonymous users and tokens
     * @param time seconds from now when the Authentication expires
     */
    public void setExpireTime(long time){
    	this.json.put("expires_on", Instant.now().getEpochSecond() + time);
    	if (this.parent != null && this.credential.isPersistent()) this.parent.commit();
    }

    /**
     * Check if the authentication is still valid
     * @return true if the Authentication is still valid or does not have an expire time set. false otherwise
     */
    public boolean checkExpireTime() {
        return !this.json.has("expires_on") || this.json.getLong("expires_on") > Instant.now().getEpochSecond();
    }

    /**
     * Get a value from the internal JSONObject
     * @param key the key for the object
     * @return the value
     */
    public Object get(String key){
    	return this.json.get(key);
    }

    /**
     * Get a String form the internal JSONObject
     * @param key the key for the object
     * @return the String
     */
    public String getString(String key){
    	return this.json.getString(key);
    }

    /**
     * Get a String form the internal JSONObject.
     * @param key the key for the object
     * @param defVal a default value in case the key does not exist or is not a String
     * @return the value or, on error, the default value
     */
    public String getString(String key, String defVal){
        try {
            return getString(key);
        } catch (Throwable e){
            return defVal;
        }
    }

    /**
     * Get a boolean form the internal JSONObject
     * @param key the key for the object
     * @return the boolean
     */
    public boolean getBoolean(String key){
    	return this.json.getBoolean(key);
    }

    /**
     * Get a boolean form the internal JSONObject
     * @param key the key for the object
     * @param defVal a default value in case the key does not exist or is not a boolean
     * @return the boolean or, on error, the default value
     */
    public boolean getBoolean(String key, boolean defVal){
        try {
            return getBoolean(key);
        } catch (Throwable e){
            return defVal;
        }
    }

    /**
     * Check the internal JSONObject for a key
     * @param key the key to be looked for
     * @return true if the key exists, false otherwise
     */
    public boolean has(String key){
    	return this.json.has(key);
    }

    /**
     * Put data into the internal JSONObject
     * @param key the key
     * @param value the data
     */
    public void put(String key, Object value){
    	this.json.put(key, value);
    	if (this.parent != null && this.credential.isPersistent()) this.parent.commit();
    }

    /**
     * Remove an object from the internal JSONObject
     * @param key the key of the object
     */
    public void remove(String key){
    	this.json.remove(key);
    	if (this.parent != null && this.credential.isPersistent()) this.parent.commit();
    }

    /**
     * Delete the authentication. That is important if the Authentication turned out to invalid.
     * For example during login, if no ClientIdentity was associated with the given Credentials
     */
    public void delete(){
    	this.parent.remove(this.credential.toString());
    	parent = null;
    }
}
