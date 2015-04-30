/**
 *  QueryFactory
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

public class QueryFactory extends AbstractIndexFactory<QueryEntry> implements IndexFactory<QueryEntry> {

    public QueryFactory(final Client elasticsearch_client, final String index_name, final int cacheSize) {
        super(elasticsearch_client, index_name, cacheSize);
    }

    @Override
    public XContentBuilder getMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("properties")
                  .startObject("query").field("type","string").field("include_in_all","false").endObject()
                  .startObject("query_length").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("since").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("until").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("source_type").field("type","string").field("index","not_analyzed").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("timezoneOffset").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("query_first").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("query_last").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("retrieval_last").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("retrieval_next").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("expected_next").field("type","date").field("format","dateOptionalTime").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("query_count").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("retrieval_count").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("message_period").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("messages_per_day").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("score_retrieval").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                  .startObject("score_suggest").field("type","long").field("include_in_all","false").field("doc_values", true).endObject()
                .endObject()
              .endObject();
            return mapping;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public QueryEntry init(Map<String, Object> map) {
        return new QueryEntry(map);
    }
    
}