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

public class UserRoles {

    JsonFile parent;
    UserRole defaultAnonymous;
    UserRole defaultUser;
    UserRole defaultAdmin;
    JSONObject json;

    public UserRoles(JsonFile parent){
        this.parent = parent;
        if(parent != null){
            this.json = parent;
        }
        else this.json = new JSONObject();

        for(BaseUserRole bur : BaseUserRole.values()){

        }
    }
}
