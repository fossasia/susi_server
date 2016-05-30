package org.loklak.tools.storage;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.loklak.tools.BufferedRandomAccessFile;

import junit.framework.TestCase;

public class JsonFileTest extends TestCase {

    private File testFile;
    private JsonFile testJsonFile;
    
    @Before
    public void setUp() throws Exception {
        this.testFile = BufferedRandomAccessFile.Test.getTestFile();
        this.testJsonFile = new JsonFile(this.testFile);
    }

    @After
    public void tearDown() throws Exception {
        this.testFile.delete();
    }

    public void test() throws IOException {
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            this.testJsonFile.put("key", i);
        }
        long stop = System.currentTimeMillis();
        System.out.println("runtime: " + (stop - start) + " milliseconds");
        
    }
}
