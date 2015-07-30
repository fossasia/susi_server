/**
 *  JsonMinifier
 *  Copyright 30.07.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.tools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMinifier {

    private final ConcurrentHashMap<String, String> keytransform;
    
    public JsonMinifier() {
        this.keytransform = new ConcurrentHashMap<>();
    }
    
    public Capsule minify(Map<String, Object> json) throws JsonProcessingException {
        return new Capsule(json);
    }
    
    public class Capsule {
        
        byte[] capsule; // byte 0 is a flag: 0 = raw json, 1 = compressed json

        private Capsule(Map<String, Object> json) throws JsonProcessingException {
            byte[] b =  new ObjectMapper().writer().writeValueAsBytes(json);
            byte[] c = Compression.gzip(b);
            if (b.length <= c.length) {
                this.capsule = new byte[b.length + 1];
                this.capsule[0] = 0;
                System.arraycopy(b, 0, this.capsule, 1, b.length);
            } else {
                this.capsule = new byte[c.length + 1];
                this.capsule[0] = 1;
                System.arraycopy(c, 0, this.capsule, 1, c.length);
            }
        }
        
        public Map<String, Object> getJson() {
            return getRawJson();
        }
        
        private Map<String, Object> getRawJson() {
            byte[] x = new byte[this.capsule.length - 1];
            System.arraycopy(this.capsule, 1, x, 0, this.capsule.length - 1);
            if (this.capsule[0] == 1) {
                x = Compression.gunzip(x);
            }
            try {
                Map<String, Object> json = DAO.jsonMapper.readValue(x, DAO.jsonTypeRef);
                return json;
            } catch (Throwable e) {
                Log.getLog().warn("cannot parse capsule \"" + UTF8.String(this.capsule) + "\"", e);
            } 
            return null;
        }

    }
    
}
