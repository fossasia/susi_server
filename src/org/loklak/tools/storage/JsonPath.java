/**
 *  JsonPath
 *  Copyright 13.03.2017 by Michael Peter Christen, @0rb1t3r
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonPath {

    /**
     * Very simple JSONPath decoder which always creates a JSONArray as result.
     * If the jsonPath does not point to an array, but to an object, the object is
     * placed within an array artificially. Therefore all kind of objects can be addressed.
     * Examples for JSONPaths:
     * $ - the whole json is the array
     * @param tokener contains the parsed JSON
     * @param jsonPath a path as defined by http://goessner.net/articles/JsonPath/
     * @return a JSONArray with the data part of a console query
     */
    public static JSONArray parse(JSONTokener tokener, String jsonPath) {
        try {
            if (tokener == null) return null;
            String[] dompath = jsonPath.split("\\.");
            if (dompath == null || dompath.length < 1 || !dompath[0].equals("$")) return null; // wrong syntax of jsonPath
            if (dompath.length == 1) {
                // the tokener contains already the data array
                return new JSONArray(tokener);
            }
            Object decomposition = null;
            for (int domc = 1; domc < dompath.length; domc++) {
                String path = dompath[domc];
                int p = path.indexOf('[');
                if (p < 0) {
                    decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path);
                } else if (p == 0) {
                    int idx = Integer.parseInt(path.substring(1, path.length() - 1));
                    decomposition = ((decomposition == null) ? new JSONArray(tokener) : ((JSONArray) decomposition)).get(idx);
                } else {
                    int idx = Integer.parseInt(path.substring(p + 1, path.length() - 1));
                    path = path.substring(0, p);
                    decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path);
                    decomposition = ((JSONArray) decomposition).get(idx);
                }
            }
            if (decomposition instanceof JSONArray) return (JSONArray) decomposition;
            if (decomposition instanceof JSONObject) {
                // enrich the decomposition with header/column entries
                // then we can access them with $h0$, $c0$, ... and so on
                // example source: http://api.loklak.org/api/search.json?q=fossasia&source=cache&count=0&fields=mentions,hashtags&limit=6
                String[] columns = ((JSONObject) decomposition).keySet().toArray(new String[((JSONObject) decomposition).length()]);
                int rowcount = 0;
                for (String column: columns) {
                    ((JSONObject) decomposition).put("k" + rowcount, column);
                    ((JSONObject) decomposition).put("v" + rowcount, ((JSONObject) decomposition).get(column));
                    rowcount++;
                }
                ((JSONObject) decomposition).put("mapsize", columns.length);
                return new JSONArray().put((JSONObject) decomposition);
            }
            if (decomposition instanceof String) return new JSONArray().put(new JSONObject().put("object", (String) decomposition));
            if (decomposition instanceof Integer) return new JSONArray().put(new JSONObject().put("object", decomposition.toString()));
            if (decomposition instanceof Double) return new JSONArray().put(new JSONObject().put("object", decomposition.toString()));
        } catch (JSONException e) {}
        return null;
    }
    
    private static void test(String json, String path) {
    	JSONTokener tokener = new JSONTokener(json.trim());
    	JSONArray array = parse(tokener, path);
    	System.out.println("json:" + json.toString());
    	System.out.println("path:" + path);
    	System.out.println("pars:" + (array == null ? "NULL" : array.toString()));
    }
    
    public static void main(String[] args) {
    	// https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20csv%20where%20url%3D%27http%3A%2F%2Fdownload.finance.yahoo.com%2Fd%2Fquotes.csv%3Fs%3DYHOO%26f%3Dsl1d1t1c1ohgv%26e%3D.csv%27%20and%20columns%3D%27symbol%2Cprice%2Cdate%2Ctime%2Cchange%2Ccol1%2Chigh%2Clow%2Ccol2%27&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys
    	test("{\"query\":{\"count\":1,\"created\":\"2017-03-13T12:41:55Z\",\"lang\":\"de-DE\",\"results\":{\"row\":{\"symbol\":\"YHOO\",\"price\":\"45.98\",\"date\":\"3/10/2017\",\"time\":\"5:00pm\",\"change\":\"+0.04\",\"col1\":\"46.20\",\"high\":\"46.20\",\"low\":\"45.75\",\"col2\":\"3824629\"}}}}",
    			"$.query.results.row");
    }
    
}
