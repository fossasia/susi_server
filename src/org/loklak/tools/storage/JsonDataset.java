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

package org.loklak.tools.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.loklak.data.DAO;
import org.loklak.tools.ASCII;
import org.loklak.tools.BufferedRandomAccessFile;

public class JsonDataset {
    
    private final JsonRepository indexDump; // a directory containing dump, import and imported subdirectories
    private final Map<String, JsonFactoryIndex> index; // a mapping from a search key to the search index
    private final JsonMinifier minifier; // a minifier for json which learns about json mapping key names
    private final Map<String, Boolean> columns; // a mapping from the column key to a boolean which is true if the column value is case-insensitive
    
    public static class Column {
        public String key;
        public boolean caseInsensitive;
        public Column (String key, boolean caseInsensitive) {
            this.key = key;
            this.caseInsensitive = caseInsensitive;
        }
    }
    
    /**
     * define a data set: an indexed JsonDump where the index is held in RAM
     * @param dump_dir the path where the subdirectories for this data set shall be stored
     * @param dump_file_prefix a prefix for the file names
     * @param index_keys the names of the json property keys where their content shall be indexed by this field
     * @param mode the indexing mode, either completely in RAM with Mode.COMPRESSED or with file handles with Mode.REWRITABLE
     * @throws IOException
     */
    public JsonDataset(File dump_dir, String dump_file_prefix, Column[] columns, JsonRepository.Mode mode) throws IOException {
        
        // initialize class objects
        int concurrency = Runtime.getRuntime().availableProcessors();
        this.indexDump = new JsonRepository(dump_dir, dump_file_prefix, null, mode, concurrency);
        this.index = new ConcurrentHashMap<>();
        this.minifier = new JsonMinifier();
        this.columns = new HashMap<>();
        for (Column column: columns) this.columns.put(column.key, column.caseInsensitive);
        
        // assign for each index key one JsonFactory index
        for (Column col: columns) this.index.put(col.key, new JsonFactoryIndex());

        // start reading of the JsonDump
        final Collection<JsonReader> readers = indexDump.getOwnDumpReaders(true);

        // for each reader one threqd is started which does Json parsing and indexing
        if (readers != null) for (final JsonReader reader: readers) {
            DAO.log("loading " + reader.getName());
            (new Thread(reader)).start();
            Thread[] indexerThreads = new Thread[concurrency];
            for (int i = 0; i < concurrency; i++) {
                indexerThreads[i] = new Thread() {
                    public void run() {
                        JsonFactory jsonHandle;
                        try {
                            while ((jsonHandle = reader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                                Map<String, Object> op = jsonHandle.getJson();
                                JsonFactory json;
                                if (jsonHandle instanceof JsonRandomAccessFile.JsonHandle) {
                                    JsonRandomAccessFile.JsonHandle handle = (JsonRandomAccessFile.JsonHandle) jsonHandle;
                                    assert reader instanceof JsonRandomAccessFile;
                                    // create the file json handle which does not contain the json any more
                                    // but only the file handle
                                    json = ((JsonRandomAccessFile) reader).getJsonFactory(handle.getIndex(), handle.getLength());
                                } else {
                                    assert JsonDataset.this.indexDump.getMode() == JsonRepository.COMPRESSED_MODE;
                                    // create the json minifier object which contains the json in minified version
                                    // before we create the minifier, we remove the meta keys from the json to further minify it
                                    for (byte[] meta_key: JsonRepository.META_KEYS) {
                                        op.remove(ASCII.String(meta_key));
                                    }
                                    json = JsonDataset.this.minifier.minify(op);
                                }
                                // the resulting json factory is written to each search index
                                for (Map.Entry<String, Boolean> column: JsonDataset.this.columns.entrySet()) {
                                    String searchKey = column.getKey();
                                    boolean case_insensitive = column.getValue();
                                    JsonFactoryIndex factoryIndex = JsonDataset.this.index.get(searchKey);
                                    Object searchValue = op.get(searchKey);
                                    if (searchValue != null && searchValue instanceof String) {
                                        JsonFactory old = factoryIndex.put(case_insensitive ? ((String) searchValue).toLowerCase() : (String) searchValue, json);
                                        /*
                                        if (old != null) {
                                            if (json instanceof ReaderJsonFactory) {
                                                ReaderJsonFactory rjf = (ReaderJsonFactory) json;
                                                System.out.println("Double Key: new is in pos " + rjf.getIndex() + " in file " +  rjf.getFile());
                                            } else {
                                                System.out.println("Double Key: new is " + json);
                                            }
                                            if (old instanceof ReaderJsonFactory) {
                                                ReaderJsonFactory rjf = (ReaderJsonFactory) old;
                                                System.out.println("Double Key: old is in pos " + rjf.getIndex() + " in file " +  rjf.getFile());
                                            } else {
                                                System.out.println("Double Key: old is " + json);
                                            }
                                        }
                                        */
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                indexerThreads[i].start();
            }
            // wait for the completion of each task
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
    public JsonFactory putUnique(Map<String, Object> obj) throws IOException {
        JsonFactory json = indexDump.write(obj, 'I');
        idxstore: for (Map.Entry<String, Boolean> column: this.columns.entrySet()) {
            //for (Map.Entry<String, JsonFactoryIndex> idxo: this.index.entrySet()) {
            String searchKey = column.getKey();
            boolean case_insensitive = column.getValue();
            Object value = obj.get(searchKey);
            if (value != null && value instanceof String) {
                JsonFactoryIndex index = this.index.get(searchKey);
                String valueString = case_insensitive ? ((String) value).toLowerCase() : (String) value;
                if (index.containsKey(valueString)) continue idxstore; // we don't overwrite existing indexes
                index.put(valueString, json);
            }
        }
        return json;
    }
    
    public JsonFactory get(String column, String value) {
        Boolean insensitive = this.columns.get(column);
        if (insensitive == null) throw new RuntimeException("Column " + column + " was not declared");
        JsonFactoryIndex jfi = this.index.get(column);
        if (jfi == null) throw new RuntimeException("Column " + column + " was not defined");
        return jfi.get(insensitive ? value.toLowerCase() : value);
    }
    
    public void close() {
        this.indexDump.close();
    }
    
    public static class JsonFactoryIndex extends ConcurrentHashMap<String, JsonFactory> implements Map<String, JsonFactory> {
        private static final long serialVersionUID = 4596787150066539880L;        
    }
    
    public int size() {
        int size = 0;
        for (JsonFactoryIndex fi: this.index.values()) {
            size = Math.max(size, fi.size());
        }
        return size;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test.class);
    }

    public static class Test extends TestCase {
        
        private File testFile;
        
        @Before
        public void setUp() throws Exception {
            this.testFile = BufferedRandomAccessFile.Test.getTestFile();
        }
    
        @After
        public void tearDown() throws Exception {
            this.testFile.delete();
        }
    
        public void test() throws IOException {
            for (JsonRepository.Mode mode: JsonRepository.Mode.values()) try {
                JsonDataset dtst = new JsonDataset(this.testFile, "idx_", new Column[]{new Column("abc", true), new Column("def", false)}, mode);
                
                Map<String,  Object> map = new HashMap<>();
                map.put("abc", 1);
                map.put("def", "Hello World");
                map.put("ghj", new String[]{"Hello", "World"});
                
                dtst.putUnique(map);
                
                dtst.close();

                dtst = new JsonDataset(this.testFile, "idx_", new Column[]{new Column("abc", true), new Column("def", false)}, mode);
                JsonFactoryIndex idx = dtst.index.get("abc");
                System.out.println(idx.get(1));
                idx = dtst.index.get("def");
                System.out.println(idx.get("Hello World"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
}
