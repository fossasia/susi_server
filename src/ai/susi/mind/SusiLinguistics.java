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

package ai.susi.mind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class SusiLinguistics {

    private final static Map<SusiLanguage, Map<String,String>> synonyms = new ConcurrentHashMap<>(); // a map from a language to a map from a synonym to a canonical expression
    private final static Map<SusiLanguage, Map<String,String>> categories = new ConcurrentHashMap<>(); // a map from a language to a map from an expression to an associated category name
    private final static Map<SusiLanguage, Set<String>> filler = new ConcurrentHashMap<>(); // a map from a language to a set of words that can be ignored completely
    
    public static void learn(SusiLanguage language, JSONObject json) {

        // initialize temporary json objects
        JSONObject syn = json.has("synonyms") ? json.getJSONObject("synonyms") : new JSONObject();
        JSONArray fill = json.has("filler") ? json.getJSONArray("filler") : new JSONArray();
        JSONObject cat = json.has("categories") ? json.getJSONObject("categories") : new JSONObject();
        
        // check if synonyms exist
        if (!synonyms.containsKey(language)) synonyms.put(language, new ConcurrentHashMap<>());
        Map<String,String> synmap = synonyms.get(language);
        
        // add synonyms
        for (String canonical: syn.keySet()) {
            JSONArray a = syn.getJSONArray(canonical);
            a.forEach(synonym -> synmap.put(((String) synonym).toLowerCase(), canonical));
        }
        
        // check if filler exist
        if (!filler.containsKey(language)) filler.put(language, new HashSet<>());
        Set<String> fillset = filler.get(language);
        
        // add filler
        fill.forEach(word -> fillset.add((String) word));
        
        // check if language exist
        if (!categories.containsKey(language)) categories.put(language, new ConcurrentHashMap<>());
        Map<String, String> catforlang = categories.get(language);
        
        // add categories
        if (cat.length() > 0) {
            for (String canonical: cat.keySet()) {
                JSONArray a = cat.getJSONArray(canonical);
                a.forEach(synonym -> catforlang.put(((String) synonym).toLowerCase(), canonical));
            }
        }
    }

    public static class Token {
        public final String original, canonical, categorized;
        public Token(String original, String canonical, String categorized) {
            this.original = original;
            this.canonical = canonical;
            this.categorized = categorized;
        }
        public String toString() {
            return "{" +
                    "\"original\"=\"" + original + "\"," +
                    "\"canonical\"=\"" + canonical + "\"," +
                    "\"categorized\"=\"" + categorized + "\"" +
                    "}";
        }
    }

    public static Token tokenizeTerm(SusiLanguage language, String term) {
        language = language == null ? SusiLanguage.en : language;
        String original = term.toLowerCase();
        Map<String, String> synmap = synonyms.get(language);
        String s = synmap == null ? null : synmap.get(original);
        String canonical = s == null ? original : s;
        Map<String, String> catforlang = categories.get(language);
        String c = catforlang == null ? null : catforlang.get(canonical);
        String categorized = c == null ? canonical : c;
        return new Token(original, canonical, categorized);
    }

    public static List<Token> tokenizeSentence(SusiLanguage language, String term) {
        language = language == null ? SusiLanguage.en : language;
        List<Token> t = new ArrayList<>();
        term = term.replaceAll("\\?", " ?").replaceAll("\\!", " !").replaceAll("\\.", " .").replaceAll("\\,", " ,").replaceAll("\\;", " ;").replaceAll("\\:", " :").replaceAll("  ", " ");
        String[] u = term.split(" ");
        Set<String> fillset = filler.get(language);
        for (String v: u) {
            String original = v.toLowerCase();
            if (fillset != null && fillset.contains(original)) continue;
            t.add(tokenizeTerm(language, original));
        }
        return t;
    }

    public static JSONObject expandStarter(SusiLanguage language, String answer) {
        JSONObject json = new JSONObject(true);
        return json;
    }
    
    public static JSONObject expandStarter(SusiLanguage language, String answer, String subject) {
        JSONObject json = new JSONObject(true);
        return json;
    }
}
