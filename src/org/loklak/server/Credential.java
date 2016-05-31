/**
 *  Credential
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

/**
 * A credential is used as key in DAO.authentication
 */
public class Credential {

    public final static char SEPARATOR = ':';
    
    public enum Type {
        passwdLogin,
        login_token,
        host;
        private String prefix;
        private Type() {
            this.prefix = name() + SEPARATOR;
        }
        public String prefix() {
            return this.prefix;
        }
        public int prefixLength() {
            return this.prefix.length();
        }
    }
    
    private String id;
    private int separatorPos;

    public Credential(String rawIdString) {
        this.separatorPos = rawIdString.indexOf(SEPARATOR);
        assert this.separatorPos >= 0;
        this.id = rawIdString;
    }
    
    public Credential(Type type, String untypedId) {
        this.id = type.name() + SEPARATOR + untypedId;
        this.separatorPos = this.id.indexOf(SEPARATOR);
    }
    
    public boolean isPasswdLogin() {
        return this.id.startsWith(Type.passwdLogin.prefix());
    }
    
    public boolean isToken() {
        return this.id.startsWith(Type.login_token.prefix());
    }
    
    public boolean isAnonymous() {
        return this.id.startsWith(Type.host.prefix());
    }
    
    public Type getType() {
        return Type.valueOf(id.substring(0, this.separatorPos));
    }
    
    public String getPayload() {
        return this.id.substring(this.separatorPos + 1);
    }
    
    public String toString() {
        return this.id;
    }
}
