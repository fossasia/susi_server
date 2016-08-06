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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.data.DAO;
import org.loklak.server.ClientIdentity;

public class SusiMind {
    
    private final Map<String, Map<Long, SusiRule>> ruletrigger; // a map from a keyword to a list of actions
    private final File initpath, watchpath; // a path where the memory looks for new additions of knowledge with memory files
    private final Map<File, Long> observations; // a mapping of mind memory files to the time when the file was read the last time
    private final SusiReader reader; // responsible to understand written communication
    private final SusiLog logs; // conversation logs
    
    public SusiMind(File initpath, File watchpath) {
        // initialize class objects
        this.initpath = initpath;
        this.initpath.mkdirs();
        this.watchpath = watchpath;
        this.watchpath.mkdirs();
        this.ruletrigger = new ConcurrentHashMap<>();
        this.observations = new HashMap<>();
        this.reader = new SusiReader();
        this.logs = new SusiLog(watchpath, 3);
    }

    public SusiMind observe() throws IOException {
        observe(this.initpath);
        observe(this.watchpath);
        return this;
    }
    
    private void observe(File path) throws IOException {
        for (File f: path.listFiles()) {
            if (!f.isDirectory() && f.getName().endsWith(".json")) {
                if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                    observations.put(f, System.currentTimeMillis());
                    try {
                        learn(f);
                    } catch (Throwable e) {
                        DAO.severe(e.getMessage());
                    }
                }
            }
        }
    }
    
    public SusiMind learn(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return learn(json);
    }
    
    public SusiMind learn(JSONObject json) {

        // teach the language parser
        this.reader.learn(json);
        
        // initialize temporary json object
        JSONArray rules = json.has("rules") ? json.getJSONArray("rules") : new JSONArray();
        
        // add rules
        rules.forEach(j -> {
            SusiRule rule = new SusiRule((JSONObject) j);
            rule.getKeys().forEach(key -> {
                    Map<Long, SusiRule> l = this.ruletrigger.get(key);
                    if (l == null) {l = new HashMap<>(); this.ruletrigger.put(key, l);}
                    l.put(rule.getID(), rule); 
                });
            });
        
        return this;
    }
    
    public List<SusiIdea> associate(String query, SusiArgument previous_argument, int maxcount) {
        // tokenize query to have hint for idea collection
        final List<SusiIdea> ideas = new ArrayList<>();
        this.reader.tokenize(query).forEach(token -> {
            Map<Long, SusiRule> r = this.ruletrigger.get(token.categorized);
            if (r != null) {
                r.values().forEach(rule -> ideas.add(new SusiIdea(rule).setIntent(token)));
            }
        });

        // add catchall rules always (those are the 'bad ideas')
        Collection<SusiRule> ca = this.ruletrigger.get(SusiRule.CATCHALL_KEY).values();
        if (ca != null) ca.forEach(rule -> ideas.add(new SusiIdea(rule)));
        
        // create list of all ideas that might apply
        TreeMap<Integer, List<SusiIdea>> scored = new TreeMap<>();
        ideas.forEach(idea -> {
            //System.out.println("idea.phrase-1:" + idea.getRule().getPhrases().toString());
            int score = idea.getRule().getScore();
            List<SusiIdea> r = scored.get(-score);
            if (r == null) {r = new ArrayList<>(); scored.put(-score, r);}
            r.add(idea);
        });

        // make a sorted list of all ideas
        ideas.clear(); scored.values().forEach(r -> ideas.addAll(r));
        
        // test ideas and collect those which match up to maxcount
        List<SusiIdea> plausibleIdeas = new ArrayList<>(Math.min(10, maxcount));
        for (SusiIdea idea: ideas) {
            //System.out.println("idea.phrase-2:" + idea.getRule().getPhrases().toString());
            SusiRule rule = idea.getRule();
            if (rule.getActions().size() == 0) continue;
            if (rule.getActions().get(0).getPhrases().size() == 0) continue;
            if (rule.getActions().get(0).getPhrases().get(0).length() == 0) continue;
            Matcher m = rule.matcher(query);
            if (m == null) continue;
            plausibleIdeas.add(idea);
            if (plausibleIdeas.size() >= maxcount) break;
        }
        return plausibleIdeas;
    }
    
    
    /**
     * react on a user input: this causes the selection of deduction rules and the evaluation of the process steps
     * in every rule up to the moment where enough rules have been applied as consideration. The reaction may also
     * cause the evaluation of operational steps which may cause learning effects within the SusiMind.
     * @param query
     * @param maxcount
     * @return
     */
    public List<SusiArgument> react(final String query, int maxcount, String client) {
        List<SusiInteraction> previous_interactions = this.logs.getInteractions(client); // first entry is latest interaction
        SusiArgument latest_argument = new SusiArgument();
        for (int i = previous_interactions.size() - 1; i >= 0; i--) {
            latest_argument.think(previous_interactions.get(i).recallDispute());
        }
        List<SusiArgument> answers = new ArrayList<>();
        List<SusiIdea> ideas = associate(query, latest_argument, 100);
        for (SusiIdea idea: ideas) {
            SusiArgument argument = idea.getRule().consideration(query, latest_argument, idea.getIntent());
            if (argument != null) answers.add(argument);
            if (answers.size() >= maxcount) break;
        }
        return answers;
    }
    
    public String react(String query) {
        List<SusiArgument> datalist = react(query, 1, "host_localhost");
        SusiArgument bestargument = datalist.get(0);
        return bestargument.getActions().get(0).apply(bestargument).getStringAttr("expression");
    }
    
    public SusiInteraction interaction(final String query, int maxcount, ClientIdentity identity) {
        // get a response from susis mind
        String client = identity.getType() + "_" + identity.getName();
        SusiInteraction si = new SusiInteraction(query, maxcount, client, this);
        // write a log about the response using the users identity
        this.logs.addInteraction(client, si);
        // return the computed response
        return si;
    }
    
    public static void main(String[] args) {
        try {
            File init = new File(new File("conf"), "susi");
            File watch = new File(new File("data"), "susi");
            SusiMind mem = new SusiMind(init, watch);
            mem.learn(new File("conf/susi/susi_cognition_000.json"));
            System.out.println(mem.react("I feel funny"));
            System.out.println(mem.react("Help me!"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
