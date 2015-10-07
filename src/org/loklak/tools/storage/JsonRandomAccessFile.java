/**
 *  JsonRandomAccessFile
 *  Copyright 2015 by Michael Peter Christen
 *  First released 04.10.2015
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


package org.loklak.tools.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.log.Log;
import org.junit.After;
import org.junit.Before;
import org.loklak.data.DAO;
import org.loklak.tools.ASCII;
import org.loklak.tools.BufferedRandomAccessFile;
import org.loklak.tools.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonRandomAccessFile extends BufferedRandomAccessFile implements JsonReader {
    
    public final static com.fasterxml.jackson.core.JsonFactory jsonFactory = new com.fasterxml.jackson.core.JsonFactory();
    public final static ObjectMapper jsonMapper = new ObjectMapper(jsonFactory);
    public final static TypeReference<HashMap<String,Object>> jsonTypeRef = new TypeReference<HashMap<String,Object>>() {};

    private File file;
    private int concurrency;
    private ArrayBlockingQueue<JsonFactory> jsonline;
    
    /**
     * if a JsonRandomAccessFile object in initiated, it must be wrapped with a Thread object and started.
     * @param dumpFile
     * @param concurrency
     * @throws IOException
     */
    public JsonRandomAccessFile(final File dumpFile, final int concurrency) throws IOException {
        super(dumpFile, "rw");
        this.file = dumpFile;
        this.concurrency = concurrency;
        this.jsonline = new ArrayBlockingQueue<>(1000);
    }
    
    public String getName() {
        return this.file.getAbsolutePath();
    }
    
    public int getConcurrency() {
        return this.concurrency;
    }
    
    public JsonFactory take() throws InterruptedException {
        return this.jsonline.take();
    }

    public void run() {
        try {
            BufferedRandomAccessFile.IndexedLine line;
            while ((line = this.readIndexedLine()) != null) {
                try {
                    Map<String, Object> json = DAO.jsonMapper.readValue(line.getText(), DAO.jsonTypeRef);
                    if (json == null) continue;
                    this.jsonline.put(new JsonHandle(json, line.getPos(), line.getText().length));
                } catch (Throwable e) {
                    Log.getLog().warn("cannot parse line in file " + JsonRandomAccessFile.this.file + ": \"" + line + "\"", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            for (int i = 0; i < this.concurrency; i++) {
                try {this.jsonline.put(JsonReader.POISON_JSON_MAP);} catch (InterruptedException e) {}
            }
        }
    }
    
    /**
     * The JsonHandle class is a bundle of a json with the information about the
     * seek location in the file and the length of bytes of the original json string
     */
    public static class JsonHandle implements JsonFactory {
        private Map<String, Object> json;
        private long index;
        private int length;
        public JsonHandle(Map<String, Object> json, long index, int length) {
            this.json = json;
            this.index = index;
            this.length = length;
        }
        public Map<String, Object> getJson() {
            return json;
        }
        public long getIndex() {
            return index;
        }
        public int getLength() {
            return length;
        }
        public String toString() {
            return new JSONObject(this.json).toString();
        }
    }
    
    public JsonFactory getJsonFactory(long index, int length) {
        return new ReaderJsonFactory(index, length);
    }
    
    public class ReaderJsonFactory implements JsonFactory {

        private long index;
        private int length;
        
        public ReaderJsonFactory(long index, int length) {
            this.index = index;
            this.length = length;
        }
        
        @Override
        public Map<String, Object> getJson() throws IOException {
            byte[] b = new byte[this.length];
            JsonRandomAccessFile.this.read(b, this.index);
            return jsonMapper.readValue(b, jsonTypeRef);
        }
        public long getIndex() {
            return this.index;
        }
        public int getLength() {
            return this.length;
        }
        public File getFile() {
            return JsonRandomAccessFile.this.file;
        }
        public String toString() {
            try {
                return new JSONObject(this.getJson()).toString();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
    
    public void close() throws IOException {
        super.close();
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test.class);
    }

    public static class Test extends TestCase {
        
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
                                String screen_name = (String) jh.getJson().get("screen_name");
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
                String json = new JSONObject(e.getValue().getJson()).toString();
                if (!ASCII.String(b).equals(json)) System.out.println(ASCII.String(b) + " != " + json);
                assertTrue(ASCII.String(b).equals(json));
            }
            reader.close();
        }
        
    }

}
