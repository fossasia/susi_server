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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.loklak.tools.JsonDump.ConcurrentReader;

public class JsonIndex {
    
    JsonDump indexDump;
    Map<String, Map<String, Object>> index;
    
    public JsonIndex(File dump_dir, String dump_file_prefix) throws IOException {
        this.indexDump = new JsonDump(dump_dir, dump_file_prefix, null);
        this.index = new ConcurrentHashMap<>();
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
                                String key = (String) obj.get("k");
                                Map<String, Object> value = (Map<String, Object>) obj.get("v");
                                if (key != null && value != null) JsonIndex.this.index.put(key, value);
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
     * put a key/value pair into the index, but do not overwrite existing pairs
     * @param key
     * @param value
     * @throws IOException
     */
    public void putUnique(String key, Map<String, Object> value) throws IOException {
        if (this.index.containsKey(key)) return;
        this.index.put(key, value);
        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("k", key);
        dump.put("v", value);
        indexDump.write(dump);
    }
    
    public void close() {
        this.indexDump.close();
    }
    
    /**
     * get a key/value pair
     * @param key
     * @return
     */
    public Map<String, Object> get(String key) {
        return this.index.get(key);
    }
    
    public static void main(String[] args) {
        File testidx = new File("testidx");
        try {
            JsonIndex idx = new JsonIndex(testidx, "idx_");
            
            Map<String,  Object> map = new HashMap<>();
            map.put("abc", 1);
            map.put("def", "Hello World");
            map.put("ghj", new String[]{"Hello", "World"});
            
            idx.putUnique("1", map);
            
            idx.close();

            idx = new JsonIndex(testidx, "idx_");
            System.out.println(idx.get("1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
