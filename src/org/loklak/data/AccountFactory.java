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

public class AccountFactory extends AbstractIndexFactory<AccountEntry> implements IndexFactory<AccountEntry> {

    public AccountFactory(final Client elasticsearch_client, final String index_name, final int cacheSize) {
        super(elasticsearch_client, index_name, cacheSize);
    }

    @Override
    public XContentBuilder getMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                .startObject()
                  .startObject("properties") // the id has been omitted on purpose, we identify the user by its screen_name (even if that is changeable..)
                    .startObject("screen_name").field("type","string").field("include_in_all","false").field("index","not_analyzed").field("doc_values", true).endObject()
                    .startObject("source_type").field("type","string").field("include_in_all","false").field("index","not_analyzed").field("doc_values", true).endObject()
                    .startObject("authentication_first").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("authentication_latest").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("consumerKey").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("consumerSecret").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
                    .startObject("apps").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
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