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

package org.loklak.objects;

import java.io.IOException;
import java.util.Date;
import org.json.JSONObject;
import org.loklak.harvester.SourceType;

public class AccountEntry extends AbstractObjectEntry implements ObjectEntry {

    public enum Field {
        screen_name,
        source_type,
        oauth_token,
        oauth_token_secret,
        authentication_first,
        authentication_latest,
        apps;
    }
    
    private final JSONObject json;

    private AccountEntry() {
        this.json = new JSONObject(true);
    }
    
    public AccountEntry(final JSONObject map) throws IOException {
        this();
        this.init(map);
    }
    
    public void init(final JSONObject extmap) throws IOException {
        this.json.putAll(extmap);
        Date now = new Date();
        this.json.put(Field.authentication_first.name(), this.json.has(Field.authentication_first.name()) ? parseDate(this.json.get(Field.authentication_first.name()), now) : now);
        this.json.put(Field.authentication_latest.name(), this.json.has(Field.authentication_latest.name()) ? parseDate(this.json.get(Field.authentication_latest.name()), now) : now);
        boolean containsAuth = this.json.has(Field.oauth_token.name()) && this.json.has(Field.oauth_token_secret.name());
        if (this.json.has(Field.source_type.name())) {
            // verify the type
            try {
                SourceType st = SourceType.valueOf((String) this.json.get(Field.source_type.name()));
                this.json.put(Field.source_type.name(), st.toString());
            } catch (IllegalArgumentException e) {
                throw new IOException(Field.source_type.name() + " contains unknown type " + (String) this.json.get(Field.source_type.name()));
            }
        } else {
            this.json.put(Field.source_type.name(), SourceType.TWITTER);
        }
        if (!this.json.has(Field.apps.name()) && !containsAuth) {
            throw new IOException("account data must either contain authentication details or an apps setting");
        }
    }
    
    public String getScreenName() {
        return parseString((String) this.json.get(Field.screen_name.name()));
    }

    public Date getAuthenticationFirst() {
        return parseDate(this.json.get(Field.authentication_first.name()));
    }

    public Date getAuthenticationLatest() {
        return parseDate(this.json.get(Field.authentication_latest.name()));
    }

    public SourceType getSourceType() {
        Object st = this.json.get(Field.source_type.name());
        if (st == null) return SourceType.TWITTER;
        if (st instanceof SourceType) return (SourceType) st;
        if (st instanceof String) return SourceType.valueOf((String) st);
        return SourceType.TWITTER;
    }

    public String getOauthToken () {
        return (String) this.json.get(Field.oauth_token.name());
    }

    public String getOauthTokenSecret () {
        return (String) this.json.get(Field.oauth_token_secret.name());
    }

    public String getApps() {
        return (String) this.json.get(Field.apps.name());
    }
    
    @Override
    public JSONObject toJSON() {
        return toJSON(null);
    }
    
    public JSONObject toJSON(UserEntry user) {
        JSONObject m = new JSONObject();
        m.put(Field.authentication_latest.name(), utcFormatter.print(getAuthenticationLatest().getTime()));
        m.put(Field.authentication_first.name(), utcFormatter.print(getAuthenticationFirst().getTime()));
        m.put(Field.source_type.name(), getSourceType().toString());
        m.put(Field.screen_name.name(), getScreenName());
        if (this.json.has(Field.oauth_token.name())) m.put(Field.oauth_token.name(), this.json.get(Field.oauth_token.name()));
        if (this.json.has(Field.oauth_token_secret.name())) m.put(Field.oauth_token_secret.name(), this.json.get(Field.oauth_token_secret.name()));
        if (this.json.has(Field.apps.name())) {
            m.put(Field.apps.name(), this.json.get(Field.apps.name()));
        }
        // add user
        if (user != null) m.put("user", user.toJSON());
        return m;
    }
    
    public static JSONObject toEmptyAccountJson(UserEntry user) {
        assert user != null;
        JSONObject m = new JSONObject();
        if (user != null) m.put("user", user.toJSON());
        return m;
    }
    
}
