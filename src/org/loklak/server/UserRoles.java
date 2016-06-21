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

import org.json.JSONObject;
import org.loklak.tools.storage.JsonFile;

import java.util.ArrayList;
import java.util.HashMap;

public class UserRoles {

    private JSONObject json;
    private JsonFile parent;
    private HashMap<BaseUserRole, UserRole> defaultRoles;
    private HashMap<String, UserRole> roles;

    public UserRoles(JsonFile parent) throws Exception{
        if(parent != null){
            json = parent;
        }
        else json = new JSONObject();
        this.parent = parent;
    }

    /**
     * Create default user roles, use names of the BaseUserRoles
     */
    public void loadDefaultUserRoles(){
        defaultRoles = new HashMap<>();
        roles = new HashMap<>();

        for(BaseUserRole bur : BaseUserRole.values()){
            JSONObject obj = new JSONObject();
            json.put(bur.name(), obj);

            UserRole userRole = new UserRole(bur.name(), bur, null, obj);
            setDefaultUserRole(bur, userRole);
        }

        if(parent != null) parent.commit(); // this seems like a bug to me, it shouldn't be necessary

    }

    public void loadUserRolesFromObject() throws IllegalArgumentException{
        defaultRoles = new HashMap<>();
        roles = new HashMap<>();

        try {
            ArrayList<String> queue = new ArrayList<>();

            // get all user roles based on BaseUserRole. Add all other into a queue.
            for (String key : json.keySet()) {
                JSONObject obj = json.getJSONObject(key);
                if (hasMandatoryFields(obj)) {
                    BaseUserRole bur;
                    try {
                        bur = BaseUserRole.valueOf(obj.getString("parent"));
                    } catch (IllegalArgumentException e) {
                        queue.add(key);
                        continue;
                    }
                    UserRole userRole = new UserRole(key, bur, null, obj);
                    roles.put(key, userRole);
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

            JSONObject defaults = json.getJSONObject("defaults");
            for (BaseUserRole bur : BaseUserRole.values()) {
                defaultRoles.put(bur, roles.get(defaults.getString(bur.name())));
            }
        } catch(Exception e){
            defaultRoles = null;
            roles = null;
            throw new IllegalArgumentException("Could not load user roles from file: ", e);
        }
    }

    private boolean hasMandatoryFields(JSONObject object){
        return object.has("parent") && object.has("name") && object.has("permissions");
    }

    public UserRole getDefaultUserRole(BaseUserRole bur){
        return defaultRoles.get(bur);
    }

    public void setDefaultUserRole(BaseUserRole bur, UserRole ur){
        if(!json.has("defaults")) json.put("defaults", new JSONObject());
        defaultRoles.put(bur, ur);
        json.getJSONObject("defaults").put(bur.name(), ur.getUserRoleName());
    }

}
