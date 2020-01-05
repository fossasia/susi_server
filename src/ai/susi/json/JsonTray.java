/**
 *  JsonTray
 *  Copyright 2016 by Michael Peter Christen
 *  First released 05.06.2016
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


package ai.susi.json;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.tools.CacheMap;

/**
 * The JsonTray class is a very simple database storage solution for json objects.
 * Any json object is addressed with a single key.
 * Json objects may be either persistent (it's stored to a file right after it was written here),
 * or they might be volatile (they are not written at all and only stored in case the object is closed).
 */
public class JsonTray {
    
    private JsonFile per;
    private CacheMap<String, JSONObject> vol;
    private File file_volatile;
    private long file_volatile_lastModified;
    
    public JsonTray(File file_persistent, File file_volatile, int cachesize) throws IOException {
        this.per = new JsonFile(file_persistent, false);
        this.vol = new CacheMap<String, JSONObject>(cachesize);
        this.file_volatile = file_volatile;
        this.file_volatile_lastModified = this.file_volatile.lastModified();
        if (this.file_volatile != null && this.file_volatile.exists()) try {
            JSONObject j = JsonFile.readJson(this.file_volatile);
            for (String key: j.keySet()) this.vol.put(key, j.getJSONObject(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DAO.log("init JsonTray persistent '" + file_persistent.getAbsolutePath() + "' " + getPersistentSize());
        DAO.log("init JsonTray volatile '" + file_volatile.getAbsolutePath() + "' " + getVolatileSize());
    }

    public int getPersistentSize() {
        return this.per.length();
    }
    
    public int getVolatileSize() {
        return this.vol.size();
    }
	
    public void close() {
        // commit any data that has not yet been stored.
        this.commit();
        
        if (this.file_volatile != null) try {
        	// copy the volatile data into a JSONObject to store it in a dump file
            JSONObject j = new JSONObject(true);
            for (Map.Entry<String, JSONObject> entry: this.vol.getMap().entrySet()) {
                j.put(entry.getKey(), entry.getValue());
            }
            
            // check the storage time of the volatile file; if it was stored in between, read it again and merge the data
            if (this.file_volatile.lastModified() > this.file_volatile_lastModified) {
            	JSONObject k = JsonFile.readJson(this.file_volatile);
            	for (String s: k.keySet()) {
            		if (!j.has(s)) j.put(s, k.get(s));
            	}
            }
            
            // finally store the data
            JsonFile.writeJson(this.file_volatile, j, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean has(String key) {
        synchronized(this.vol) {
            if (this.vol.exist(key)) return true;
        }
        return this.per.has(key);
    }
    
    public JsonTray put(String key, JSONObject value, boolean persistent) {
        if (persistent) putPersistent(key, value); else putVolatile(key, value);
        return this;
    }
    
    private JsonTray putPersistent(String key, JSONObject value) {
        this.per.put(key, value);
        return this;
    }
    
    private JsonTray putVolatile(String key, JSONObject value) {
        synchronized (this.vol) {
            this.vol.put(key, value);
        }
        return this;
    }
    
    public JsonTray remove(String key){
    	synchronized(this.vol) {
            if (this.vol.exist(key)){
            	this.vol.remove(key);
            	return this;
            }
        }
    	if(this.per.has(key)){
    		this.per.remove(key);
    	}
    	return this;
    }
    
    public JsonTray commit() {
        this.per.commit();
        return this;
    }
    
    public JSONObject getJSONObject(String key) {
        synchronized(this.vol) {
            JSONObject value = this.vol.get(key);
            if (value != null) return value;
        }
        return this.per.getJSONObject(key);
    }
    
    public JSONObject toJSON() {
    	JSONObject j = new JSONObject();
    	for (String key : this.per.keySet()){
    		j.put(key, this.per.get(key));
    	}
    	synchronized (this.vol) {
    		LinkedHashMap<String,JSONObject> map = this.vol.getMap();
	    	for(String key : map.keySet()){
	    		j.put(key, map.get(key));
	    	}
    	}
    	return j;
    }
    
    public Collection<String> keys() {
    	ArrayList<String> keys = new ArrayList<>();
    	keys.addAll(this.per.keySet());
    	synchronized (this.vol) {
    		keys.addAll(this.vol.getMap().keySet());
    	}
    	return keys;
    }
}
