/**
 *  SusiAction
 *  Copyright 29.06.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

public class SusiAction {

    public static enum ActionType {answer, table, piechart;}
    public static enum ActionAnswerType {random, roundrobin;}
    
    private final static Random random = new Random(System.currentTimeMillis());
    
    private JSONObject json;

    public SusiAction(JSONObject json) {
        this.json = json;
    }
    public ActionType getType() {
        return this.json.has("type") ? ActionType.valueOf(this.json.getString("type")) : null;
    }
    public ArrayList<String> getPhrases() {
        ArrayList<String> a = new ArrayList<>();
        if (!this.json.has("phrases")) return a;
        this.json.getJSONArray("phrases").forEach(p -> a.add((String) p));
        return a;
    }
    public String getStringAttr(String attr) {
        return this.json.has(attr) ? this.json.getString(attr) : "";
    }
    public int getIntAttr(String attr) {
        return this.json.has(attr) ? this.json.getInt(attr) : 0;
    }
    public SusiAction apply(SusiData data) {
        if (this.getType() == ActionType.answer && this.json.has("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = getPhrases();
            String phrase = a.get(random.nextInt(a.size()));
            this.json.put("expression", apply(phrase, data));
        }
        return this;
    }
    private String apply(String p, SusiData data) {
        JSONArray table = data.getData();
        if (table != null && table.length() > 0) {
            JSONObject row = table.getJSONObject(0);
            for (String key: row.keySet()) {
                int i = p.indexOf("$" + key + "$");
                if (i >= 0) {
                    p = p.substring(0, i) + row.get(key).toString() + p.substring(i + key.length() + 2);
                }
            }
        }
        return p;
    }
    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key))); // make a clone
        if (j.has("expression")) {
            j.remove("phrases");
            j.remove("select");
            this.json.remove("expression"); // thats a bad hack, TODO: better concurrency
        }
        return j;
    }
}
