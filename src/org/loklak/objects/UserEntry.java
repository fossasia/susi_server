/**
 *  UserEntry
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.common.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;

public class UserEntry extends AbstractObjectEntry implements ObjectEntry {

    public final static String field_screen_name = "screen_name"; // used as id of the record
    public final static String field_user_id = "user_id"; // to reference the id of the providing service (here: twitter)
    public final static String field_name = "name";
    public final static String field_profile_image_url_http = "profile_image_url_http";
    public final static String field_profile_image_url_https = "profile_image_url_https";
    public final static String field_profile_image = "profile_image";
    public final static String field_appearance_first = "appearance_first";
    public final static String field_appearance_latest = "appearance_latest";
    
    private final Map<String, Object> map;

    public UserEntry(String user_id, String screen_name_raw, String profile_image_url, String name_raw) {
        this.map =  new LinkedHashMap<>();
        this.map.put(field_user_id, user_id);
        this.map.put(field_screen_name, screen_name_raw.replaceAll("</?s>", "").replaceAll("</?b>", "").replaceAll("@", ""));
        this.map.put(field_name, name_raw);
        this.map.put(profile_image_url.startsWith("https:") ? field_profile_image_url_https : field_profile_image_url_http, profile_image_url);
        long now = System.currentTimeMillis();
        this.map.put(field_appearance_first, new Date(now));
        this.map.put(field_appearance_latest, new Date(now));
    }

    public UserEntry(final JSONObject json) {
        this.map = new LinkedHashMap<String, Object>();
        Iterator<String> ki =json.keys();
        while (ki.hasNext()) {
            String key = ki.next();
            try {
                Object val = json.get(key);
                this.map.put(key, val);
            } catch (JSONException e) {}
        }
        Date now = new Date();
        map.put(field_appearance_first, parseDate(map.get(field_appearance_first), now));
        map.put(field_appearance_latest, parseDate(map.get(field_appearance_latest), now));
    }

    
    public SourceType getType() {
        try {
            return SourceType.byName(parseString((String) this.map.get("$type")));
        } catch (RuntimeException e) {
        	Log.getLog().warn(e);
            return null;
        }
    }
    
    public Number getUser() {
        String id = getUserId();
        try {
            return id == null ? null : Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public String getUserId() {
        return parseString((String) this.map.get(field_user_id));
    }

    public String getScreenName() {
        return parseString((String) this.map.get(field_screen_name));
    }

    public String getName() {
        return MessageEntry.html2utf8(parseString((String) this.map.get(field_name))); // html2utf8 should not be necessary here since it is already applied in the scraper; however there are old data lines which had not been converted
    }

    public String getProfileImageUrl() {
        Object url = this.map.get(field_profile_image_url_https);
        if (url != null) return (String) url;
        return parseString((String) this.map.get(field_profile_image_url_http));
    }

    public boolean containsProfileImage() {
        Object image = this.map.get(field_profile_image);
        return image != null && ((String) image).length() > 0;
    }

    public byte[] getProfileImage() {
        Object image = this.map.get(field_profile_image);
        if (image == null) return null;
        try {
            return Base64.decode((String) image);
        } catch (IOException e) {
            return null;
        }
    }

    public void setProfileImageUrl(String url) {
        this.map.put(url.startsWith("https:") ? field_profile_image_url_https : field_profile_image_url_http, url);
    }
    
    public void setProfileImage(byte[] image) {
        this.map.put(field_profile_image, Base64.encodeBytes(image));
    }

    public Date getAppearanceFirst() {
        return parseDate(this.map.get(field_appearance_first));
    }

    public Date getAppearanceLatest() {
        return parseDate(this.map.get(field_appearance_latest));
    }
    
    public void toJSON(JsonGenerator json) {
        try {
            json.writeStartObject(); // object name for this should be 'user'
            json.writeObjectField(field_screen_name, getScreenName());
            json.writeObjectField(field_user_id, getUserId());
            json.writeObjectField(field_name, getName());
            if (this.map.containsKey(field_profile_image_url_http)) json.writeObjectField(field_profile_image_url_http, this.map.get(field_profile_image_url_http));
            if (this.map.containsKey(field_profile_image_url_https)) json.writeObjectField(field_profile_image_url_https, this.map.get(field_profile_image_url_https));
            writeDate(json, field_appearance_first, getAppearanceFirst().getTime());
            writeDate(json, field_appearance_latest, getAppearanceLatest().getTime());
            if (this.map.containsKey(field_profile_image)) json.writeObjectField(field_profile_image, this.map.get(field_profile_image));
            json.writeEndObject();
        } catch (IOException e) {
        }
    }
    
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(field_screen_name, getScreenName());
        json.put(field_user_id, getUserId());
        json.put(field_name, getName());
        if (this.map.containsKey(field_profile_image_url_http)) json.put(field_profile_image_url_http, this.map.get(field_profile_image_url_http));
        if (this.map.containsKey(field_profile_image_url_https)) json.put(field_profile_image_url_https, this.map.get(field_profile_image_url_https));
        json.put(field_appearance_first, utcFormatter.print(getAppearanceFirst().getTime()));
        json.put(field_appearance_latest, utcFormatter.print(getAppearanceLatest().getTime()));
        if (this.map.containsKey(field_profile_image)) json.put(field_profile_image, this.map.get(field_profile_image));
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserEntry)) return false;
        UserEntry u = (UserEntry) o;
        return this.map.equals(u.map);
    }
    
    public static void main(String args[]) {
        System.out.println(new UserEntry("", "test", "http://test.com", "Mr. Test").toString());
        //String j = "{\"name\":\"Mr. Test\",\"screen_name\":\"test\",\"profile_image_url\":\"http://test.com\"}";
    }
}
