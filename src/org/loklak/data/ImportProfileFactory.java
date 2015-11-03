/**
 *  ImportProfileFactory
 *  Copyright 24.07.2015 by Dang Hai An, @zyzo
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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Map;

public class ImportProfileFactory extends AbstractIndexFactory<ImportProfileEntry> implements IndexFactory<ImportProfileEntry> {

    public ImportProfileFactory(Client elasticsearch_client, String index_name, int cacheSize) {
        super(elasticsearch_client, index_name, cacheSize);
    }

    @Override
    public XContentBuilder getMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
              .startObject()
                .startObject("properties")
                  .startObject("id").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("source_url").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("source_type").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("source_hash").field("type", "long").field("include_in_all", "false").endObject()
                    .startObject("harvesting_freq").field("type", "long").field("include_in_all", "false").endObject()
                    .startObject("lifetime").field("type", "long").field("include_in_all", "false").endObject()
                    .startObject("created_at").field("type", "date").field("format","dateOptionalTime").field("include_in_all", "false").endObject()
                    .startObject("last_modified").field("type", "date").field("format","dateOptionalTime").field("include_in_all", "false").endObject()
                    .startObject("last_harvested").field("type", "date").field("format","dateOptionalTime").field("include_in_all", "false").endObject()
                    .startObject("importer").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("client_host").field("type","string").field("include_in_all","false").endObject()
                    .startObject("active_status").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("privacy_status").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("imported").field("type", "string").field("include_in_all", "false").endObject()
                    .startObject("sharers").field("type", "string").field("include_in_all", "false").endObject()
                    .endObject()
              .endObject();

            return mapping;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ImportProfileEntry init(Map<String, Object> map) throws IOException {
        return new ImportProfileEntry(map);
    }
}
