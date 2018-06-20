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

package ai.susi.server;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonTray;

import javax.annotation.Nonnull;

/**
 * Authorization asks: what is the user allowed to do? This class holds user rights.
 * An object instance of this class is handed to each serivce call to enable that
 * service to work according to granted service level.
 * One part of authorization decisions is the history of the past user action.
 * Therefore an authorization object has the attachment of an Accounting object;  
 */
public class Authorization {

    private JsonTray parent;
    private JSONObject json;
    private JSONObject permissions;
    private ClientIdentity identity;
    private UserRole userRole;
    
    /**
     * create a new authorization object. The given json object must be taken
     * as value from a parent json. If the parent json is a JsonFile, then that
     * file can be handed over as well to enable persistency.
     * @param identity
     * @param parent the parent file or null if there is no parent file (no persistency)
     */
    public Authorization(@Nonnull ClientIdentity identity, JsonTray parent) {

        this.parent = parent;
        this.identity = identity;

        if (parent != null) {
        	String[] lookupKeys = identity.getLookupKeys();
        	this.json = null;
        	for (String key: lookupKeys) {
        		if (parent.has(key)) {
        			this.json = parent.getJSONObject(key);
    	    		break;
        		}
        	}
        	// in case that the identity has the uuid inside, we must loop here over all objects
        	if (this.json == null && identity.isUuid()) for (String key: this.parent.keys()) {
        		if (new ClientIdentity(key).getUuid().equals(identity.getName())) {
        			this.json = parent.getJSONObject(key);
    	    		break;
        		}
        	}
	    	if (this.json == null) {
	    		this.json = new JSONObject();
	        	parent.put(identity.toString(), json, identity.isPersistent());
	        }
    	} else {
    		this.json = new JSONObject();
    	}
    	
    	if (this.json.has("userRole")) {
    		String userRoleName = this.json.getString("userRole");
            try {
            	userRole = UserRole.valueOf(userRoleName.toUpperCase());
            } catch (IllegalArgumentException e) {
            	DAO.severe("user role " + userRoleName + " invalid");
                userRole = UserRole.ANONYMOUS;
            }
    	} else{
            DAO.severe("user role invalid (not given)");
            userRole = UserRole.ANONYMOUS;
            this.json.put("userRole", userRole.getName());
        }

        if(!this.json.has("permissions")) this.json.put("permissions", new JSONObject());
        permissions = this.json.getJSONObject("permissions");
    }
    
    public Authorization setAdmin() {
        this.json.put("admin", true);
        if (parent != null && getIdentity().isPersistent()) parent.commit();
        return this;
    }
    
    public boolean isAdmin() {
        if (!this.json.has("admin")) return false;
        return this.json.getBoolean("admin");
    }
    
    public Authorization setRequestFrequency(String path, int reqPerHour) {
        if (!this.json.has("frequency")) {
            this.json.put("frequency", new JSONObject());
        }
        JSONObject paths = this.json.getJSONObject("frequency");
        paths.put(path, reqPerHour);
        if (parent != null && getIdentity().isPersistent()) parent.commit();
        return this;
    }
    
    public int getRequestFrequency(String path) {
        if (!this.json.has("frequency")) return -1;
        JSONObject paths = this.json.getJSONObject("frequency");
        if (!paths.has(path)) return -1;
        return paths.getInt(path);
    }
    
    public Authorization addService(ClientService service) {
        if (!this.json.has("services")) this.json.put("services", new JSONObject());
        JSONObject services = this.json.getJSONObject("services");
        services.put(service.toString(), service.toJSON().getJSONObject("meta"));
        if (parent != null && getIdentity().isPersistent()) parent.commit();
        return this;
    }
    
    public ClientService getService(String serviceId) throws IllegalArgumentException {
        if (!this.json.has("services")) this.json.put("services", new JSONObject());
        JSONObject services = this.json.getJSONObject("services");
        if (!services.has(serviceId)) return null;
        JSONObject s = services.getJSONObject(serviceId);
        ClientService service = new ClientService(serviceId);
        service.setMetadata(s.getJSONObject("meta"));
        return service;
    }
    
    public ClientIdentity getIdentity() {
        return identity;
    }
    
    public UserRole getUserRole(){
    	return this.userRole;
    }

    public Authorization setUserRole(UserRole ur) {
        this.userRole = ur;
        json.put("userRole", userRole.getName());
        if (parent != null && getIdentity().isPersistent()) parent.commit();
        return this;
    }

    public JSONObject getPermission() {
        return permissions;
    }

    public void setPermission(APIHandler servlet, String key, long value) {
        String servletCanonicalName = servlet.getClass().getCanonicalName();
		if (!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
		permissions.getJSONObject(servletCanonicalName).put(key, value);
    }
    
    public JSONObject getJSON() {
    	return this.json;
    }
}
