/**
 *  Identity
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

package ai.susi.server;

import org.json.JSONObject;

import ai.susi.tools.Digest;

/**
 * an identity is only a string which contains details sufficient enough to
 * identify a user and to send data to that user
 */
public class ClientIdentity extends Client {
    
    public final static ClientIdentity ANONYMOUS = new ClientIdentity("host" + Client.SEPARATOR + "localhost");
    
    public enum Type {
        uuid(true),  // non-anonymous identity, computed from the (first) email address as md5
        email(true), // non-anonymous identity
        host(false), // anonymous identity users which do not authentify; they are identified by their host name
        nick(true); // non-anonymous identity using the nickname in a chat
        private final boolean persistent;
        Type(final boolean persistent) {
            this.persistent = persistent;
        }
        public boolean isPersistent() {
            return this.persistent;
        }
    }
    
    private final boolean persistent;
    
    public ClientIdentity(String rawIdString) throws IllegalArgumentException {
        super(rawIdString);
        String key = super.getKey();
        Type type = Type.valueOf(key); // throws IllegalArgumentException
        this.persistent = type.isPersistent();
    }
    
    public ClientIdentity(Type type, String untypedId) {
        super(type.name(), untypedId);
        this.persistent = type.isPersistent();
    }
    
    public boolean isPersistent() {
        return this.persistent;
    }
    
    public boolean isUuid() {
        return this.getKey().equals(Type.uuid.name());
    }
    
    public boolean isEmail() {
        return this.getKey().equals(Type.email.name());
    }
    
    public boolean isAnonymous() {
        return this.getKey().equals(Type.host.name());
    }
    
    public String getClient() {
        String client = this.getType() + "_" + this.getName();
        return client;
    }
    
    public Type getType() {
        return Type.valueOf(this.getKey());
    }
    
    /**
     * compute a unique user id
     * @return the unique user id.
     */
    public String getUuid() {
    	if (this.isUuid()) {
    		return super.getName();
    	} else {
    		return Digest.encodeMD5Hex(super.getName());
    	}
    }
    
    public String[] getLookupKeys() {
    	// if this is an email identity then we have two lookup keys:
    	// - the email identity name itself
    	// - the uuid computed from the email identity
    	if (this.isEmail()) {
    		return new String[]{super.toString(), getUuid()};
    	} else {
    		return new String[]{super.toString()};
    	}
    }
    
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("anonymous", this.isAnonymous());
        return json;
    }
}
