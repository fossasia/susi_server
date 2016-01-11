/**
 *  IndexFactory
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
import org.loklak.harvester.SourceType;
import org.loklak.tools.json.JSONObject;

public interface IndexFactory<Entry extends IndexEntry> {

    /**
     * get a mapping for the index definition
     * @return a json string which can be used to define an index in elasticsearch
     */
    public XContentBuilder getMapping();

    public Entry init(Map<String, Object> map) throws IOException;
    
    //public Entry init(JSONObject json) throws IOException;

    public boolean exists(String id);
    
    public boolean existsCache(String id);
    
    public boolean delete(String id, SourceType sourceType);
    
    public Map<String, Object> readMap(String id);

    public void writeEntry(String id, String type, Entry entry, boolean bulk) throws IOException;
    
    public void close();
    
}
