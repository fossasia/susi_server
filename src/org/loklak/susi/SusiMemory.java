/**
 *  SusiMemory
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.data.DAO;

public class SusiMemory {
    
    private final Map<String,String> synonyms; // a map from a synonym to a canonical expression
    private final Map<String,String> categories; // a map from an expression to an associated category name
    private final Set<String> filler; // a set of words that can be ignored completely
    private final Map<String, List<SusiRule>> ruletrigger; // a map from a keyword to a list of actions
    private final File watchpath; // a path where the memory looks for new additions of knowledge with memory files
    private final Map<File, Long> observations; // a mapping of mind memory files to the time when the file was read the last time
    
    public SusiMemory(File watchpath) {
        // initialize class objects
        this.watchpath = watchpath;
        this.watchpath.mkdirs();
        this.synonyms = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.filler = new HashSet<>();
        this.ruletrigger = new ConcurrentHashMap<>();
        this.observations = new HashMap<>();
    }
    
    public SusiMemory observe() throws IOException {
        for (File f: this.watchpath.listFiles()) {
            if (!f.isDirectory() && f.getName().endsWith(".json")) {
                if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                    observations.put(f, System.currentTimeMillis());
                    try {
                        add(f);
                    } catch (Throwable e) {
                        DAO.log(e.getMessage());
                    }
                }
            }
        }
        return this;
    }
    
    public SusiMemory add(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return add(json);
    }
    
    public SusiMemory add(JSONObject json) {

        // initialize temporary json objects
        JSONObject syn = json.has("synonyms") ? json.getJSONObject("synonyms") : new JSONObject();
        JSONArray fill = json.has("filler") ? json.getJSONArray("filler") : new JSONArray();
        JSONObject cat = json.has("categories") ? json.getJSONObject("categories") : new JSONObject();
        JSONArray rules = json.has("rules") ? json.getJSONArray("rules") : new JSONArray();
        
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
        
        // add rules
        rules.forEach(j -> {
            SusiRule rule = new SusiRule((JSONObject) j);
            rule.getKeys().forEach(key -> {
                    List<SusiRule> l = this.ruletrigger.get(key);
                    if (l == null) {l = new ArrayList<>(); this.ruletrigger.put(key, l);}
                    l.add(rule); 
                });
            });
        
        return this;
    }
    
    public List<SusiRule> getRules(String query, int maxcount) {
        // tokenize query to have hint for rule collection
        List<SusiRule> rules = new ArrayList<>();
        token(query).forEach(token -> {List<SusiRule> r = this.ruletrigger.get(token); if (r != null) rules.addAll(r);});

        // add catchall rules always
        List<SusiRule> ca = this.ruletrigger.get("*"); if (ca != null) rules.addAll(ca);
        
        // create list of all rules that might apply
        TreeMap<Integer, List<SusiRule>> scored = new TreeMap<>();
        rules.forEach(rule -> {
            int score = rule.getScore();
            List<SusiRule> r = scored.get(score);
            if (r == null) {r = new ArrayList<>(); scored.put(-score, r);}
            r.add(rule);
        });

        // make a sorted list of all rules
        rules.clear(); scored.values().forEach(r -> rules.addAll(r));
        
        // test rules and collect those which match up to maxcount
        List<SusiRule> mrules = new ArrayList<>(Math.min(10, maxcount));
        for (SusiRule rule: rules) {
            Matcher m = rule.matcher(query);
            if (m == null) continue;
            mrules.add(rule);
            if (mrules.size() >= maxcount) break;
        }
        return mrules;
    }

    private List<String> token(String query) {
        List<String> t = new ArrayList<>();
        query = query.replaceAll("\\?", " ?").replaceAll("\\!", " !").replaceAll("\\.", " .").replaceAll("\\,", " ,").replaceAll("\\;", " ;").replaceAll("\\:", " :").replaceAll("  ", " ");
        String[] u = query.split(" ");
        for (String v: u) {
            String vl = v.toLowerCase();
            if (this.filler.contains(vl)) continue;
            String s = this.synonyms.get(vl);
            if (s != null) vl = s;
            String c = this.categories.get(vl);
            if (c != null) vl = c;
            t.add(vl);
        }
        return t;
    }
    
    public List<SusiArgument> answer(final String query, int maxcount) {
        List<SusiRule> rules = getRules(query, maxcount);
        return rules.stream().map(rule -> rule.consideration(query)).collect(Collectors.toList());
    }
    
    public String answer(String query) {
        List<SusiArgument> datalist = answer(query, 1);
        SusiArgument bestargument = datalist.get(0);
        return bestargument.mindstate().getActions().get(0).apply(bestargument).getStringAttr("expression");
    }
    
    public static void main(String[] args) {
        try {
            SusiMemory mem = new SusiMemory(new File(new File("data"), "susi"));
            mem.add(new File("conf/susi/susi_cognition_000.json"));
            //System.out.println(mem.answer("who will win euro2016?", 3));
            System.out.println(mem.answer("I feel funny"));
            System.out.println(mem.answer("Help me!"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
