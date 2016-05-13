package org.loklak.tools.storage;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.json.JSONObject;
import org.json.JSONObjectTest;
import org.junit.After;
import org.junit.Before;
import org.loklak.tools.BufferedRandomAccessFile;
import org.loklak.tools.storage.JsonDataset.Column;
import org.loklak.tools.storage.JsonDataset.JsonFactoryIndex;

public class JsonDatasetTest extends TestCase {
    
    private File testFile;
    
    @Before
    public void setUp() throws Exception {
        this.testFile = BufferedRandomAccessFile.Test.getTestFile();
    }

    @After
    public void tearDown() throws Exception {
        this.testFile.delete();
    }

    public void test() throws IOException {
        JsonFactoryIndex idx;
        for (JsonRepository.Mode mode: JsonRepository.Mode.values()) try {

            // write the file
            JsonDataset dtst = new JsonDataset(this.testFile, "idx_", new Column[]{new Column("abc", true), new Column("def", false)}, null, null, mode, false, Integer.MAX_VALUE);
            JSONObject json = JSONObjectTest.testJson(true);
            dtst.putUnique(json);
            dtst.close();

            // read the file
            dtst = new JsonDataset(this.testFile, "idx_", new Column[]{new Column("abc", true), new Column("def", false)}, null, null, mode, false, Integer.MAX_VALUE);
            idx = dtst.index.get("abc");
            System.out.println(idx.get(1));
            idx = dtst.index.get("def");
            System.out.println(idx.get("Hello World"));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
