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
import org.loklak.tools.json.JSONObject;

public class AccountEntry extends AbstractIndexEntry implements IndexEntry {

    public enum Field {
        screen_name,
        source_type,
        oauth_token,
        oauth_token_secret,
        authentication_first,
        authentication_latest,
        apps;
    }
    
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
        this.map.put(Field.authentication_first.name(), parseDate(this.map.get(Field.authentication_first.name()), now));
        this.map.put(Field.authentication_latest.name(), parseDate(this.map.get(Field.authentication_latest.name()), now));
        boolean containsAuth = this.map.containsKey(Field.oauth_token.name()) && this.map.containsKey(Field.oauth_token_secret.name());
        if (this.map.containsKey(Field.source_type.name())) {
            // verify the type
            try {
                SourceType st = SourceType.valueOf((String) this.map.get(Field.source_type.name()));
                this.map.put(Field.source_type.name(), st.toString());
            } catch (IllegalArgumentException e) {
                throw new IOException(Field.source_type.name() + " contains unknown type " + (String) this.map.get(Field.source_type.name()));
            }
        } else {
            this.map.put(Field.source_type.name(), SourceType.TWITTER);
        }
        if (!this.map.containsKey(Field.apps.name()) && !containsAuth) {
            throw new IOException("account data must either contain authentication details or an apps setting");
        }
    }
    
    public String getScreenName() {
        return parseString((String) this.map.get(Field.screen_name.name()));
    }

    public Date getAuthenticationFirst() {
        return parseDate(this.map.get(Field.authentication_first.name()));
    }

    public Date getAuthenticationLatest() {
        return parseDate(this.map.get(Field.authentication_latest.name()));
    }

    public SourceType getSourceType() {
        Object st = this.map.get(Field.source_type.name());
        if (st == null) return SourceType.TWITTER;
        if (st instanceof SourceType) return (SourceType) st;
        if (st instanceof String) return SourceType.valueOf((String) st);
        return SourceType.TWITTER;
    }

    public String getOauthToken () {
        return (String) this.map.get(Field.oauth_token.name());
    }

    public String getOauthTokenSecret () {
        return (String) this.map.get(Field.oauth_token_secret.name());
    }

    public String getApps() {
        return (String) this.map.get(Field.apps.name());
    }

    @Override
    public Map<String, Object> toMap() {
        return toMap(null);
    }
    
    @Override
    public JSONObject toJSON() {
        return toJSON(null);
    }
    
    public Map<String, Object> toMap(UserEntry user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(Field.authentication_latest.name(), utcFormatter.print(getAuthenticationLatest().getTime()));
        m.put(Field.authentication_first.name(), utcFormatter.print(getAuthenticationFirst().getTime()));
        m.put(Field.source_type.name(), getSourceType().toString());
        m.put(Field.screen_name.name(), getScreenName());
        if (this.map.containsKey(Field.oauth_token.name())) m.put(Field.oauth_token.name(), this.map.get(Field.oauth_token.name()));
        if (this.map.containsKey(Field.oauth_token_secret.name())) m.put(Field.oauth_token_secret.name(), this.map.get(Field.oauth_token_secret.name()));
        if (this.map.containsKey(Field.apps.name())) {
            m.put(Field.apps.name(), this.map.get(Field.apps.name()));
        }
        // add user
        if (user != null) m.put("user", user.toMap());
        return m;
    }
    
    public JSONObject toJSON(UserEntry user) {
        JSONObject m = new JSONObject();
        m.put(Field.authentication_latest.name(), utcFormatter.print(getAuthenticationLatest().getTime()));
        m.put(Field.authentication_first.name(), utcFormatter.print(getAuthenticationFirst().getTime()));
        m.put(Field.source_type.name(), getSourceType().toString());
        m.put(Field.screen_name.name(), getScreenName());
        if (this.map.containsKey(Field.oauth_token.name())) m.put(Field.oauth_token.name(), this.map.get(Field.oauth_token.name()));
        if (this.map.containsKey(Field.oauth_token_secret.name())) m.put(Field.oauth_token_secret.name(), this.map.get(Field.oauth_token_secret.name()));
        if (this.map.containsKey(Field.apps.name())) {
            m.put(Field.apps.name(), this.map.get(Field.apps.name()));
        }
        // add user
        if (user != null) m.put("user", user.toJSON());
        return m;
    }
    
    public static Map<String, Object> toEmptyAccount(UserEntry user) {
        assert user != null;
        Map<String, Object> m = new LinkedHashMap<>();
        if (user != null) m.put("user", user.toMap());
        return m;
    }
    
}
