/**
 *  AccountFactory
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
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.tools.json.JSONObject;

public class AccountFactory extends AbstractIndexFactory<AccountEntry> implements IndexFactory<AccountEntry> {

    public enum Field {
        screen_name,
        source_type,
        oauth_token,
        oauth_token_secret,
        authentication_first,
        authentication_latest,
        apps;
    }
    
    public AccountFactory(final Client elasticsearch_client, final String index_name, final int cacheSize, final int existSize) {
        super(elasticsearch_client, index_name, cacheSize, existSize);
    }

    @Override
    public XContentBuilder getMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                .startObject()
                  .startObject("properties") // the id has been omitted on purpose, we identify the user by its screen_name (even if that is changeable..)
                    .startObject(Field.screen_name.name()).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.source_type.name()).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.authentication_first.name()).field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.authentication_latest.name()).field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.oauth_token.name()).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.oauth_token_secret.name()).field("type","string").field("include_in_all","false").field("index","not_analyzed").endObject()
                    .startObject(Field.apps.name()).field("type","object").field("include_in_all","false").endObject()
                  .endObject()
                .endObject();
            return mapping;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AccountEntry init(Map<String, Object> map) throws IOException {
        return new AccountEntry(map);
    }
    
}