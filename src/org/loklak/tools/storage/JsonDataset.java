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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.tools.ASCII;

public class JsonDataset {
    
    private final JsonRepository indexDump; // a directory containing dump, import and imported subdirectories
    final Map<String, JsonFactoryIndex> index; // a mapping from a search key to the search index
    private final JsonMinifier minifier; // a minifier for json which learns about json mapping key names
    private final Map<String, Boolean> columns; // a mapping from the column key to a boolean which is true if the column value is case-insensitive
    private final String dateFieldName; // a name of a date field which shows the update time of the record
    private final DateFormat dateFieldFormat; 
    
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
    public JsonDataset(
            File dump_dir, String dump_file_prefix,
            Column[] columns,
            String dateFieldName, String dateFieldFormat,
            JsonRepository.Mode mode, final boolean dailyDump,
            int count) throws IOException {
        
        // initialize class objects
        int concurrency = Runtime.getRuntime().availableProcessors();
        this.indexDump = new JsonRepository(dump_dir, dump_file_prefix, null, mode, dailyDump, concurrency);
        this.index = new ConcurrentHashMap<>();
        this.minifier = new JsonMinifier();
        this.columns = new HashMap<>();
        this.dateFieldName = dateFieldName == null ? "" : dateFieldName;
        this.dateFieldFormat = this.dateFieldName.length() == 0 ? null : new SimpleDateFormat(dateFieldFormat);
        for (Column column: columns) this.columns.put(column.key, column.caseInsensitive);
        
        // assign for each index key one JsonFactory index
        for (Column col: columns) this.index.put(col.key, new JsonFactoryIndex());

        // start reading of the JsonDump
        final Collection<File> dumps = indexDump.getOwnDumps(count);

        // for each reader one threqd is started which does Json parsing and indexing
        if (dumps != null) for (final File dump: dumps) {
            final JsonReader reader = indexDump.getDumpReader(dump);
            DAO.log("loading " + reader.getName());
            Thread[] indexerThreads = new Thread[concurrency];
            for (int i = 0; i < concurrency; i++) {
                indexerThreads[i] = new Thread() {
                    public void run() {
                        JsonFactory jsonHandle;
                        try {
                            while ((jsonHandle = reader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                                JSONObject op = jsonHandle.getJSON();
                                JsonFactory jsonFactory;
                                if (jsonHandle instanceof JsonRandomAccessFile.JsonHandle) {
                                    JsonRandomAccessFile.JsonHandle handle = (JsonRandomAccessFile.JsonHandle) jsonHandle;
                                    assert reader instanceof JsonRandomAccessFile;
                                    // create the file json handle which does not contain the json any more
                                    // but only the file handle
                                    jsonFactory = ((JsonRandomAccessFile) reader).getJsonFactory(handle.getIndex(), handle.getLength());
                                } else {
                                    assert JsonDataset.this.indexDump.getMode() == JsonRepository.COMPRESSED_MODE;
                                    // create the json minifier object which contains the json in minified version
                                    // before we create the minifier, we remove the meta keys from the json to further minify it
                                    for (byte[] meta_key: JsonRepository.META_KEYS) {
                                        op.remove(ASCII.String(meta_key));
                                    }
                                    jsonFactory = JsonDataset.this.minifier.minify(op);
                                }
                                // the resulting json factory is written to each search index
                                for (Map.Entry<String, Boolean> column: JsonDataset.this.columns.entrySet()) {
                                    String searchKey = column.getKey();
                                    boolean case_insensitive = column.getValue();
                                    JsonFactoryIndex factoryIndex = JsonDataset.this.index.get(searchKey);
                                    Object searchValue = op.has(searchKey) ? op.get(searchKey) : null;
                                    if (searchValue != null) {
                                        if (searchValue instanceof String) {
                                            JsonFactory old = factoryIndex.put(case_insensitive ? ((String) searchValue).toLowerCase() : (String) searchValue, jsonFactory);
                                        } else {
                                            JsonFactory old = factoryIndex.put(searchValue, jsonFactory);
                                        }
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                        	Log.getLog().warn(e);
                        } catch (IOException e) {
                        	Log.getLog().warn(e);
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
    public JsonFactory putUnique(JSONObject obj) throws IOException {
        JsonFactory json = indexDump.write(obj, 'I');
        for (Map.Entry<String, Boolean> column: this.columns.entrySet()) {
            //for (Map.Entry<String, JsonFactoryIndex> idxo: this.index.entrySet()) {
            String searchKey = column.getKey();
            boolean case_insensitive = column.getValue();
            Object value = obj.get(searchKey);
            if (value != null && value instanceof String) {
                JsonFactoryIndex index = this.index.get(searchKey);
                String valueString = case_insensitive ? ((String) value).toLowerCase() : (String) value;
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
    
    public Date parseDate(JSONObject json) throws ParseException {
        if (this.dateFieldName == null || this.dateFieldName.length() == 0 || this.dateFieldFormat == null) throw new ParseException("no date field defined", 0);
        Object d = json.get(this.dateFieldName);
        if (d == null) throw new ParseException("no date field in json, expected field '" + this.dateFieldName + "'", 0);
        if (d instanceof Date) return (Date) d;
        if (!(d instanceof String)) throw new ParseException("date field in json must contain a String or Date, not " + d.getClass().getName(), 0);
        return this.dateFieldFormat.parse((String) d);
    }
    
    public void close() {
        this.indexDump.close();
    }
    
    public static class JsonFactoryIndex extends ConcurrentHashMap<Object, JsonFactory> implements Map<Object, JsonFactory> {
        private static final long serialVersionUID = 4596787150066539880L;        
    }
    
    public int size() {
        int size = 0;
        for (JsonFactoryIndex fi: this.index.values()) {
            size = Math.max(size, fi.size());
        }
        return size;
    }
    
}
