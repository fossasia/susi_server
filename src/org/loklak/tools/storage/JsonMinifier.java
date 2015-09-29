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

package org.loklak.tools.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.loklak.data.DAO;
import org.loklak.tools.Compression;
import org.loklak.tools.UTF8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMinifier {

    private final ConcurrentHashMap<String, String> key2short, short2key;
    
    public JsonMinifier() {
        this.key2short = new ConcurrentHashMap<>();
        this.short2key = new ConcurrentHashMap<>();
    }
    
    public Capsule minify(Map<String, Object> json) throws JsonProcessingException {
        if (json == null) return null;
        LinkedHashMap<String, Object> minified = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry: json.entrySet()) {
            String s = this.key2short.get(entry.getKey());
            if (s == null) synchronized(this.key2short) {
                s = this.key2short.get(entry.getKey());
                if (s == null) {
                    s = Integer.toHexString(this.key2short.size());
                    this.key2short.put(entry.getKey(), s);
                    this.short2key.put(s, entry.getKey());
                }
            }
            minified.put(s, entry.getValue());
        }
        return new Capsule(minified);
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
            Map<String, Object> minified = getRawJson();
            LinkedHashMap<String, Object> original = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry: minified.entrySet()) {
                String s = JsonMinifier.this.short2key.get(entry.getKey());
                assert s != null;
                if (s != null) original.put(s, entry.getValue());
            }
            return original;
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
                DAO.log("cannot parse capsule \"" + UTF8.String(this.capsule) + "\"");
            } 
            return null;
        }

    }
    
}
