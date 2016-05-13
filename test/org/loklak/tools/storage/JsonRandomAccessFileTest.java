package org.loklak.tools.storage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.After;
import org.junit.Before;
import org.loklak.tools.ASCII;
import org.loklak.tools.BufferedRandomAccessFile;
import org.loklak.tools.storage.JsonRandomAccessFile.JsonHandle;

public class JsonRandomAccessFileTest  extends TestCase {
    
    private File testFile;
    
    @Before
    public void setUp() throws Exception {
        this.testFile = BufferedRandomAccessFile.Test.getTestFile();
        BufferedRandomAccessFile.Test.writeLines(this.testFile, BufferedRandomAccessFile.Test.getTestLines(100000));
    }

    @After
    public void tearDown() throws Exception {
        this.testFile.delete();
    }

    public void testRead() throws IOException {
        File f = new File("data/accounts/own/users_201508_74114447.txt");

        final int concurrency = 8;
        final JsonRandomAccessFile reader = new JsonRandomAccessFile(f, concurrency);
        final Thread readerThread = new Thread(reader);
        readerThread.start();
        Thread[] t = new Thread[concurrency];
        final ConcurrentHashSet<String> names = new ConcurrentHashSet<>();
        for (int i = 0; i < concurrency; i++) {
            t[i] = new Thread() {
                public void run() {
                    JsonFactory ij;
                    try {
                        while ((ij = reader.take()) != JsonReader.POISON_JSON_MAP) {
                            //assertTrue(ij instanceof JsonHandle);
                            JsonHandle jh = (JsonHandle) ij;
                            //assertTrue(jh.getJson() != null);
                            String screen_name = (String) jh.getJSON().get("screen_name");
                            if (names.contains(screen_name)) System.out.println("double name: " + screen_name);
                            //assertFalse(names.contains(screen_name));
                            names.add(screen_name);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            t[i].start();
        }
        for (int i = 0; i < concurrency; i++) try {t[i].join();} catch (InterruptedException e) {throw new IOException(e.getMessage());}
        reader.close();
    }
    
    public void test() throws IOException {
        final int concurrency = 8;
        final JsonRandomAccessFile reader = new JsonRandomAccessFile(this.testFile, concurrency);
        final Thread readerThread = new Thread(reader);
        readerThread.start();
        long start = System.currentTimeMillis();
        final Map<Long, JsonHandle> m = new ConcurrentHashMap<>();
        Thread[] t = new Thread[concurrency];
        for (int i = 0; i < concurrency; i++) {
            t[i] = new Thread() {
                public void run() {
                    JsonFactory ij;
                    try {
                        while ((ij = reader.take()) != JsonReader.POISON_JSON_MAP) {
                            assert(ij instanceof JsonHandle);
                            m.put(((JsonHandle) ij).getIndex(), (JsonHandle) ij);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            t[i].start();
        }
        for (int i = 0; i < concurrency; i++) try {t[i].join();} catch (InterruptedException e) {throw new IOException(e.getMessage());}
        System.out.println("time: " + (System.currentTimeMillis() - start));
        // test if random read is identical to original
        for (Map.Entry<Long, JsonHandle> e: m.entrySet()) {
            byte[] b = new byte[e.getValue().getLength()];
            reader.read(b, e.getValue().getIndex());
            String json = e.getValue().getJSON().toString();
            if (!ASCII.String(b).equals(json)) System.out.println(ASCII.String(b) + " != " + json);
            assertTrue(ASCII.String(b).equals(json));
        }
        reader.close();
    }
    
}