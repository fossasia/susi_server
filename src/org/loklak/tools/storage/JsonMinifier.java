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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.tools.Compression;
import org.loklak.tools.UTF8;


public class JsonMinifier {

    private final ConcurrentHashMap<String, String> key2short, short2key;
    
    public JsonMinifier() {
        this.key2short = new ConcurrentHashMap<>();
        this.short2key = new ConcurrentHashMap<>();
    }
    
    public JsonCapsuleFactory minify(JSONObject json) {
        if (json == null) return null;
        JSONObject minified = new JSONObject(true);
        for (String key: json.keySet()) {
            String s = this.key2short.get(key);
            if (s == null) synchronized(this.key2short) {
                s = this.key2short.get(key);
                if (s == null) {
                    s = Integer.toHexString(this.key2short.size());
                    this.key2short.put(key, s);
                    this.short2key.put(s, key);
                }
            }
            minified.put(s, json.get(key));
        }
        return new JsonCapsuleFactory(minified);
    }
    
    public class JsonCapsuleFactory implements JsonFactory {
        
        byte[] capsule; // byte 0 is a flag: 0 = raw json, 1 = compressed json

        private JsonCapsuleFactory(JSONObject jo) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            GZIPOutputStream out = null; try {out = new GZIPOutputStream(baos, 1024){{def.setLevel(Deflater.BEST_COMPRESSION);}};} catch (IOException e) {}
            OutputStreamWriter osw = new OutputStreamWriter(out);
            jo.write(osw);
            try {osw.close();} catch (IOException e) {}
            //byte[] b = new ObjectMapper().writer().writeValueAsBytes(json);
            //byte[] c = Compression.gzip(b);
            byte[] c = baos.toByteArray();
            //if (b.length <= c.length) {
            //    this.capsule = new byte[b.length + 1];
            //    this.capsule[0] = 0;
            //    System.arraycopy(b, 0, this.capsule, 1, b.length);
            //} else {
                this.capsule = new byte[c.length + 1];
                this.capsule[0] = 1;
                System.arraycopy(c, 0, this.capsule, 1, c.length);
            //}
            //System.out.print("DEBUG " + this.getRawJson());
        }
        
        public JSONObject getJSON() {
            JSONObject minified = getRawJSON();
            JSONObject original = new JSONObject(true);
            for (String key: minified.keySet()) {
                String s = JsonMinifier.this.short2key.get(key);
                assert s != null;
                if (s != null) original.put(s, minified.get(key));
            }
            return original;
        }
        
        private JSONObject getRawJSON() {
            byte[] x = new byte[this.capsule.length - 1];
            System.arraycopy(this.capsule, 1, x, 0, this.capsule.length - 1);
            if (this.capsule[0] == 1) {
                x = Compression.gunzip(x);
            }
            try {
                JSONObject json = new JSONObject(UTF8.String(x));
                return json;
            } catch (Throwable e) {
                DAO.log("cannot parse capsule \"" + UTF8.String(this.capsule) + "\"");
            } 
            return null;
        }

    }
    
}
