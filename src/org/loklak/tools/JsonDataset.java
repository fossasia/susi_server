/**
 *  JsonIndex
 *  Copyright 16.07.2015 by Michael Peter Christen, @0rb1t3r
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.loklak.tools.JsonDump.ConcurrentReader;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonDataset {
    
    private final JsonDump indexDump;
    private final Map<String, Index> index;
    private final JsonMinifier minifier;
    
    /**
     * define a data set
     * @param dump_dir the path where the subdirectories for this data set shall be stored
     * @param dump_file_prefix a prefix for the file names
     * @param index_keys the names of the json property keys where their content shall be indexed by this field
     * @throws IOException
     */
    public JsonDataset(File dump_dir, String dump_file_prefix, String[] index_keys) throws IOException {
        this.indexDump = new JsonDump(dump_dir, dump_file_prefix, null);
        this.index = new ConcurrentHashMap<>();
        this.minifier = new JsonMinifier();
        for (String idx: index_keys) this.index.put(idx, new Index());
        int concurrency = Runtime.getRuntime().availableProcessors();
        final ConcurrentReader reader = indexDump.getOwnDumpReader(concurrency);
        if (reader != null) {
            reader.start();
            Thread[] indexerThreads = new Thread[concurrency];
            for (int i = 0; i < concurrency; i++) {
                indexerThreads[i] = new Thread() {
                    public void run() {
                        Map<String, Object> obj;
                        try {
                            while ((obj = reader.take()) != JsonDump.POISON_JSON_MAP) {
                                // write index to object
                                Object op = obj.remove(new String(JsonDump.OPERATION_KEY));
                                try {
                                    JsonMinifier.Capsule json = JsonDataset.this.minifier.minify(obj);
                                    for (Map.Entry<String, Index> idxo: JsonDataset.this.index.entrySet()) {
                                        Object x = obj.get(idxo.getKey());
                                        if (x != null) idxo.getValue().put(x, json);
                                    }
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                indexerThreads[i].start();
            }
            for (int i = 0; i < concurrency; i++) {
                try {indexerThreads[i].join();} catch (InterruptedException e) {}
            }
        }
    }
    
    /**
     * put an object into the index, but do not overwrite existing pairs
     * @param key
     * @param value
     * @throws IOException
     */
    public void putUnique(Map<String, Object> obj) throws IOException {
        JsonMinifier.Capsule json = this.minifier.minify(obj);
        idxstore: for (Map.Entry<String, Index> idxo: this.index.entrySet()) {
            String idx_field = idxo.getKey();
            Object value = obj.get(idx_field);
            if (value != null) {
                Index index = idxo.getValue();
                if (index.containsKey(value)) continue idxstore; // we don't overwrite existing indexes
                index.put(value, json);
            }
        }
        indexDump.write(obj, 'I');
    }
    
    public Index getIndex(String key) {
        return this.index.get(key);
    }
    
    public void close() {
        this.indexDump.close();
    }
    
    public static class Index extends ConcurrentHashMap<Object, JsonMinifier.Capsule> implements Map<Object, JsonMinifier.Capsule> {
        private static final long serialVersionUID = 4596787150066539880L;        
    }
    
    public static void main(String[] args) {
        File testidx = new File("testidx");
        try {
            JsonDataset dtst = new JsonDataset(testidx, "idx_", new String[]{"abc", "def"});
            
            Map<String,  Object> map = new HashMap<>();
            map.put("abc", 1);
            map.put("def", "Hello World");
            map.put("ghj", new String[]{"Hello", "World"});
            
            dtst.putUnique(map);
            
            dtst.close();

            dtst = new JsonDataset(testidx, "idx_", new String[]{"abc", "def"});
            Index idx = dtst.getIndex("abc");
            System.out.println(idx.get(1));
            idx = dtst.getIndex("def");
            System.out.println(idx.get("Hello World"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
