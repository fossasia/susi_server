package org.loklak.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.elasticsearch.common.settings.Settings;
import org.json.JSONObject;
import org.json.JSONObjectTest;
import org.junit.After;
import org.junit.Before;
import org.loklak.data.ElasticsearchClient.BulkEntry;
import org.loklak.data.ElasticsearchClient.BulkWriteResult;
import org.loklak.tools.BufferedRandomAccessFile;

public class ElasticsearchClientTest extends TestCase {
    
    private File testFile;
    
    @Before
    public void setUp() throws Exception {
        this.testFile = BufferedRandomAccessFile.Test.getTestFile();
    }

    @After
    public void tearDown() throws Exception {
        this.testFile.delete();
    }

    private List<BulkEntry> testBulk(int from, int to) {
        List<BulkEntry> bulk = new ArrayList<>();
        for (int i = from; i < to; i++) {
            JSONObject testJson = JSONObjectTest.testJson(true);
            BulkEntry entry = new BulkEntry("id_" + i, "testtype", null, null, testJson.toMap());    
            bulk.add(entry);
        }
        return bulk;
    }
    
    public void test() throws IOException {
        Settings.Builder settings = Settings.builder();
        settings.put("path.home", this.testFile.getAbsolutePath());
        settings.put("path.data", this.testFile.getAbsolutePath());
        settings.build();
        ElasticsearchClient client = new ElasticsearchClient(settings);
        String indexName = "test";
        List<BulkEntry> bulk = testBulk(0, 1100);
        BulkWriteResult result = client.writeMapBulk(indexName, bulk);
        assertTrue(result.getCreated().size() == 1100);
        assertTrue(result.getErrors().size() == 0);
        bulk = testBulk(1000, 1200);
        result = client.writeMapBulk(indexName, bulk);
        assertTrue(result.getCreated().size() == 100);
        assertTrue(result.getErrors().size() == 0);
        client.close();
    }
}
