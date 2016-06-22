/**
 *  UserRoles
 *  Copyright 20.06.2016 by Robert Mader, @treba123
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

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class UserRoles {

    private JSONObject json;
    private HashMap<String, UserRole> defaultRoles;
    private HashMap<String, UserRole> roles;

    public UserRoles(JSONObject obj) throws Exception{
        if(obj != null){
            json = obj;
        }
        else json = new JSONObject();
    }

    /**
     * Create default user roles, use names of the BaseUserRoles
     */
    public void loadDefaultUserRoles(){
        defaultRoles = new HashMap<>();
        roles = new HashMap<>();

        for(BaseUserRole bur : BaseUserRole.values()){
            createDefaultUserRole(bur);
        }
    }

    private void createDefaultUserRole(BaseUserRole bur){
        JSONObject obj = new JSONObject();

        UserRole userRole = new UserRole(bur.name().toLowerCase(), bur, null, obj);
        setDefaultUserRole(bur, userRole);
        roles.put(userRole.getName(), userRole);

        json.put(userRole.getName(), obj); // this order of putting this at the end currently prevents a race condition in JsonFile !?
    }

    public void loadUserRolesFromObject() throws IllegalArgumentException{
        defaultRoles = new HashMap<>();
        roles = new HashMap<>();

        try {
            ArrayList<String> queue = new ArrayList<>();

            // get all user roles based on BaseUserRole. Add all other into a queue.
            for (String name : json.keySet()) {
                Log.getLog().debug("searching for key " + name);
                JSONObject obj = json.getJSONObject(name);
                if (hasMandatoryFields(obj)) {
                    Log.getLog().debug(name + " has mandatory fields");
                    Log.getLog().debug("parent value is: " + obj.getString("parent"));
                    BaseUserRole bur;
                    try {
                        bur = BaseUserRole.valueOf(obj.getString("parent"));
                    } catch (IllegalArgumentException e) {
                        queue.add(name);
                        Log.getLog().debug("no bur, adding to queue");
                        continue;
                    }
                    Log.getLog().debug("successfully created bur from parent");
                    UserRole userRole = new UserRole(name, bur, null, obj);
                    roles.put(name, userRole);
                }
            }

            // recursively add
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String key : queue) {
                    JSONObject obj = json.getJSONObject(key);
                    if (roles.containsKey(obj.getString("parent"))) {
                        UserRole parent = roles.get(obj.getString("parent"));
                        UserRole userRole = new UserRole(key, parent.getBaseUserRole(), parent, obj);
                        roles.put(key, userRole);
                        queue.remove(key);
                        changed = true;
                    }
                }
            }

            Log.getLog().debug("available roles: " + roles.keySet().toString());

            // get default roles
            JSONObject defaults = json.getJSONObject("defaults");
            for (BaseUserRole bur : BaseUserRole.values()) {
                if(defaults.has(bur.name()) && roles.containsKey(defaults.getString(bur.name()))) {
                    Log.getLog().debug("found default role for " + bur.name() + ": " + roles.get(defaults.getString(bur.name())).getDisplayName());
                    setDefaultUserRole(bur, roles.get(defaults.getString(bur.name())));
                }
                else{
                    Log.getLog().info("could not find default role for " + bur.name() + ", creating default role" );
                    createDefaultUserRole(bur);
                }
            }
        } catch(Exception e){
            defaultRoles = null;
            roles = null;
            throw new IllegalArgumentException("Could not load user roles from file: ", e);
        }
    }

    private boolean hasMandatoryFields(JSONObject object){
        return object.has("parent") && object.has("display-name") && object.has("permissions");
    }

    public UserRole getDefaultUserRole(BaseUserRole bur){
        return defaultRoles.get(bur.name());
    }

    public void setDefaultUserRole(BaseUserRole bur, UserRole ur){
        if(!json.has("defaults")) json.put("defaults", new JSONObject());
        defaultRoles.put(bur.name(), ur);
        json.getJSONObject("defaults").put(bur.name(), ur.getName());
    }

    public boolean has(String ur){
        return roles.containsKey(ur);
    }

    public UserRole getUserRoleFromString(String ur){
        if(roles.containsKey(ur)){
            return roles.get(ur);
        }
        return null;
    }
}
