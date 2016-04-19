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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.loklak.harvester.SourceType;
import org.loklak.objects.AbstractIndexEntry;
import org.loklak.objects.IndexEntry;
import org.loklak.tools.CacheMap;
import org.loklak.tools.CacheSet;
import org.loklak.tools.CacheStats;

/**
 * test calls:
 * curl "http://localhost:9000/api/account.json?screen_name=test"
 * curl -g "http://localhost:9000/api/account.json?action=update&data={\"screen_name\":\"test\",\"apps\":{\"wall\":{\"type\":\"vertical\"}}}"
 */
public abstract class AbstractIndexFactory<Entry extends IndexEntry> implements IndexFactory<Entry> {
    
    private final static int         MAX_BULK_SIZE       =  1500;
    private final static int         MAX_BULK_TIME       = 10000;
    
    protected final ElasticsearchClient elasticsearch_client;
    protected final CacheMap<String, Entry> objectCache;
    private CacheSet<String> existCache;
    protected final String index_name;
    private long lastBulkWrite;
    private AtomicLong indexWrite, indexExist, indexGet;
    
    
    public AbstractIndexFactory(final ElasticsearchClient elasticsearch_client, final String index_name, final int cacheSize, final int existSize) {
        this.elasticsearch_client = elasticsearch_client;
        this.index_name = index_name;
        this.objectCache = new CacheMap<>(cacheSize);
        this.existCache = new CacheSet<>(existSize);
        this.lastBulkWrite = System.currentTimeMillis();
        this.indexWrite = new AtomicLong(0);
        this.indexExist = new AtomicLong(0);
        this.indexGet = new AtomicLong(0);
    }
    
    public CacheStats getObjectStats() {
        return this.objectCache.getStats();
    }
    
    public CacheStats getExistStats() {
        return this.existCache.getStats();
    }
    
    public JSONObject getStats() {
        JSONObject json = new JSONObject(true);
        json.put("name", index_name);
        json.put("object_cache", this.objectCache.getStatsJson());
        json.put("exist_cache", this.existCache.getStatsJson());
        JSONObject index = new JSONObject();
        index.put("write", this.indexWrite.get());
        index.put("exist", this.indexExist.get());
        index.put("get", this.indexGet.get());
        json.put("index", index);
        return json;
    }
    
    public Entry read(String id) throws IOException {
        assert id != null;
        if (id == null) return null;
        Entry entry = this.objectCache.get(id);
        if (entry != null) {
            this.existCache.add(id);
            return entry;
        }
        Map<String, Object> map = readMap(id);
        if (map == null) return null;
        entry = init(map);
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        return entry;
    }
    
    @Override
    public boolean exists(String id) {
        if (existsCache(id)) return true;
        boolean exist = elasticsearch_client.exist(index_name, id, null);
        this.indexExist.incrementAndGet();
        if (exist) this.existCache.add(id);
        return exist;
    }

    @Override
    public boolean existsCache(String id) {
        return this.objectCache.exist(id) || this.existCache.contains(id);
    }
    
    @Override
    public boolean delete(String id, SourceType sourceType) {
        this.objectCache.remove(id);
        this.existCache.remove(id);
        return elasticsearch_client.delete(index_name, id, sourceType.name());
    }

    @Override
    public Map<String, Object> readMap(String id) {
        Map<String, Object> json = elasticsearch_client.readMap(index_name, id);
        if (json != null) this.existCache.add(id);
        this.indexGet.incrementAndGet();
        return json;
    }
    
    public void writeEntry(String id, String type, Entry entry, boolean bulk) throws IOException {
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        if (bulk) {
            ElasticsearchClient.BulkEntry be = new ElasticsearchClient.BulkEntry(id, type, AbstractIndexEntry.TIMESTAMP_FIELDNAME, null, entry.toMap());
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
                elasticsearch_client.writeMap(this.index_name, jsonMap, id, type);
                this.indexWrite.incrementAndGet();
                //System.out.println("writing 1 entry"); // debug
            }
        }
    }
    
    public int bulkCacheSize() {
        return this.bulkCache.size();
    }

    public List<Map.Entry<String, String>> bulkCacheFlush() {
        this.lastBulkWrite = System.currentTimeMillis();
        if (this.bulkCache.size() == 0) return new ArrayList<Map.Entry<String, String>>(0);
        
        int count = 0;
        List<ElasticsearchClient.BulkEntry> jsonMapList = new ArrayList<ElasticsearchClient.BulkEntry>();
        while (this.bulkCache.size() > 0) {
            ElasticsearchClient.BulkEntry be = this.bulkCache.poll();
            if (be == null) break;
            jsonMapList.add(be);
            count++;
            if (count >= MAX_BULK_SIZE) break; // protect against OOM, the cache can be filled concurrently
        }
        if (count == 0) return new ArrayList<Map.Entry<String, String>>(0);
        List<Map.Entry<String, String>> errors = elasticsearch_client.writeMapBulk(this.index_name, jsonMapList);
        this.indexWrite.addAndGet(jsonMapList.size());
        return errors;
    }
    
    private BlockingQueue<ElasticsearchClient.BulkEntry> bulkCache = new ArrayBlockingQueue<>(2 * MAX_BULK_SIZE);
    
    public void close() {
        this.bulkCacheFlush();
    }

}
