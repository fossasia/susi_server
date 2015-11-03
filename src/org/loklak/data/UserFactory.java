/**
 *  UserFactory
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
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class UserFactory extends AbstractIndexFactory<UserEntry> implements IndexFactory<UserEntry> {

    public final static String field_screen_name = "screen_name"; // used as id of the record
    public final static String field_user_id = "user_id"; // to reference the id of the providing service (here: twitter)
    public final static String field_name = "name";
    public final static String field_profile_image_url_http = "profile_image_url_http";
    public final static String field_profile_image_url_https = "profile_image_url_https";
    public final static String field_profile_image = "profile_image";
    public final static String field_appearance_first = "appearance_first";
    public final static String field_appearance_latest = "appearance_latest";
    
    
    public UserFactory(final Client elasticsearch_client, final String index_name, final int cacheSize) {
        super(elasticsearch_client, index_name, cacheSize);
    }

    @Override
    public XContentBuilder getMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                .startObject()
                  .startObject("properties") // the id has been omitted on purpose, we identify the user by its screen_name (even if that is changeable..)
                    .startObject(field_screen_name).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject() // our identification of the object
                    .startObject(field_name).field("type","string").field("include_in_all","false").endObject()
                    .startObject(field_user_id).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject() // stored as reference to twitter, not as identification in loklak
                    .startObject(field_appearance_first).field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(field_appearance_latest).field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(field_profile_image_url_http).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(field_profile_image_url_https).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(field_profile_image).field("type","binary").field("index","no").field("include_in_all","false").field("index","not_analyzed").endObject() // base64 of the image if available
                  .endObject()
                .endObject();
            return mapping;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UserEntry init(Map<String, Object> map) {
        return new UserEntry(map);
    }
    
}