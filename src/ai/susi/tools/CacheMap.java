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

package ai.susi.tools;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class CacheMap<K,V> {

    private int maxSize;
    private LinkedHashMap<K, V> map;
    
    public CacheMap(int maxSize) {
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>();
    }

    public int size() {
        return this.map.size();
    }

    public int maxSize() {
        return this.maxSize;
    }
    
    public void clear() {
        this.map.clear();
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
                return null;
            }
            
            // the old value gets to the end of the list
            this.map.put(key, value);
        }
        return value;
    }
    
    public V remove(K key) {
        synchronized (this.map) {
            return this.map.remove(key);
        }
    }
    
    public boolean exist(K key) {
        boolean exist = false;
        synchronized (this.map) {
            exist = this.map.containsKey(key);
        }
        return exist;
    }
    
    public LinkedHashMap<K,V> getMap(){
    	return map;
    }
    
}
