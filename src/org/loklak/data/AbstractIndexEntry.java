/**
 *  AbstractIndexEntry
 *  Copyright 26.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.data;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public abstract class AbstractIndexEntry implements IndexEntry {

    public AbstractIndexEntry() {
    }
    
    public AbstractIndexEntry(Map<String, Object> map) {
        this.init(map);
    }
    
    public String toString() {
        try {
            XContentBuilder m = XContentFactory.jsonBuilder();
            this.toJSON(m);
            String s = m.bytes().toUtf8();
            m.close();
            return s;
        } catch (IOException e) {
            return null;
        }
    }
    
    public XContentBuilder toJSON() {
        try {
            XContentBuilder m = XContentFactory.jsonBuilder();
            this.toJSON(m);
            return m;
        } catch (IOException e) {
            return null;
        }
    }
}
