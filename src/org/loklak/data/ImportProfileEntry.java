package org.loklak.data;

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
    // importer username
    protected String screen_name;
    // importer ip address
    protected String client_host;
    protected URL source_url;
    protected SourceType source_type;
    protected int harvesting_freq;
    protected int lifetime;

    // id list of imported messages
    protected List<String> imported;


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
        this.screen_name = (String) map.get("screen_name");
        this.client_host = (String) map.get("client_host");
        this.harvesting_freq = (int) map.get("harvesting_freq");
        this.lifetime = (int) map.get("lifetime");
        this.imported = (List<String>) map.get("imported");
        this.id = (String) map.get("id_str");
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

    public int getHarvestingFreq() {
        return harvesting_freq;
    }

    public void setHarvestingFreq(int harvesting_freq) {
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


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id_str", this.id);
        m.put("created_at", utcFormatter.print(this.created_at.getTime()));
        m.put("screen_name", this.screen_name);
        m.put("client_host", this.client_host);
        m.put("source_url", this.source_url.toString());
        m.put("source_type", this.source_type.name());
        m.put("harvesting_freq", this.harvesting_freq);
        m.put("lifetime", this.lifetime);
        m.put("imported", this.imported);
        return m;
    }

}
