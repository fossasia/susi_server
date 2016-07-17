/**
 *  SusiTransfer
 *  Copyright 14.07.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.susi;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * Transfer is the ability to perceive a given thought in a different representation
 * in such a way that it applies on a skill or a skill set.
 */
public class SusiTransfer {
    
    private LinkedHashMap<String, String> selectionMapping;
    
    /**
     * Create a new transfer. The mapping must be given in the same way as SQL column selection
     * statements. The selection of sub-objects of json object can be done using dot-notion.
     * Arrays can be accessed using brackets '[' and ']'. An example is:
     * mapping = "location.lon AS longitude, location.lat AS latitude"
     * or
     * mapping = "names[0] AS firstname"
     * As a reference, the mappingExpression shall be superset of a list of
     * https://mariadb.com/kb/en/mariadb/select/#select-expressions
     * @param mapping
     */
    public SusiTransfer(String mappingExpression) {
        this.selectionMapping = parse(mappingExpression);
    }

    /**
     * get the set of transfer keys
     * @return
     */
    public Set<String> keys() {
        return this.selectionMapping.keySet();
    }
    
    /**
     * transfer mappings can be used to extract specific information from a json object to
     * create a new json object. In the context of Susi this is applied on choices from thought data
     * @param choice one 'row' of a SusiThought data array
     * @return a choice where the elements of the given choice are extracted according to the given mapping
     */
    public JSONObject extract(JSONObject choice) {
        if (this.selectionMapping == null) return choice;
        JSONObject json = new JSONObject(true);
        for (Map.Entry<String, String> c: selectionMapping.entrySet()) {
            String key = c.getKey();
            int p = key.indexOf('.');
            if (p > 0) {
                // sub-element
                String k0 = key.substring(0,  p);
                String k1 = key.substring(p + 1);
                if (choice.has(k0)) {
                    if (k1.equals("length") || k1.equals("size()")) {
                        Object a = choice.get(k0);
                        if (a instanceof String[]) {
                            json.put(c.getValue(),((String[]) a).length);
                        } else if (a instanceof JSONArray) {
                            json.put(c.getValue(),((JSONArray) a).length());
                        }
                    } else {
                        JSONObject o = choice.getJSONObject(k0);
                        if (o.has(k1)) json.put(c.getValue(), o.get(k1));
                    }
                }
            } else if ((p = key.indexOf('[')) > 0) {
                // array
                int q = key.indexOf("]", p);
                if (q > 0) {
                    String k0 = key.substring(0,  p);
                    int i = Integer.parseInt(key.substring(p + 1, q));
                    if (choice.has(k0)) {
                        JSONArray a = choice.getJSONArray(k0);
                        if (i < a.length()) json.put(c.getValue(), a.get(i));
                    }
                }
            } else {
                // flat
                if (choice.has(key)) json.put(c.getValue(), choice.get(key));
            }
        }
        return json;
    }
    
    /**
     * A conclusion from choices is done by the application of a function on the choice set.
     * This may be done by i.e. counting the number of choices or extracting a maximum element.
     * @param choices the given set of json objects from the data object of a SusiThought
     * @returnan array of json objects which are the extraction of given choices according to the given mapping
     */
    public JSONArray conclude(JSONArray choices) {
        JSONArray a = new JSONArray();
        if (this.selectionMapping != null && this.selectionMapping.size() == 1) {
            // test if this has an aggregation key: AVG, COUNT, MAX, MIN, SUM
            final String aggregator = this.selectionMapping.keySet().iterator().next();
            final String aggregator_as = this.selectionMapping.get(aggregator);
            if (aggregator.startsWith("COUNT(") && aggregator.endsWith(")")) { // TODO: there should be a special pattern for this to make it more efficient
                return a.put(new JSONObject().put(aggregator_as, choices.length()));
            }
            if (aggregator.startsWith("MAX(") && aggregator.endsWith(")")) {
                final AtomicDouble max = new AtomicDouble(Double.MIN_VALUE); String c = aggregator.substring(4, aggregator.length() - 1);
                choices.forEach(json -> max.set(Math.max(max.get(), ((JSONObject) json).getDouble(c))));
                return a.put(new JSONObject().put(aggregator_as, max.get()));
            }
            if (aggregator.startsWith("MIN(") && aggregator.endsWith(")")) {
                final AtomicDouble min = new AtomicDouble(Double.MAX_VALUE); String c = aggregator.substring(4, aggregator.length() - 1);
                choices.forEach(json -> min.set(Math.min(min.get(), ((JSONObject) json).getDouble(c))));
                return a.put(new JSONObject().put(aggregator_as, min.get()));
            }
            if (aggregator.startsWith("SUM(") && aggregator.endsWith(")")) {
                final AtomicDouble sum = new AtomicDouble(0.0d); String c = aggregator.substring(4, aggregator.length() - 1);
                choices.forEach(json -> sum.addAndGet(((JSONObject) json).getDouble(c)));
                return a.put(new JSONObject().put(aggregator_as, sum.get()));
            }
            if (aggregator.startsWith("AVG(") && aggregator.endsWith(")")) {
                final AtomicDouble sum = new AtomicDouble(0.0d); String c = aggregator.substring(4, aggregator.length() - 1);
                choices.forEach(json -> sum.addAndGet(((JSONObject) json).getDouble(c)));
                return a.put(new JSONObject().put(aggregator_as, sum.get() / choices.length()));
            }
        }
        if (this.selectionMapping != null && this.selectionMapping.size() == 2) {
            Iterator<String> ci = this.selectionMapping.keySet().iterator();
            String aggregator = ci.next(); String column = ci.next();
            if (column.indexOf('(') >= 0) {String s = aggregator; aggregator = column; column = s;}
            final String aggregator_as = this.selectionMapping.get(aggregator);
            final String column_as = this.selectionMapping.get(column);
            final String column_final = column;
            if (aggregator.startsWith("PERCENT(") && aggregator.endsWith(")")) {
                final AtomicDouble sum = new AtomicDouble(0.0d); String c = aggregator.substring(8, aggregator.length() - 1);
                choices.forEach(json -> sum.addAndGet(((JSONObject) json).getDouble(c)));
                choices.forEach(json -> a.put(new JSONObject()
                        .put(aggregator_as, 100.0d * ((JSONObject) json).getDouble(c) / sum.get())
                        .put(column_as, ((JSONObject) json).get(column_final))));
                return a;
            }
        }
        for (Object json: choices) a.put(this.extract((JSONObject) json));
        return a;
    }
    
    private static LinkedHashMap<String, String> parse(String mapping) {
        LinkedHashMap<String, String> columns;
        String[] column_list = mapping.trim().split(",");
        if (column_list.length == 1 && column_list[0].equals("*")) {
            columns = null;
        } else {
            columns = new LinkedHashMap<>();
            for (String column: column_list) {
                String c = column.trim();
                int p = c.indexOf(" AS ");
                if (p < 0) {
                    c = trimQuotes(c);
                    columns.put(c, c);
                } else {
                    columns.put(trimQuotes(c.substring(0,  p).trim()), trimQuotes(c.substring(p + 4).trim()));
                }
            }
        }
        return columns;
    }

    private static String trimQuotes(String s) {
        if (s.length() == 0) return s;
        if (s.charAt(0) == '\'' || s.charAt(0) == '\"') s = s.substring(1);
        if (s.charAt(s.length() - 1) == '\'' || s.charAt(s.length() - 1) == '\"') s = s.substring(0, s.length() - 1);
        return s;
    }
    
    public String toString() {
        return this.selectionMapping == null ? "NULL" : this.selectionMapping.toString();
    }
}
