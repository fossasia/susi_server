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
public abstract class AbstractIndexFactory<IndexObject extends ObjectEntry> implements IndexFactory<IndexObject> {
    
    protected final ElasticsearchClient elasticsearch_client;
    protected final CacheMap<String, IndexObject> objectCache;
    private CacheSet<String> existCache;
    protected final String index_name;
    private AtomicLong indexWrite, indexExist, indexGet;

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
    
    public IndexObject read(String id) throws IOException {
        assert id != null;
        if (id == null) return null;
        IndexObject entry = this.objectCache.get(id);
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

    @Override
    public boolean writeEntry(IndexEntry<IndexObject> entry) throws IOException {
        this.objectCache.put(entry.getId(), entry.getObject());
        this.existCache.add(entry.getId());
        // record user into search index
        JSONObject json = entry.getObject().toJSON();
        if (json == null) return false;
        
        /*
         * best data format here would be XContentBuilder because the data is converted into
         * this format always; in this case with these lines
         *   XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
         *   builder.map(source);
         */
        if (!json.has(AbstractObjectEntry.TIMESTAMP_FIELDNAME)) json.put(AbstractObjectEntry.TIMESTAMP_FIELDNAME, AbstractObjectEntry.utcFormatter.print(System.currentTimeMillis()));
        boolean newDoc = elasticsearch_client.writeMap(this.index_name, json.toMap(), entry.getType(), entry.getId());
        this.indexWrite.incrementAndGet();
        return newDoc;
    }

    @Override
    public ElasticsearchClient.BulkWriteResult writeEntries(Collection<IndexEntry<IndexObject>> entries) throws IOException {

        List<ElasticsearchClient.BulkEntry> jsonMapList = new ArrayList<ElasticsearchClient.BulkEntry>();
        
        for (IndexEntry<IndexObject> entry: entries) {
            this.objectCache.put(entry.getId(), entry.getObject());
            this.existCache.add(entry.getId());

            Map<String, Object> jsonMap = entry.getObject().toJSON().toMap();
            assert jsonMap != null;
            if (jsonMap == null) continue;
            ElasticsearchClient.BulkEntry be = new ElasticsearchClient.BulkEntry(entry.getId(), entry.getType(), AbstractObjectEntry.TIMESTAMP_FIELDNAME, null, jsonMap);

            jsonMapList.add(be);
        }
        if (jsonMapList.size() == 0) return ElasticsearchClient.EMPTY_BULK_RESULT;
        
        ElasticsearchClient.BulkWriteResult result = elasticsearch_client.writeMapBulk(this.index_name, jsonMapList);
        this.indexWrite.addAndGet(jsonMapList.size());
        return result;
    }
    
    public void close() {
    }

}
