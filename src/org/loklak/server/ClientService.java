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

public class ClientService extends Client {

    public enum Type {
        apiAccess, // service grants access to a specific api
    }
    
    public ClientService(String rawIdString) {
        super(rawIdString);
    }
    
    public ClientService(Type type, String untypedId) {
        super(type.name(), untypedId);
    }
    
    public Type getType() {
        return Type.valueOf(this.getKey());
    }
    
}
