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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.api.aggregation.ConsoleService;
import org.loklak.data.DAO;
import org.loklak.server.ClientIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SusiMind {
    
    private final Map<String, Set<SusiRule>> ruletrigger; // a map from a keyword to a set of actions
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
        this.logs = new SusiLog(watchpath, 5);
        try {observe();} catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getUnanswered() {
        return this.logs.getUnanswered();
    }
    
    public SusiMind observe() throws IOException {
        observe(this.initpath);
        observe(new File(this.initpath.getParentFile(), "aiml"));
        observe(this.watchpath);
        observe(new File(this.watchpath.getParentFile(), "aiml"));
        return this;
    }
    
    private void observe(File path) throws IOException {
        if (!path.exists()) return;
        for (File f: path.listFiles()) {
            if (!f.isDirectory() && !f.getName().startsWith(".") && (f.getName().endsWith(".json") || f.getName().endsWith(".txt") || f.getName().endsWith(".aiml"))) {
                if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                    observations.put(f, System.currentTimeMillis());
                    try {
                        JSONObject lesson = new JSONObject();
                        if (f.getName().endsWith(".json")) {
                            lesson = readJsonLesson(f);
                        }
                        if (f.getName().endsWith(".txt")) {
                            lesson = readTextLesson(f);
                        }
                        if (f.getName().endsWith(".aiml")) {
                            lesson = readAIMLLesson(f);
                        }
                        learn(lesson);
                    } catch (Throwable e) {
                        DAO.severe("BAD JSON FILE: " + f.getAbsolutePath() + ", " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //this.ruletrigger.forEach((term, map) -> System.out.println("***DEBUG trigger " + term + " -> " + map.toString()));
    }

    public JSONObject readJsonLesson(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return json;
    }
    
    public JSONObject readTextLesson(File file) throws JSONException, FileNotFoundException {
        // read the text file and turn it into a rule json; then learn that
        BufferedReader br = new BufferedReader(new FileReader(file));
        JSONObject json = new JSONObject();
        JSONArray rules = new JSONArray();
        json.put("rules", rules);
        String lastLine = "", line = "";
        boolean prior = false;
        try {readloop: while ((line = br.readLine()) != null) {
            line = line.trim();
            
            // read metadata
            if (line.startsWith("::")) {
                line = line.toLowerCase();
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                lastLine = "";
                continue readloop;
            }
            
            if (line.startsWith("#")) {
                lastLine = "";
                continue readloop;
            }
            
            // read content
            if (line.length() > 0 && lastLine.length() > 0) {
                // mid of conversation (last answer is query for next rule)
                JSONObject rule = SusiRule.simpleRule(lastLine.split("\\|"), line.split("\\|"), prior);
                //System.out.println(rule.toString());
                rules.put(rule);
            }
            lastLine = line;
        }} catch (IOException e) {}
        return json;
    }
    
    public JSONObject readAIMLLesson(File file) throws Exception {
        // read the file as string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        StringBuilder buf=new StringBuilder();
        while ((str = br.readLine()) != null) buf.append(str);
        br.close();
        
        // parse the string as xml into a node object
        InputStream is = new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();
        Node root = doc.getDocumentElement();
        Node node = root;
        NodeList nl = node.getChildNodes();
        JSONObject json = new JSONObject();
        JSONArray rules = new JSONArray();
        json.put("rules", rules);
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                JSONObject rule = readAIMLCategory(nl.item(i));
                if (rule != null && rule.length() > 0) rules.put(rule);
            }
            System.out.println("ROOT NODE " + nl.item(i).getNodeName());
        }
        return json;
    }
    
    public JSONObject readAIMLCategory(Node category) {
        NodeList nl = category.getChildNodes();
        String[] phrases = null;
        String[] answers = null;
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            System.out.println("CATEGORYY NODE " + nl.item(i).getNodeName());
            if (nodename.equals("pattern")) {
                phrases = readAIMLSentences(nl.item(i));
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                answers = readAIMLSentences(nl.item(i));
            }
        }
        if (phrases != null && answers != null) {
            return SusiRule.simpleRule(phrases, answers, false);
        }
        return null;
    }
    
    public String[] readAIMLSentences(Node pot) {
        NodeList nl = pot.getChildNodes();
        JSONObject json = new JSONObject();
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            System.out.println("SENTENCE NODE " + nl.item(i).getNodeName());
            if (nodename.equals("pattern")) {
                
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                
            }
        }
        return null;
    }
    
    public SusiMind learn(JSONObject json) {

        // teach the language parser
        this.reader.learn(json);

        // add console rules
        JSONObject consoleServices = json.has("console") ? json.getJSONObject("console") : new JSONObject();
        consoleServices.keySet().forEach(console -> {
            JSONObject service = consoleServices.getJSONObject(console);
            if (service.has("url") && service.has("data") && service.has("parser")) {
                String url = service.getString("url");
                String data = service.getString("data");
                String parser = service.getString("parser");
                if (parser.equals("json")) {
                    ConsoleService.addGenericConsole(console, url, data);
                }
            }
        });

        // add conversation rules
        JSONArray rules = json.has("rules") ? json.getJSONArray("rules") : new JSONArray();
        rules.forEach(j -> {
            SusiRule rule = new SusiRule((JSONObject) j);
            rule.getKeys().forEach(key -> {
                    Set<SusiRule> l = this.ruletrigger.get(key);
                    if (l == null) {
                        l = new HashSet<>();
                        this.ruletrigger.put(key, l);
                    }
                    l.add(rule);
                    rule.getPhrases().forEach(phrase -> this.logs.removeUnanswered(phrase.getPattern()));
                    //System.out.println("***DEBUG: ADD RULE FOR KEY " + key + ": " + rule.toString());
                });
            });
        
        return this;
    }
    
    /**
     * extract the mind system from the ruletrigger
     * @return
     */
    public JSONObject getMind() {
        JSONObject mind = new JSONObject(true);
        this.ruletrigger.forEach((key, rulemap) -> {
            JSONArray rules = new JSONArray();
            mind.put(key, rules);
            rulemap.forEach(rule -> {
                JSONObject r = new JSONObject(true);
                r.putAll(rule.toJSON());
                r.put("hash", rule.hashCode());
                rules.put(r);
            });
        });
        return mind;
    }
    
    /**
     * This is the core principle of creativity: being able to match a given input
     * with problem-solving knowledge.
     * This method finds ideas (with a query instantiated rules) for a given query.
     * The rules are selected using a scoring system and pattern matching with the query.
     * Not only the most recent user query is considered for rule selection but also
     * previously requested queries and their answers to be able to set new rule selections
     * in the context of the previous conversation.
     * @param query the user input
     * @param previous_argument the latest conversation with the same user
     * @param maxcount the maximum number of ideas to return
     * @return an ordered list of ideas, first idea should be considered first.
     */
    public List<SusiIdea> creativity(String query, SusiThought latest_thought, int maxcount) {
        // tokenize query to have hint for idea collection
        final List<SusiIdea> ideas = new ArrayList<>();
        this.reader.tokenizeSentence(query).forEach(token -> {
            Set<SusiRule> rule_for_category = this.ruletrigger.get(token.categorized);
            Set<SusiRule> rule_for_original = token.original.equals(token.categorized) ? null : this.ruletrigger.get(token.original);
            Set<SusiRule> r = new HashSet<>();
            if (rule_for_category != null) r.addAll(rule_for_category);
            if (rule_for_original != null) r.addAll(rule_for_original);
            r.forEach(rule -> ideas.add(new SusiIdea(rule).setIntent(token)));
        });
        
        //for (SusiIdea idea: ideas) System.out.println("idea.phrase-1:" + idea.getRule().getPhrases().toString());
        
        // add catchall rules always (those are the 'bad ideas')
        Collection<SusiRule> ca = this.ruletrigger.get(SusiRule.CATCHALL_KEY);
        if (ca != null) ca.forEach(rule -> ideas.add(new SusiIdea(rule)));
        
        // create list of all ideas that might apply
        TreeMap<Long, List<SusiIdea>> scored = new TreeMap<>();
        AtomicLong count = new AtomicLong(0);
        ideas.forEach(idea -> {
            int score = idea.getRule().getScore();
            long orderkey = Long.MAX_VALUE - ((long) score) * 1000L + count.incrementAndGet();
            List<SusiIdea> r = scored.get(orderkey);
            if (r == null) {r = new ArrayList<>(); scored.put(orderkey, r);}
            r.add(idea);
        });

        // make a sorted list of all ideas
        ideas.clear(); scored.values().forEach(r -> ideas.addAll(r));
        
        //for (SusiIdea idea: ideas) System.out.println("idea.phrase-2: score=" + idea.getRule().getScore() + " : " + idea.getRule().getPhrases().toString());
        
        // test ideas and collect those which match up to maxcount
        List<SusiIdea> plausibleIdeas = new ArrayList<>(Math.min(10, maxcount));
        for (SusiIdea idea: ideas) {
            SusiRule rule = idea.getRule();
            Collection<Matcher> m = rule.matcher(query);
            if (m.isEmpty()) continue;
            // TODO: evaluate leading SEE flow commands right here as well
            plausibleIdeas.add(idea);
            if (plausibleIdeas.size() >= maxcount) break;
        }
        
        for (SusiIdea idea: plausibleIdeas) System.out.println("idea.phrase-3: score=" + idea.getRule().getScore() + " : " + idea.getRule().getPhrases().toString());
        
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
    public List<SusiArgument> react(String query, int maxcount, String client) {
        // get the history a list of thoughts
        SusiArgument latest_argument = new SusiArgument();
        ArrayList<SusiInteraction> interactions = this.logs.getInteractions(client);
        interactions.forEach(action -> latest_argument.think(action.recallDispute()));
        // perform a mindmeld to create a single thought out of the recalled argument
        // the mindmeld will squash the latest thoughts into one so it does not pile up to exponential growth
        SusiThought recall = latest_argument.mindmeld(false);
        
        // normalize the query
        query = SusiPhrase.normalizeExpression(query);
        
        // find an answer
        List<SusiArgument> answers = new ArrayList<>();
        List<SusiIdea> ideas = creativity(query, recall, 100);
        for (SusiIdea idea: ideas) {
            SusiArgument argument = idea.getRule().consideration(query, recall, idea.getIntent(), this, client);
            if (argument != null) answers.add(argument);
            if (answers.size() >= maxcount) break;
        }
        return answers;
    }
    
    public String react(String query) {
        String client = "host_localhost";
        List<SusiArgument> datalist = react(query, 1, client);
        SusiArgument bestargument = datalist.get(0);
        return bestargument.getActions().get(0).apply(bestargument, this, client).getStringAttr("expression");
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
            JSONObject lesson = mem.readJsonLesson(new File("conf/susi/susi_cognition_000.json"));
            mem.learn(lesson);
            System.out.println(mem.react("I feel funny"));
            System.out.println(mem.react("Help me!"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
