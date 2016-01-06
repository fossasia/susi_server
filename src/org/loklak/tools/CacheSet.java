/**
 *  CacheSet
 *  Copyright 04.01.2016 by Michael Peter Christen, @0rb1t3r
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
import java.util.LinkedHashSet;

public class CacheSet<K> {

    private int setSize;
    private LinkedHashSet<K> set;
    
    public CacheSet(int maxSize) {
        this.setSize = maxSize;
        this.set = new LinkedHashSet<K>();
    }

    public void clear() {
        this.set.clear();
    }
    
    private void checkSize() {
        if (this.set.size() >= this.setSize) {
            Iterator<K> i = this.set.iterator();
            while (i.hasNext() && this.set.size() > this.setSize) this.set.remove(i.next());
        }
    }
    
    public boolean full() {
        return this.set.size() >= this.setSize;
    }
    
    public boolean add(K key) {
        boolean oldval;
        synchronized (this.set) {
            // make room; this may remove entries from the beginning of the list
            checkSize();
            
            // we remove the value first to ensure that the value gets at the end of the list
            oldval = this.set.remove(key);
            
            // the new value gets to the end of the list
            this.set.add(key);
        }
        return oldval;
    }
    
    public boolean contains(K key) {
        synchronized (this.set) {
            // we remove the value to add it again at the end of the list
            if (!this.set.remove(key)) return false; // in case that the entry does not exist we are ready here
            
            // the new entry gets to the end of the list
            this.set.add(key);
        }
        return true;
    }
    
    public boolean remove(K key) {
        synchronized (this.set) {
            return this.set.remove(key);
        }
    }
    
}
