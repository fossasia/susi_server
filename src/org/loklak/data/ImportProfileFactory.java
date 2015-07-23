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
                  .startObject("id").field("type", "string").field("include_in_all", "false").field("index", "not_analyzed").field("doc_values", true).endObject()
                    .startObject("source_url").field("type", "string").field("include_in_all", "false").field("doc_values", true).endObject()
                    .startObject("source_type").field("type", "string").field("include_in_all", "false").field("doc_values", true).endObject()
                    .startObject("harvesting_freq").field("type", "long").field("include_in_all", "false").field("doc_values", true).endObject()
                    .startObject("lifetime").field("type", "long").field("include_in_all", "false").field("doc_values", true).endObject()
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
