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
import org.loklak.tools.storage.JsonTray;

import javax.annotation.Nonnull;

public class UserRole {

    private String name;
    private JsonTray storage;
    private JSONObject json;
    private UserRole parent;
    private BaseUserRole baseUserRole;

    public UserRole(@Nonnull String userRoleName, JsonTray storage, UserRole parent, @Nonnull BaseUserRole baseUserRole){
        if(storage != null){
            if (storage.has(userRoleName)) {
                this.json = storage.getJSONObject(userRoleName);
            } else {
                this.json = new JSONObject();
                storage.put(userRoleName, this.json, true);
            }
        }
        else this.json = new JSONObject();

        this.name = userRoleName;
        this.parent = parent;
        this.storage = storage;
        this.baseUserRole = baseUserRole;
    }

    public BaseUserRole getBaseUserRole(){
        return this.baseUserRole;
    }

    public String getName(){
        return this.name;
    }
}
