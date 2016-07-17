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
public class ClientCredential extends Client {
    
    public enum Type {
    	passwd_login(true),
        cookie(false),
        access_token(false),
        resetpass_token(false),
        pubkey_challange(false),
        host(false);
        private final boolean persistent;
        Type(final boolean persistent) {
            this.persistent = persistent;
        }
        public boolean isPersistent() {
            return this.persistent;
        }
    }
    
    private final boolean persistent;

    public ClientCredential(String rawIdString) {
        super(rawIdString);
        this.persistent = Type.valueOf(super.getKey()).isPersistent();
    }
    
    public ClientCredential(Type type, String untypedId) {
        super(type.name(), untypedId);
        this.persistent = type.isPersistent();
    }

    public boolean isPersistent() {
        return this.persistent;
    }
    
    public boolean isPasswdLogin() {
        return this.getKey().equals(Type.passwd_login.name());
    }
    
    public boolean isCookie() {
        return this.getKey().equals(Type.cookie.name());
    }
    
    public boolean isToken() {
        return this.getKey().equals(Type.access_token.name());
    }

    public boolean isResetToken() {
        return this.getKey().equals(Type.resetpass_token.name());
    }

    public boolean isPubkeyChallange() {
        return this.getKey().equals(Type.pubkey_challange.name());
    }
    
    public boolean isAnonymous() {
        return this.getKey().equals(Type.host.name());
    }
    
    public Type getType() {
        return Type.valueOf(this.getKey());
    }
    
}
