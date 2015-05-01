/**
 *  AbstractIndexFactory
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

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.indices.IndexMissingException;
import org.loklak.tools.Cache;

public abstract class AbstractIndexFactory<Entry extends IndexEntry> implements IndexFactory<Entry> {

    protected final Client elasticsearch_client;
    protected final Cache<String, Entry> cache;
    protected final String index_name;
    
    public AbstractIndexFactory(final Client elasticsearch_client, final String index_name, final int cacheSize) {
        this.elasticsearch_client = elasticsearch_client;
        this.index_name = index_name;
        this.cache = new Cache<>(cacheSize);
    }
    
    public Entry read(String id) {
        assert id != null;
        if (id == null) return null;
        Entry entry = cache.get(id);
        if (entry != null) return entry;
        Map<String, Object> map = readMap(id);
        if (map == null) return null;
        entry = init(map);
        cache.put(id, entry);
        return entry;
    }
    
    @Override
    public boolean exists(String id) {
        try {
            return elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet().isExists();
        } catch (IndexMissingException e) {
            // may happen for first query
            return false;
        }
    }
    
    @Override
    public boolean delete(String id, SourceType sourceType) {
        try {
            return elasticsearch_client.prepareDelete(index_name, sourceType.name(), id).execute().actionGet().isFound();
        } catch (IndexMissingException e) {
            // may happen for first query
            return false;
        }
    }

    @Override
    public Map<String, Object> readMap(String id) {
        try {
            return getMap(elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet());
        } catch (IndexMissingException e) {
            // may happen for first query
            return null;
        }
    }
    
    protected static Map<String, Object> getMap(GetResponse response) {
        Map<String, Object> map;
        if (response.isExists() && (map = response.getSourceAsMap()) != null) {
            return map;
        }
        return null;
    }
    

    public void writeEntry(String id, String type, Entry entry) throws IOException {
        this.cache.put(id, entry);
        // record user into search index
        XContentBuilder json = XContentFactory.jsonBuilder();
        entry.toJSON(json);
        elasticsearch_client.prepareIndex(this.index_name, type, id)
                .setSource(json).setVersion(1).setVersionType(VersionType.FORCE).execute().actionGet();
        json.close();
    }
}
