/**
 *  GeoJsonReader
 *  Copyright 08.08.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.geo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.log.Log;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;

/**
 * High-efficient GeoJson parser (high-efficient because it is able to handle very large input files, much larger than
 * the main memory can hold (i.e. terabytes) using a stream parser within a multi-core environment. Parsed feature records
 * can be processed concurrently while the parser is stream-reading them.
 */
public class GeoJsonReader implements Runnable {

    public final static Feature POISON_FEATURE = new Feature();
    
    private final int concurrency;
    private final BlockingQueue<Feature> featureQueue;
    private final JsonParser parser;

    public GeoJsonReader(final InputStream is, final int concurrency) throws JsonParseException, IOException {
        this.concurrency = concurrency;
        this.featureQueue = new ArrayBlockingQueue<Feature>(Runtime.getRuntime().availableProcessors() * 2 + 1);
        JsonFactory factory = new JsonFactory();
        this.parser = factory.createParser(is);
    }
    
    @Override
    public void run() {
        // using a streamparser to be able to handle very large input files
        try {
            JsonToken token;
            while (!parser.isClosed() && (token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
                String name = parser.getCurrentName();
                
                if (JsonToken.FIELD_NAME.equals(token) && "type".equals(name)) {
                    parser.nextToken();
                    //System.out.println(parser.getText());
                    continue;
                }

                if (JsonToken.FIELD_NAME.equals(token) && "features".equals(name)) {
                    token = parser.nextToken(); if (!JsonToken.START_ARRAY.equals(token)) break;
                    while (!parser.isClosed() && (token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                        Feature feature = new Feature(parser);
                        //System.out.println(feature.toString());
                        try {
                            this.featureQueue.put(feature);
                        } catch (InterruptedException e) {
                        	Log.getLog().warn(e);
                        }
                    }
                }
            }
        } catch (IOException e) {
        	Log.getLog().warn(e);
        } finally {
            for (int i = 0; i < this.concurrency; i++) {
                try {this.featureQueue.put(POISON_FEATURE);} catch (InterruptedException e) {}
            }
        }
    }
    
    public Feature take() throws InterruptedException {
        return this.featureQueue.take();
    }
    
    public static class Feature {
        public String id = "", feature_type = "", geometry_type = "";
        public Map<String, String> properties = new HashMap<>();

        public Feature() {} // only used to create a POISON object
        
        public Feature(JsonParser parser) throws IOException {
            JsonToken token;
            while (!parser.isClosed() && (token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
                String name = parser.getCurrentName();
    
                if (JsonToken.FIELD_NAME.equals(token) && "id".equals(name)) {
                    parser.nextToken(); this.id = parser.getText(); continue;
                }
                if (JsonToken.FIELD_NAME.equals(token) && "type".equals(name)) {
                    parser.nextToken(); this.feature_type = parser.getText(); continue;
                }
                
                if (JsonToken.FIELD_NAME.equals(token) && "properties".equals(name)) {
                    this.properties = parseMap(parser);
                }
                
                if (JsonToken.FIELD_NAME.equals(token) && "geometry".equals(name)) {
                    token = parser.nextToken(); if (!JsonToken.START_OBJECT.equals(token)) break;
    
                    while (!parser.isClosed() && (token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
                        name = parser.getCurrentName();
    
                        if (JsonToken.FIELD_NAME.equals(token) && "type".equals(name)) {
                            parser.nextToken(); this.geometry_type = parser.getText(); continue;
                        }
                        if (JsonToken.FIELD_NAME.equals(token) && "coordinates".equals(name)) {
                            token = parser.nextToken(); if (!JsonToken.START_ARRAY.equals(token)) break;
                            parser.nextToken(); double lon = parser.getDoubleValue();
                            if (!properties.containsKey("geo_longitude")) properties.put("geo_longitude",  Double.toString(lon));
                            parser.nextToken(); double lat = parser.getDoubleValue();
                            if (!properties.containsKey("geo_latitude")) properties.put("geo_latitude",  Double.toString(lat));
                            parser.nextToken(); // END_ARRAY
                            continue;
                        }
                    }
                }
            }
        }
        
        public String toString() {
            return "id:" + id + "; lon:" + properties.get("geo_longitude") + "; lat:" + properties.get("geo_latitude");
        }
    }
    
    private static Map<String, String> parseMap(JsonParser parser) throws JsonParseException, IOException {
        Map<String, String> map = new HashMap<>();
        JsonToken token = parser.nextToken();
        if (!JsonToken.START_OBJECT.equals(token)) return map;
        
        while (!parser.isClosed() && (token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            token = parser.nextToken();
            String value = parser.getText();
            map.put(name.toLowerCase(), value);
        }
        return map;
    }

    public static void main(String[] args) {
        File f = new File(args[0]);
        try {
            GeoJsonReader reader = new GeoJsonReader(new BufferedInputStream(new FileInputStream(f)), Runtime.getRuntime().availableProcessors() * 2 + 1);
            new Thread(reader).start();
            Feature feature;
            while ((feature = reader.take()) != POISON_FEATURE) System.out.println(feature.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
