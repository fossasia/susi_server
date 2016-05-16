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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.loklak.harvester.SourceType;
import org.loklak.objects.AbstractObjectEntry;
import org.loklak.objects.ObjectEntry;
import org.loklak.tools.CacheMap;
import org.loklak.tools.CacheSet;
import org.loklak.tools.CacheStats;

/**
 * test calls:
 * curl "http://localhost:9000/api/account.json?screen_name=test"
 * curl -g "http://localhost:9000/api/account.json?action=update&data={\"screen_name\":\"test\",\"apps\":{\"wall\":{\"type\":\"vertical\"}}}"
 */
public abstract class AbstractIndexFactory<Entry extends ObjectEntry> implements IndexFactory<Entry> {
    
    private final static int MAX_BULK_SIZE = 1500;
    
    protected final ElasticsearchClient elasticsearch_client;
    protected final CacheMap<String, Entry> objectCache;
    private CacheSet<String> existCache;
    protected final String index_name;
    private AtomicLong indexWrite, indexExist, indexGet;    
    private BlockingQueue<ElasticsearchClient.BulkEntry> bulkCache = new ArrayBlockingQueue<>(2 * MAX_BULK_SIZE);

    public AbstractIndexFactory(final ElasticsearchClient elasticsearch_client, final String index_name, final int cacheSize, final int existSize) {
        this.elasticsearch_client = elasticsearch_client;
        this.index_name = index_name;
        this.objectCache = new CacheMap<>(cacheSize);
        this.existCache = new CacheSet<>(existSize);
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
        JSONObject json = readJSON(id);
        if (json == null) return null;
        entry = init(json);
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        return entry;
    }
    
    @Override
    public boolean exists(String id) {
        if (existsCache(id)) return true;
        boolean exist = elasticsearch_client.exist(index_name, null, id);
        this.indexExist.incrementAndGet();
        if (exist) this.existCache.add(id);
        return exist;
    }

    @Override
    public Set<String> existsBulk(Collection<String> ids) {
        Set<String> result = new HashSet<>();
        List<String> check = new ArrayList<>(ids.size());
        for (String id: ids) {
            if (existsCache(id)) result.add(id); else check.add(id);
        }
        this.indexExist.addAndGet(ids.size());
        Set<String> test = this.elasticsearch_client.existBulk(this.index_name, (String) null, check);
        for (String id: test) {
            this.existCache.add(id);
            result.add(id);
            //assert elasticsearch_client.exist(index_name, null, id); // uncomment for production
        }
        return result;
    }

    @Override
    public boolean existsCache(String id) {
        return this.objectCache.exist(id) || this.existCache.contains(id);
    }
    
    @Override
    public boolean delete(String id, SourceType sourceType) {
        this.objectCache.remove(id);
        this.existCache.remove(id);
        return elasticsearch_client.delete(index_name, sourceType.name(), id);
    }

    @Override
    public JSONObject readJSON(String id) {
        Map<String, Object> map = elasticsearch_client.readMap(index_name, id);
        this.indexGet.incrementAndGet();
        if (map == null) return null;
        this.existCache.add(id);
        return new JSONObject(map);
    }

    public boolean writeEntry(String id, String type, Entry entry) throws IOException {
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        bulkCacheFlush();
        // record user into search index
        JSONObject json = entry.toJSON();
        if (json == null) return false;
        
        /*
         * best data format here would be XContentBuilder because the data is converted into
         * this format always; in this case with these lines
         *   XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
         *   builder.map(source);
         */
        if (!json.has(AbstractObjectEntry.TIMESTAMP_FIELDNAME)) json.put(AbstractObjectEntry.TIMESTAMP_FIELDNAME, AbstractObjectEntry.utcFormatter.print(System.currentTimeMillis()));
        boolean newDoc = elasticsearch_client.writeMap(this.index_name, json.toMap(), type, id);
        this.indexWrite.incrementAndGet();
        return newDoc;
    }

    public void writeEntryBulk(String id, String type, Entry entry) throws IOException {
        this.objectCache.put(id, entry);
        this.existCache.add(id);
        Map<String, Object> jsonMap = entry.toJSON().toMap();
        assert jsonMap != null;
        if (jsonMap == null) return;
        ElasticsearchClient.BulkEntry be = new ElasticsearchClient.BulkEntry(id, type, AbstractObjectEntry.TIMESTAMP_FIELDNAME, null, jsonMap);
        try {
            bulkCache.put(be);
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (bulkCacheSize() >= MAX_BULK_SIZE) bulkCacheFlush(); // protect against OOM
        }
    }
    
    public int bulkCacheSize() {
        return this.bulkCache.size();
    }

    public ElasticsearchClient.BulkWriteResult bulkCacheFlush() {
        if (this.bulkCache.size() == 0) return ElasticsearchClient.EMPTY_BULK_RESULT;
        
        int count = 0;
        List<ElasticsearchClient.BulkEntry> jsonMapList = new ArrayList<ElasticsearchClient.BulkEntry>();
        while (this.bulkCache.size() > 0) {
            ElasticsearchClient.BulkEntry be = this.bulkCache.poll();
            if (be == null) break;
            jsonMapList.add(be);
            count++;
            if (count >= MAX_BULK_SIZE) break; // protect against OOM, the cache can be filled concurrently
        }
        if (count == 0) return ElasticsearchClient.EMPTY_BULK_RESULT;
        ElasticsearchClient.BulkWriteResult result = elasticsearch_client.writeMapBulk(this.index_name, jsonMapList);
        this.indexWrite.addAndGet(jsonMapList.size());
        return result;
    }
    
    public void close() {
        this.bulkCacheFlush();
    }

}
