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

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

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
    private UserRoles userRoles;
    
    /**
     * create a new authorization object. The given json object must be taken
     * as value from a parent json. If the parent json is a JsonFile, then that
     * file can be handed over as well to enable persistency.
     * @param identity
     * @param parent the parent file or null if there is no parent file (no persistency)
     */
    public Authorization(@Nonnull ClientIdentity identity, JsonTray parent, @Nonnull UserRoles urs) {

        Log.getLog().debug("new authorization");

        this.parent = parent;
        this.identity = identity;
        this.userRoles = urs;

        if(parent != null){
	    	if (parent.has(identity.toString())) {
	    		json = parent.getJSONObject(identity.toString());
	        } else {
	        	json = new JSONObject();
	        	parent.put(identity.toString(), json, identity.isPersistent());
	        }
    	}
    	else json = new JSONObject();
    	
    	if(json.has("userRole") && userRoles.has(json.getString("userRole"))){
            Log.getLog().debug("user role " + json.getString("userRole") + " valid");
    		userRole = userRoles.getUserRoleFromString(json.getString("userRole"));
            Log.getLog().debug("user role: " + userRole.getName());
    	}
    	else{
            Log.getLog().debug("user role invalid");
            userRole = userRoles.getDefaultUserRole(BaseUserRole.ANONYMOUS);
            json.put("userRole", userRole.getName());
            Log.getLog().debug("user role: " + userRole.getName());
        }

        if(!json.has("permissions")) json.put("permissions", new JSONObject());
        permissions = json.getJSONObject("permissions");
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
    
    public BaseUserRole getBaseUserRole(){
    	return userRole.getBaseUserRole();
    }

    public UserRole getUserRole(){
        return userRole;
    }

    public Authorization setUserRole(UserRole ur){
        userRole = ur;
        json.put("userRole", userRole.getName());
        if (parent != null && getIdentity().isPersistent()) parent.commit();
        return this;
    }

    public JsonObjectWithDefault getPermissions(APIHandler servlet){

        // get upstream permissions
        JsonObjectWithDefault permissions =  userRole.getPermissions(servlet);

        // override of permissions
        if(this.permissions.has(servlet.getClass().getCanonicalName())){
            permissions.putAll(this.permissions.getJSONObject(servlet.getClass().getCanonicalName()));
        }

        return permissions;
    }

    public JSONObject getPermissionOverrides(){
        return permissions;
    }

    public void setPermission(String servletCanonicalName, String key, JSONObject value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, JSONObject value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, String value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, String value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, int value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, int value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, long value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, long value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, double value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, double value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, Object value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, Object value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, boolean value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, boolean value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, Map<?, ?> value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, Map<?, ?> value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }

    public void setPermission(String servletCanonicalName, String key, Collection<?> value){
        if(!permissions.has(servletCanonicalName)) permissions.put(servletCanonicalName, new JSONObject());
        permissions.getJSONObject(servletCanonicalName).put(key, value);
    }

    public void setPermission(APIHandler servlet, String key, Collection<?> value){
        setPermission(servlet.getClass().getCanonicalName(), key, value);
    }
    
    public JSONObject getJSON() {
    	return this.json;
    }
}
