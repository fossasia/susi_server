/**
 *  CacheStats
 *  Copyright 18.04.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

public class CacheStats {

    private AtomicLong update, hit, miss;
    
    public CacheStats() {
        this.update = new AtomicLong(0);
        this.hit = new AtomicLong(0);
        this.miss = new AtomicLong(0);
    }

    public void clear() {
        this.update.set(0);
        this.hit.set(0);
        this.miss.set(0);
    }

    public void update() {
        this.update.incrementAndGet();
    }

    public void hit() {
        this.hit.incrementAndGet();
    }

    public void miss() {
        this.miss.incrementAndGet();
    }
    
    public long getUpdate() {
        return this.update.get();
    }

    public long getHit() {
        return this.hit.get();
    }

    public long getMiss() {
        return this.miss.get();
    }
    
    public JSONObject getJSON() {
        JSONObject json = new JSONObject(true);
        json.put("update", getUpdate());
        json.put("hit", getHit());
        json.put("miss", getMiss());
        return json;
    }
    
}
