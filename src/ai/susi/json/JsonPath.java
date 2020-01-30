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


package ai.susi.json;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonPath {
    
    public static JSONArray parse(byte[] b, String jsonPath) throws JSONException {
        JSONTokener tokener = new JSONTokener(new ByteArrayInputStream(b));
        try {
            JSONArray data = JsonPath.parse(tokener, jsonPath);
            return data;
        } catch (JSONException e) {
            if (jsonPath.equals("$")) {
                return new JSONArray().put(new JSONObject().put("!", new String(b, StandardCharsets.UTF_8)));
            } else {
                throw e;
            }
        }
    }
    
    private static JSONArray parse(JSONTokener tokener, String jsonPath) throws JSONException {
        JSONArray a = parseRaw(tokener, jsonPath);
        if (a.length() == 0) return a; // length == 1 will cause an empty thought. Its not wrong, it will just cause that thinking fails. May be wanted.
        Object f = a.get(0);
        if (a.length() == 1 && (!(f instanceof JSONObject))) {
            // wrap this atom into an object with key name "!"
            return new JSONArray().put(new JSONObject().put("!", a.get(0)));
        }
        // now, all objects in the array must be of the same type!
        Class<? extends Object> c = f.getClass();
        for (int i = 1; i < a.length(); i++) {
            if (a.get(i).getClass() != c) throw new JSONException("all objects in the result array must be of same type");
        }
        if (f instanceof JSONObject) return a;
        if (f instanceof JSONArray) throw new JSONException("the objects in the result array must not be an array");
        // the atomic objects must be wrapped
        JSONArray b = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            b.put(new JSONObject().put("!", a.get(i)));
        }
        return b;
    }

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
    private static JSONArray parseRaw(JSONTokener tokener, String jsonPath) throws JSONException {
        if (tokener == null) return null;
        String[] dompath = jsonPath.split("\\.");
        if (dompath == null || dompath.length < 1) return null; // wrong syntax of jsonPath
        if (dompath.length == 1) {
            if (dompath[0].equals("$")) return new JSONArray(tokener);
            if (dompath[0].length() > 1 && dompath[0].charAt(1) == '[' && dompath[0].charAt(dompath[0].length() - 1) == ']') {
                int pos = Integer.parseInt(dompath[0].substring(2, dompath[0].length() - 1));
                return new JSONArray(tokener).getJSONArray(pos);
            }
            return null;
        }
        Object decomposition = null;
        for (int domc = 1; domc < dompath.length; domc++) {
            String path = dompath[domc];
            int p = path.indexOf('[');
            int q = p < 0 ? -1 : path.indexOf(']', p + 1);
            if (p < 0) {
                decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path);
            } else if (p == 0) {
                int idx = Integer.parseInt(path.substring(1, q));
                decomposition = ((decomposition == null) ? new JSONArray(tokener) : ((JSONArray) decomposition)).get(idx);
            } else {
                int idx = Integer.parseInt(path.substring(p + 1, q));
                decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path.substring(0, p));
                decomposition = ((JSONArray) decomposition).get(idx);
                path = path.substring(q + 1);
                if (path.length() > 0 && path.charAt(0) == '[' && (q = path.indexOf(']')) > 0) {
                    // another dimension
                    idx = Integer.parseInt(path.substring(1, q));
                    decomposition = ((JSONArray) decomposition).get(idx);
                }
            }
        }
        if (decomposition instanceof JSONArray) return (JSONArray) decomposition;
        if (decomposition instanceof JSONObject) {
            // enrich the decomposition with header/column entries
            // then we can access them with $k0$, $v0$, ... and so on
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
        if (decomposition instanceof String) return new JSONArray().put(new JSONObject().put("!", (String) decomposition));
        if (decomposition instanceof Integer) return new JSONArray().put(new JSONObject().put("!", decomposition.toString()));
        if (decomposition instanceof Double) return new JSONArray().put(new JSONObject().put("!", decomposition.toString()));
        throw new JSONException("unrecognized object type: " + decomposition.getClass().getName());
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
