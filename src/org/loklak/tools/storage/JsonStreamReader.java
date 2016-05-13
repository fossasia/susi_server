/**
 *  JsonDumpReader
 *  Copyright 2015 by Michael Peter Christen
 *  First released 30.09.2015
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONObject;

public class JsonStreamReader implements JsonReader {

    private ArrayBlockingQueue<JsonFactory> jsonline;
    private InputStream inputStream;
    private int concurrency;
    private String name;

    public JsonStreamReader(InputStream inputStream, String name, int concurrency) {
        this.jsonline = new ArrayBlockingQueue<>(1000);
        this.inputStream = inputStream;
        this.name = name;
        this.concurrency = concurrency;
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getConcurrency() {
        return this.concurrency;
    }
    
    public JsonFactory take() throws InterruptedException {
        return this.jsonline.take();
    }
    
    public static class WrapperJsonFactory implements JsonFactory {
        JSONObject json;
        public WrapperJsonFactory(final JSONObject json) {
            this.json = json;
        }
        
        @Override
        public JSONObject getJSON() throws IOException {
            return this.json;
        }
    }

    public void run() {
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                try {
                    JSONObject json = new JSONObject(line);
                    this.jsonline.put(new WrapperJsonFactory(json));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < this.concurrency; i++) {
            try {this.jsonline.put(JsonReader.POISON_JSON_MAP);} catch (InterruptedException e) {}
        }
    }
    
}
