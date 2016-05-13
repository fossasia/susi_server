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
package org.loklak.objects;

import org.json.JSONObject;
import org.loklak.harvester.HarvestingFrequency;
import org.loklak.harvester.SourceType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ImportProfileEntry extends AbstractIndexEntry implements IndexEntry {

    public static enum EntryStatus {
        ACTIVE,
        DELETED
    }

    public static enum PrivacyStatus {
        PRIVATE,
        PUBLIC
    }
    
    protected String id;
    protected Date created_at;

    // last time the import profile entry is modified
    protected Date last_modified;

    // last time the url is harvested & updated by loklak harvester
    protected Date last_harvested;
    // importer screen name
    protected String importer;
    // importer ip address
    protected String client_host;
    protected URL source_url;
    protected long source_hash;
    protected SourceType source_type;

    // harvesting frequency (in min)
    protected HarvestingFrequency harvesting_freq;
    protected long lifetime;

    // id list of imported messages
    protected List<String> imported;

    // id list of users sharing this data
    protected List<String> sharers;

    protected EntryStatus activeStatus;

    protected PrivacyStatus privacyStatus;

    public ImportProfileEntry(JSONObject json) {
        try {
            this.source_url = new URL(json.getString("source_url"));
        } catch (MalformedURLException e) {
            this.source_url = null;
        }
        String source_type_string = json.getString("source_type");
        if (source_type_string == null) source_type_string = SourceType.USER.name();
        try {
            this.source_type = SourceType.valueOf(source_type_string.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.getLogger("ImportProfileEntry").warning("Illegal source type value : " + source_type_string);
            this.source_type = SourceType.USER;
        }
        this.source_hash = json.getLong("source_hash");
        this.created_at = parseDate(json.getString("created_at"));
        this.last_modified = parseDate(json.getString("last_modified"));
        this.last_harvested = parseDate(json.getString("last_harvested"));
        this.importer = json.getString("importer");
        this.client_host = json.getString("client_host");

        try {
            this.harvesting_freq = HarvestingFrequency.valueOf((int) json.getLong("harvesting_freq"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("harvesting_freq value not permitted : " + json.getLong("harvesting_freq"));
        }
        this.lifetime = json.getLong("lifetime");
        this.imported = new ArrayList<>();
        for (Object o: json.getJSONArray("imported")) this.imported.add((String) o);
        this.sharers = new ArrayList<>();
        for (Object o: json.getJSONArray("sharers")) this.sharers.add((String) o);
        this.id = json.getString("id_str");

        // profile should be active in the beginning
        try {
            this.activeStatus = EntryStatus.valueOf(json.getString("active_status"));
        } catch (IllegalArgumentException|NullPointerException e) {
            this.activeStatus = EntryStatus.ACTIVE;
        }
        try {
            this.privacyStatus = PrivacyStatus.valueOf(json.getString("privacy_status"));
        } catch (IllegalArgumentException|NullPointerException e) {
            this.privacyStatus = PrivacyStatus.PRIVATE;
        }
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

    public Date getLastHarvested() {
        return last_harvested;
    }

    public void setLastHarvested(Date last_harvested) {
        this.last_harvested = last_harvested;
    }

    public String getImporter() {
        return importer;
    }

    public void setImporter(String importer) {
        this.importer = importer;
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

    public long getSourceHash() {
        return source_hash;
    }

    public void setSourceHash(long source_hash) {
        this.source_hash = source_hash;
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

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public List<String> getImported() {
        return imported;
    }

    public void setImported(List<String> imported) {
        this.imported = imported;
    }

    public List<String> getSharers() {
        return sharers;
    }

    public void setSharers(List<String> sharers) {
        this.sharers = sharers;
    }

    public EntryStatus getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(EntryStatus status) {
        this.activeStatus = status;
    }

    public PrivacyStatus getPrivacyStatus() {
        return privacyStatus;
    }

    public void setPrivacyStatus(PrivacyStatus privacyStatus) {
        this.privacyStatus = privacyStatus;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id_str", this.id);
        json.put("created_at", utcFormatter.print(this.created_at.getTime()));
        json.put("last_modified", utcFormatter.print(this.last_modified.getTime()));
        json.put("last_harvested", utcFormatter.print(this.last_harvested.getTime()));
        json.put("importer", this.importer);
        json.put("client_host", this.client_host);
        json.put("source_url", this.source_url.toString());
        json.put("source_hash", this.source_hash);
        json.put("source_type", this.source_type.name());
        json.put("harvesting_freq", this.harvesting_freq.getFrequency());
        json.put("lifetime", this.lifetime);
        json.put("imported", this.imported);
        json.put("sharers", this.sharers);
        json.put("active_status", this.activeStatus.name());
        json.put("privacy_status", this.privacyStatus.name());
        return json;
    }

}
