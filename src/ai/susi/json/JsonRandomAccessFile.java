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


package ai.susi.json;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.tools.BufferedRandomAccessFile;

public class JsonRandomAccessFile extends BufferedRandomAccessFile {

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
        super(dumpFile, "rw", 1 << 14);
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

    /**
     * The JsonHandle class is a bundle of a json with the information about the
     * seek location in the file and the length of bytes of the original json string
     */
    public static class JsonHandle implements JsonFactory {
        private JSONObject json;
        private long index;
        private int length;
        public JsonHandle(JSONObject json, long index, int length) {
            this.json = json;
            this.index = index;
            this.length = length;
        }
        public JSONObject getJSON() {
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
        public JSONObject getJSON() throws IOException {
            byte[] b = new byte[this.length];
            JsonRandomAccessFile.this.read(b, this.index);
            return new JSONObject(new String(b, 0, b.length, StandardCharsets.UTF_8));
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
                return this.getJSON().toString();
            } catch (IOException e) {
            	DAO.severe(e);
                return "";
            }
        }
    }
    
    public void close() throws IOException {
        super.close();
    }

}
