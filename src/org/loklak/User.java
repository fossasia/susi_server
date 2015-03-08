/**
 *  User
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

package org.loklak;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

public class User {
    
    private String profile_image_url, name, screen_name;
    private Date appearance_first, appearance_latest;

    public User(String screen_name_raw, String profile_image_url, String name_raw) {
        this.screen_name = screen_name_raw.replaceAll("</?s>", "").replaceAll("</?b>", "").replaceAll("@", "");
        this.profile_image_url = profile_image_url;
        this.name = name_raw;
        long now = System.currentTimeMillis();
        this.appearance_first = new Date(now);
        this.appearance_latest = new Date(now);
    }
    
    public User(Map<String, Object> map) {
        this.screen_name = (String) map.get("screen_name");
        this.name = (String) map.get("name");
        this.profile_image_url = (String) map.get("profile_image_url_https");
        if (this.profile_image_url == null) {
            this.profile_image_url = (String) map.get("profile_image_url");
        }
        String appearance_first_string = (String) map.get("appearance_first");
        if (appearance_first_string != null && appearance_first_string.length() > 0) {
            this.appearance_first = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(appearance_first_string).toDate();
        } else {
            this.appearance_first = new Date(System.currentTimeMillis());
        }
        String appearance_latest_string = (String) map.get("appearance_latest");
        if (appearance_latest_string != null && appearance_latest_string.length() > 0) {
            this.appearance_latest = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(appearance_latest_string).toDate();
        } else {
            this.appearance_latest = new Date(System.currentTimeMillis());
        }
    }

    public String getProfileImageUrl() {
        return profile_image_url;
    }

    public String getName() {
        return name;
    }

    public String getScreenName() {
        return screen_name;
    }

    public Date getAppearanceFirst() {
        return appearance_first;
    }

    public Date getAppearanceLatest() {
        return appearance_latest;
    }

    /*
     * define the mapping for the json data type
     */
    public static XContentBuilder MAPPING;
    static {
        try {
            MAPPING = XContentFactory.jsonBuilder()
                .startObject()
                  .startObject("properties") // the id has been omitted on purpose, we identify the user by its screen_name (even if that is changeable..)
                    .startObject("name").field("type","string").endObject()
                    .startObject("screen_name").field("type","string").field("index","not_analyzed").field("doc_values", true).endObject()
                    .startObject("appearance_first").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("appearance_latest").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("profile_image_url_http").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("profile_image_url_https").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
                  .endObject()
                .endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void toJSON(XContentBuilder m) {
        try {
            m.startObject(); // object name for this should be 'user'
            m.field("name", this.name);
            m.field("screen_name", this.screen_name);
            //m.field("location", );
            //m.field("description", );
            m.field(this.profile_image_url.startsWith("https:") ? "profile_image_url_https" : "profile_image_url", this.profile_image_url);
            m.field("appearance_first", this.appearance_first);
            m.field("appearance_latest", this.appearance_latest);
            m.endObject();
        } catch (IOException e) {
        }
    }
    
    public XContentBuilder toJSON() {
        try {
            XContentBuilder m = XContentFactory.jsonBuilder();
            toJSON(m);
            return m;
        } catch (IOException e) {
            return null;
        }
    }
    
    public String toString() {
        return toJSON().bytes().toUtf8();
    }
    
    public static void main(String args[]) {
        System.out.println(new User("test", "http://test.com", "Mr. Test").toString());
        
        String j = "{\"name\":\"Mr. Test\",\"screen_name\":\"test\",\"profile_image_url\":\"http://test.com\"}";
                
        XContentParser parser = null;
        try {
          XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
          json.startObject();
          parser=XContentFactory.xContent(XContentType.JSON).createParser(j.getBytes(Charset.forName("UTF-8")));
          parser.nextToken();
          json.field("parsed").copyCurrentStructure(parser);
          json.endObject();
          System.out.println(json.string());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
          if (parser != null) {
            parser.close();
          }
        }
    }
}
