package org.loklak.susi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.json.JSONArray;
/**
 *  SusiData
 *  Copyright 29.06.2016 by Michael Peter Christen, @0rb1t3r
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

import org.json.JSONObject;

public class SusiData extends JSONObject {

    final String metadata_name, data_name;

    public SusiData() {
        super(true);
        this.metadata_name = "metadata";
        this.data_name = "data";
    }
    
    public SusiData(Matcher m) {
        this();
        this.setOffset(0).setCount(1).setHits(1);
        JSONObject row = new JSONObject();
        row.put("0", m.group(0));
        for (int i = 0; i < m.groupCount(); i++) {
            row.put(Integer.toString(i + 1), m.group(i + 1));
        }
        this.setData(new JSONArray().put(row));
    }
    
    @Deprecated
    public SusiData(String metadata_name, String data_name) {
        super(true);
        this.metadata_name = metadata_name;
        this.data_name = data_name;
    }

    public SusiData setOffset(int offset) {
        getMetadata().put("offset", offset);
        return this;
    }
    
    public SusiData setHits(int hits) {
        getMetadata().put("hits", hits);
        return this;
    }
    
    public SusiData setCount(int count) {
        getMetadata().put("count", count);
        return this;
    }
    
    public SusiData setQuery(String query) {
        getMetadata().put("query", query);
        return this;
    }
    
    public SusiData setScraperInfo(String scraperInfo) {
        getMetadata().put("scraperInfo", scraperInfo);
        return this;
    }
    
    private JSONObject getMetadata() {
        if (this.has(metadata_name)) return this.getJSONObject(metadata_name);
        JSONObject metadata = new JSONObject();
        this.put(metadata_name, metadata);
        return metadata;
    }
    
    public SusiData setData(JSONArray table) {
        this.put(data_name, table);
        return this;
    }

    public JSONArray getData() {
        return this.getJSONArray(data_name);
    }
    
    public SusiData setActions(List<SusiAction> actions) {
        JSONArray a = new JSONArray();
        actions.forEach(action -> a.put(action.toJSON()));
        this.put("actions", a);
        return this;
    }
    
    public List<SusiAction> getActions() {
        List<SusiAction> actions = new ArrayList<>();
        if (!this.has("actions")) return actions;
        this.getJSONArray("actions").forEach(action -> actions.add(new SusiAction((JSONObject) action)));
        return actions;
    }
    
}
