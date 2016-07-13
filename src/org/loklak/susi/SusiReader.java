/**
 *  SusiReader
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class SusiReader {

    private final Map<String,String> synonyms; // a map from a synonym to a canonical expression
    private final Map<String,String> categories; // a map from an expression to an associated category name
    private final Set<String> filler; // a set of words that can be ignored completely
    
    public SusiReader() {
        this.synonyms = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.filler = new HashSet<>();
    }
    
    public SusiReader learn(JSONObject json) {

        // initialize temporary json objects
        JSONObject syn = json.has("synonyms") ? json.getJSONObject("synonyms") : new JSONObject();
        JSONArray fill = json.has("filler") ? json.getJSONArray("filler") : new JSONArray();
        JSONObject cat = json.has("categories") ? json.getJSONObject("categories") : new JSONObject();
        
        // add synonyms
        for (String canonical: syn.keySet()) {
            JSONArray a = syn.getJSONArray(canonical);
            a.forEach(synonym -> synonyms.put(((String) synonym).toLowerCase(), canonical));
        }
        
        // add filler
        fill.forEach(word -> filler.add((String) word));
        
        // add categories
        for (String canonical: cat.keySet()) {
            JSONArray a = cat.getJSONArray(canonical);
            a.forEach(synonym -> categories.put(((String) synonym).toLowerCase(), canonical));
        }
        
        return this;
    }

    public static class Token {
        public final String original, canonical, categorized;
        public Token(String original, String canonical, String categorized) {
            this.original = original;
            this.canonical = canonical;
            this.categorized = categorized;
        }
    }

    public List<Token> tokenize(String query) {
        List<Token> t = new ArrayList<>();
        query = query.replaceAll("\\?", " ?").replaceAll("\\!", " !").replaceAll("\\.", " .").replaceAll("\\,", " ,").replaceAll("\\;", " ;").replaceAll("\\:", " :").replaceAll("  ", " ");
        String[] u = query.split(" ");
        for (String v: u) {
            String original = v.toLowerCase();
            if (this.filler.contains(original)) continue;
            String s = this.synonyms.get(original);
            String canonical = s == null ? original : s;
            String c = this.categories.get(canonical);
            String categorized = c == null ? canonical : c;
            t.add(new Token(original, canonical, categorized));
        }
        return t;
    }
}
