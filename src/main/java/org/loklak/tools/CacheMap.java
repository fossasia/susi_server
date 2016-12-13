/**
 *  CacheMap
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.JSONObject;

public class CacheMap<K,V> {

    private final int maxSize;
    private final LinkedHashMap<K, V> map;
    private final CacheStats stats;
    
    public CacheMap(int maxSize) {
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>();
        this.stats = new CacheStats();
    }

    public void clear() {
        this.map.clear();
        this.stats.clear();
    }
    
    public CacheStats getStats() {
        return this.stats;
    }
    
    public JSONObject getStatsJson() {
        JSONObject json = this.stats.getJSON();
        synchronized (this) {
            json.put("size", this.map.size());
            json.put("maxsize", this.maxSize);
        }
        return json;
    }
    
    private void checkSize() {
        if (this.map.size() >= this.maxSize) {
            Iterator<K> i = this.map.keySet().iterator();
            while (i.hasNext() && this.map.size() > this.maxSize) this.map.remove(i.next());
        }
    }
    
    public boolean full() {
        return this.map.size() >= this.maxSize;
    }
    
    public V put(K key, V value) {
        this.stats.update();
        V oldval;
        synchronized (this.map) {
            // make room; this may remove entries from the beginning of the list
            checkSize();
            
            // we remove the value first to ensure that the value gets at the end of the list
            oldval = this.map.remove(key);
            
            // the new value gets to the end of the list
            this.map.put(key, value);
        }
        return oldval;
    }
    
    public V get(K key) {
        V value;
        synchronized (this.map) {
            // we remove the value to add it again at the end of the list
            value = this.map.remove(key);
            
            // in case that the entry does not exist we are ready here
            if (value == null) {
                this.stats.miss();
                return null;
            }
            
            // the old value gets to the end of the list
            this.map.put(key, value);
        }
        this.stats.hit();
        return value;
    }
    
    public V remove(K key) {
        synchronized (this.map) {
            return this.map.remove(key);
        }
    }
    
    public boolean exist(K key) {
        boolean exist;
        synchronized (this.map) {
            exist = this.map.containsKey(key);
        }
        if (exist) this.stats.hit(); else this.stats.miss();
        return exist;
    }
    
    public LinkedHashMap<K,V> getMap(){
    	return map;
    }
    
}
