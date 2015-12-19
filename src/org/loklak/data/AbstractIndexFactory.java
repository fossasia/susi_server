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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.VersionType;
import org.loklak.harvester.SourceType;
import org.loklak.tools.Cache;

/**
 * test calls:
 * curl "http://localhost:9000/api/account.json?screen_name=test"
 * curl -g "http://localhost:9000/api/account.json?action=update&data={\"screen_name\":\"test\",\"apps\":{\"wall\":{\"type\":\"vertical\"}}}"
 */
public abstract class AbstractIndexFactory<Entry extends IndexEntry> implements IndexFactory<Entry> {


    private final static VersionType update_version_type = VersionType.FORCE;
    private final static int MAX_BULK_SIZE = 1500;
    
    protected final Client elasticsearch_client;
    protected final Cache<String, Entry> cache;
    protected final String index_name;
    
    public AbstractIndexFactory(final Client elasticsearch_client, final String index_name, final int cacheSize) {
        this.elasticsearch_client = elasticsearch_client;
        this.index_name = index_name;
        this.cache = new Cache<>(cacheSize);
    }
    
    public Entry read(String id) throws IOException {
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
        if (this.cache.exist(id)) return true;
        return elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet().isExists();
    }
    
    @Override
    public boolean delete(String id, SourceType sourceType) {
        this.cache.remove(id);
            return elasticsearch_client.prepareDelete(index_name, sourceType.name(), id).execute().actionGet().isFound();
    }

    @Override
    public Map<String, Object> readMap(String id) {
        return getMap(elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet());
    }
    
    protected static Map<String, Object> getMap(GetResponse response) {
        Map<String, Object> map = null;
        if (response.isExists() && (map = response.getSourceAsMap()) != null) {
            map.put("$type", response.getType());
        }
        return map;
    }
    
    public void writeEntry(String id, String type, Entry entry, boolean bulk) throws IOException {
        if (bulk) {
            BulkEntry be = new BulkEntry(id, type, entry);
            if (be.jsonMap != null) try {
                bulkCache.put(be);
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            }
            if (bulkCacheSize() >= MAX_BULK_SIZE) bulkCacheFlush(); // protect against OOM
        } else {
            bulkCacheFlush();
            this.cache.put(id, entry);
            // record user into search index
            Map<String, Object> jsonMap = entry.toMap();
            
            /*
             * best data format here would be XContentBuilder because the data is converted into
             * this format always; in this case with these lines
             *   XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
             *   builder.map(source);
             */
            if (jsonMap != null) {
                elasticsearch_client.prepareIndex(this.index_name, type, id).setSource(jsonMap)
                    .setVersion(1).setVersionType(update_version_type).execute().actionGet();
                //System.out.println("writing 1 entry"); // debug
            }
        }
    }
    
    public int bulkCacheSize() {
        return this.bulkCache.size();
    }

    public int bulkCacheFlush() throws IOException {
        if (this.bulkCache.size() == 0) return 0;
        
        BulkRequestBuilder bulkRequest = elasticsearch_client.prepareBulk();
        int count = 0;
        while (this.bulkCache.size() > 0) {
            BulkEntry be = this.bulkCache.poll();
            if (be == null) break;
            be.jsonMap.put("_version_type", update_version_type.name()); // set version type as metadata
            // be.jsonMap.put("_version", 1); // cannot change DocValues type from NUMERIC to SORTED_NUMERIC 
            bulkRequest.add(elasticsearch_client.prepareIndex(this.index_name, be.type, be.id).setSource(be.jsonMap));
            count++;
            if (count >= MAX_BULK_SIZE) break; // protect against OOM, the cache can be filled concurrently
        }
        if (count == 0) return 0;
        //System.out.println("writing bulk of " + count + " entries"); // debug
        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) throw new IOException(bulkResponse.buildFailureMessage());
        return count;
    }
    
    private BlockingQueue<BulkEntry> bulkCache = new ArrayBlockingQueue<>(2 * MAX_BULK_SIZE);
    
    private class BulkEntry {
        private String id;
        private String type;
        private Map<String, Object> jsonMap;
        public BulkEntry(String id, String type, Entry entry) {
            this.id = id;
            this.type = type;
            this.jsonMap = entry.toMap();
        }
    }
    
    public void close() {
        try {
            this.bulkCacheFlush();
        } catch (IOException e) {
        }
    }

}
