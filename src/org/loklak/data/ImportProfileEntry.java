/**
 *  ImportProfileEntry
 *  Copyright 24.07.2015 by Dang Hai An, @zyzo
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

import org.loklak.harvester.HarvestingFrequency;
import org.loklak.harvester.SourceType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportProfileEntry extends AbstractIndexEntry implements IndexEntry {

    protected String id;
    protected Date created_at;

    protected Date last_modified;
    // importer username
    protected String screen_name;
    // importer ip address
    protected String client_host;
    protected URL source_url;
    protected SourceType source_type;

    // harvesting frequency (in min)
    protected HarvestingFrequency harvesting_freq;
    protected int lifetime;

    // id list of imported messages
    protected List<String> imported;

    protected EntryStatus activeStatus;


    @SuppressWarnings("unchecked")
    public ImportProfileEntry(Map<String, Object> map) {
        try {
            this.source_url = new URL((String) map.get("source_url"));
        } catch (MalformedURLException e) {
            this.source_url = null;
        }
        String source_type_string = (String) map.get("source_type");
        if (source_type_string == null) source_type_string = SourceType.USER.name();
        try {
            this.source_type = SourceType.valueOf(source_type_string);
        } catch (IllegalArgumentException e) {
            this.source_type = SourceType.USER;
        }
        this.created_at = parseDate(map.get("created_at"));
        this.last_modified = parseDate(map.get("last_modified"));
        this.screen_name = (String) map.get("screen_name");
        this.client_host = (String) map.get("client_host");

        try {
            this.harvesting_freq = HarvestingFrequency.init((int) parseLong(map.get("harvesting_freq")));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("harvesting_freq value not permitted : " + map.get("harvesting_freq"));
        }
        this.lifetime = (int) parseLong(map.get("lifetime"));
        this.imported = (List<String>) map.get("imported");
        this.id = (String) map.get("id_str");

        // profile should be active in the beginning
        this.activeStatus = EntryStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(Date created_at) {
        this.created_at = created_at;
    }

    public Date getLastModified() {
        return last_modified;
    }

    public void setLastModified(Date last_modified) {
        this.last_modified = last_modified;
    }

    public String getScreenName() {
        return screen_name;
    }

    public void setScreenName(String screen_name) {
        this.screen_name = screen_name;
    }

    public String getClientHost() {
        return client_host;
    }

    public void setClientHost(String client_host) {
        this.client_host = client_host;
    }

    public URL getSourceUrl() {
        return source_url;
    }

    public void setSourceUrl(URL source_url) {
        this.source_url = source_url;
    }

    public SourceType getSourceType() {
        return source_type;
    }

    public void setSourceType(SourceType source_type) {
        this.source_type = source_type;
    }

    public HarvestingFrequency getHarvestingFreq() {
        return harvesting_freq;
    }

    public void setHarvestingFreq(HarvestingFrequency harvesting_freq) {
        this.harvesting_freq = harvesting_freq;
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public List<String> getImported() {
        return imported;
    }

    public void setImported(List<String> imported) {
        this.imported = imported;
    }


    public EntryStatus getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(EntryStatus status) {
        this.activeStatus = status;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_str", this.id);
        m.put("created_at", utcFormatter.print(this.created_at.getTime()));
        m.put("last_modified", utcFormatter.print(this.last_modified.getTime()));
        m.put("screen_name", this.screen_name);
        m.put("client_host", this.client_host);
        m.put("source_url", this.source_url.toString());
        m.put("source_type", this.source_type.name());
        m.put("harvesting_freq", this.harvesting_freq.getFrequency());
        m.put("lifetime", this.lifetime);
        m.put("imported", this.imported);
        m.put("active_status", this.activeStatus.name());
        return m;
    }

}
