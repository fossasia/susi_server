/**
 *  UserRole
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
import javax.annotation.Nonnull;

public class UserRole {

    private String name;
    private JSONObject json;
    private UserRole parent;
    private BaseUserRole baseUserRole;

    /**
     *
     * @param name          the key in the sourceObject
     * @param baseUserRole
     * @param parent        if a parent user role exists
     * @param sourceObject  JSONObject in some storage file. Write changes here if exist
     */
    public UserRole(@Nonnull String name, @Nonnull BaseUserRole baseUserRole, UserRole parent, JSONObject sourceObject){
        this.baseUserRole = baseUserRole;
        this.name = name;

        if(sourceObject != null) json = sourceObject;
        else json = new JSONObject();

        if(!json.has("display-name")) json.put("display-name", name);
        if(!json.has("permissions")) json.put("permissions", new JSONObject());

        setParent(parent);
    }


    public BaseUserRole getBaseUserRole(){
        return this.baseUserRole;
    }

    public String getUserRoleName(){
        return this.name;
    }

    public String getDisplayName() { return json.getString("display-name"); }

    public UserRole getParent(){
        return parent;
    }

    public void setParent(UserRole parent){
        if(parent == null){
            json.put("parent", baseUserRole.name());
        }
        else{
            json.put("parent", parent.getUserRoleName());
        }
        this.parent = parent;
    }

    public void setDisplayName(String name){
        json.put("display-name", name);
    }
}
