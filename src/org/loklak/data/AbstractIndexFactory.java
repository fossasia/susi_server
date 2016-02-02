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
import org.loklak.objects.AbstractIndexEntry;
import org.loklak.objects.IndexEntry;
import org.loklak.tools.CacheMap;
import org.loklak.tools.CacheSet;

/**
 * test calls:
 * curl "http://localhost:9000/api/account.json?screen_name=test"
 * curl -g "http://localhost:9000/api/account.json?action=update&data={\"screen_name\":\"test\",\"apps\":{\"wall\":{\"type\":\"vertical\"}}}"
 */
public abstract class AbstractIndexFactory<Entry extends IndexEntry> implements IndexFactory<Entry> {
    
    private final static VersionType UPDATE_VERSION_TYPE = VersionType.FORCE;
    private final static int         MAX_BULK_SIZE       =  1500;
    private final static int         MAX_BULK_TIME       = 10000;
    
    protected final Client elasticsearch_client;
    protected final CacheMap<String, Entry> objectCache;
    private CacheSet<String> existCache;
    protected final String index_name;
    private long lastBulkWrite;
    
    
    public AbstractIndexFactory(final Client elasticsearch_client, final String index_name, final int cacheSize, final int existSize) {
        this.elasticsearch_client = elasticsearch_client;
        this.index_name = index_name;
        this.objectCache = new CacheMap<>(cacheSize);
        this.existCache = new CacheSet<>(existSize);
        this.lastBulkWrite = System.currentTimeMillis();
    }
    
    public Entry read(String id) throws IOException {
        assert id != null;
        if (id == null) return null;
        Entry entry = objectCache.get(id);
        if (entry != null) return entry;
        Map<String, Object> map = readMap(id);
        if (map == null) return null;
        entry = init(map);
        objectCache.put(id, entry);
        existCache.add(id);
        return entry;
    }
    
    @Override
    public boolean exists(String id) {
        if (existsCache(id)) return true;
        boolean exist = elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet().isExists();
        if (exist) this.existCache.add(id);
        return exist;
    }

    @Override
    public boolean existsCache(String id) {
        return (this.objectCache.exist(id) || this.existCache.contains(id));
    }
    
    @Override
    public boolean delete(String id, SourceType sourceType) {
        this.objectCache.remove(id);
        this.existCache.remove(id);
        return elasticsearch_client.prepareDelete(index_name, sourceType.name(), id).execute().actionGet().isFound();
    }

    @Override
    public Map<String, Object> readMap(String id) {
        Map<String, Object> json = getMap(elasticsearch_client.prepareGet(index_name, null, id).execute().actionGet());
        if (json != null) this.existCache.add(id);
        return json;
    }
    
    protected static Map<String, Object> getMap(GetResponse response) {
        Map<String, Object> map = null;
        if (response.isExists() && (map = response.getSourceAsMap()) != null) {
            map.put("$type", response.getType());
        }
        return map;
    }
    
    public void writeEntry(String id, String type, Entry entry, boolean bulk) throws IOException {
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        if (bulk) {
            BulkEntry be = new BulkEntry(id, type, entry);
            if (be.jsonMap != null) try {
                bulkCache.put(be);
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            }
            if (bulkCacheSize() >= MAX_BULK_SIZE || this.lastBulkWrite + MAX_BULK_TIME < System.currentTimeMillis()) bulkCacheFlush(); // protect against OOM
        } else {
            bulkCacheFlush();
            // record user into search index
            Map<String, Object> jsonMap = entry.toMap();
            
            /*
             * best data format here would be XContentBuilder because the data is converted into
             * this format always; in this case with these lines
             *   XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
             *   builder.map(source);
             */
            if (jsonMap != null) {
                if (!jsonMap.containsKey(AbstractIndexEntry.TIMESTAMP_FIELDNAME)) jsonMap.put(AbstractIndexEntry.TIMESTAMP_FIELDNAME, AbstractIndexEntry.utcFormatter.print(System.currentTimeMillis()));
                elasticsearch_client.prepareIndex(this.index_name, type, id).setSource(jsonMap)
                    .setVersion(1).setVersionType(UPDATE_VERSION_TYPE).execute().actionGet();
                //System.out.println("writing 1 entry"); // debug
            }
        }
    }
    
    public int bulkCacheSize() {
        return this.bulkCache.size();
    }

    public int bulkCacheFlush() throws IOException {
        this.lastBulkWrite = System.currentTimeMillis();
        if (this.bulkCache.size() == 0) return 0;
        
        BulkRequestBuilder bulkRequest = elasticsearch_client.prepareBulk().setRefresh(true);
        int count = 0;
        while (this.bulkCache.size() > 0) {
            BulkEntry be = this.bulkCache.poll();
            if (be == null) break;
            be.jsonMap.put("_version_type", UPDATE_VERSION_TYPE.name()); // set version type as metadata
            // be.jsonMap.put("_version", 1); // cannot change DocValues type from NUMERIC to SORTED_NUMERIC 
            bulkRequest.add(elasticsearch_client.prepareIndex(this.index_name, be.type, be.id).setSource(be.jsonMap));
            count++;
            if (count >= MAX_BULK_SIZE) break; // protect against OOM, the cache can be filled concurrently
        }
        if (count == 0) return 0;
        //System.out.println("writing bulk of " + count + " entries"); // debug
        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) throw new IOException(bulkResponse.buildFailureMessage());
        // flush the translog cache
        
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
            if (!this.jsonMap.containsKey(AbstractIndexEntry.TIMESTAMP_FIELDNAME)) this.jsonMap.put(AbstractIndexEntry.TIMESTAMP_FIELDNAME, AbstractIndexEntry.utcFormatter.print(System.currentTimeMillis()));
        }
    }
    
    public void close() {
        try {
            this.bulkCacheFlush();
        } catch (IOException e) {
        }
    }

}
