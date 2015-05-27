/**
 *  AccountEntry
 *  Copyright 27.05.2015 by Michael Peter Christen, @0rb1t3r
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.loklak.harvester.SourceType;

import com.fasterxml.jackson.core.JsonGenerator;

public class AccountEntry extends AbstractIndexEntry implements IndexEntry {
    
    private final static String field_screen_name = "screen_name";
    private final static String field_source_type = "source_type";
    private final static String field_consumerKey = "consumerKey";
    private final static String field_consumerSecret = "consumerSecret";
    private final static String field_authentication_first = "authentication_first";
    private final static String field_authentication_latest = "authentication_latest";
    private final static String field_apps = "apps";
    
    private final Map<String, Object> map;

    private AccountEntry() {
        this.map = new LinkedHashMap<>();
    }
    
    public AccountEntry(final Map<String, Object> map) throws IOException {
        this();
        this.init(map);
    }
    
    public void init(final Map<String, Object> extmap) throws IOException {
        this.map.putAll(extmap);
        Date now = new Date();
        this.map.put(field_authentication_first, parseDate(this.map.get(field_authentication_first), now));
        this.map.put(field_authentication_latest, parseDate(this.map.get(field_authentication_latest), now));
        boolean containsAuth = this.map.containsKey(field_consumerKey) && this.map.containsKey(field_consumerSecret);
        if (this.map.containsKey(field_source_type)) {
            // verify the type
            try {
                SourceType st = SourceType.valueOf((String) this.map.get(field_source_type));
                this.map.put(field_source_type, st.toString());
            } catch (IllegalArgumentException e) {
                throw new IOException(field_source_type + " contains unknown type " + (String) this.map.get(field_source_type));
            }
        } else {
            this.map.put(field_source_type, SourceType.TWITTER);
        }
        if (!this.map.containsKey(field_apps) && !containsAuth) {
            throw new IOException("account data must either contain authentication details or an apps setting");
        }
    }
    
    public String getScreenName() {
        return parseString((String) this.map.get(field_screen_name));
    }

    public Date getAuthenticationFirst() {
        return parseDate(this.map.get(field_authentication_first));
    }

    public Date getAuthenticationLatest() {
        return parseDate(this.map.get(field_authentication_latest));
    }

    public SourceType getSourceType() {
        Object st = this.map.get(field_source_type);
        if (st == null) return SourceType.TWITTER;
        if (st instanceof SourceType) return (SourceType) st;
        if (st instanceof String) return SourceType.valueOf((String) st);
        return SourceType.TWITTER;
    }

    public String getConsumerKey() {
        return (String) this.map.get(field_consumerKey);
    }

    public String getConsumerSecret() {
        return (String) this.map.get(field_consumerSecret);
    }

    public String getApps() {
        return (String) this.map.get(field_apps);
    }
    
    @Override
    public void toJSON(JsonGenerator json) {
        toJSON(json, null);
    }

    public void toJSON(JsonGenerator json, UserEntry user) {
        try {
            json.writeStartObject(); // object name for this should be 'user'
            writeDate(json, field_authentication_latest, getAuthenticationLatest().getTime());
            writeDate(json, field_authentication_first, getAuthenticationFirst().getTime());
            json.writeObjectField(field_source_type, getSourceType().toString());
            json.writeObjectField(field_screen_name, getScreenName());
            if (this.map.containsKey(field_consumerKey)) json.writeObjectField(field_consumerKey, this.map.get(field_consumerKey));
            if (this.map.containsKey(field_consumerSecret)) json.writeObjectField(field_consumerSecret, this.map.get(field_consumerSecret));
            if (this.map.containsKey(field_apps)) json.writeObjectField(field_apps, this.map.get(field_apps));
            
            // add user
            if (user != null) {
                json.writeFieldName("user");
                user.toJSON(json);
            }
            
            json.writeEndObject();
        } catch (IOException e) {
        }
    }
    
    public static void toEmptyAccountJSON(JsonGenerator json, UserEntry user) {
        assert user != null;
        try {
            json.writeStartObject(); // object name for this should be 'user'
            // add user
            if (user != null) {
                json.writeFieldName("user");
                user.toJSON(json);
            }
            json.writeEndObject();
        } catch (IOException e) {
        }
    }
    
}
