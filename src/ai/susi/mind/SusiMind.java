/**
 *  SusiMind
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
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.susi.DAO;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiInference.Type;
import ai.susi.mind.SusiMemory.TokenMapList;
import ai.susi.server.api.susi.ConsoleService;

public class SusiMind {
    
    public final static int ATTENTION_TIME = 5;
    
    private final Map<String, Set<SusiSkill>> skilltrigger; // a map from a keyword to a set of skills
    private final File[] watchpaths;
    private final File memorypath; // a path where the memory looks for new additions of knowledge with memory files
    private final Map<File, Long> observations; // a mapping of mind memory files to the time when the file was read the last time
    private final SusiReader reader; // responsible to understand written communication
    private final SusiMemory memories; // conversation logs are memories
    
    public SusiMind(File memorypath, File... watchpaths) {
        // initialize class objects
        this.watchpaths = watchpaths;
        for (int i = 0; i < watchpaths.length; i++) {
            if (watchpaths[i] != null) watchpaths[i].mkdirs();
        }
        this.memorypath = memorypath;
        if (this.memorypath != null) this.memorypath.mkdirs();
        this.skilltrigger = new ConcurrentHashMap<>();
        this.observations = new HashMap<>();
        this.reader = new SusiReader();
        this.memories = new SusiMemory(memorypath, ATTENTION_TIME);
        try {observe();} catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SusiMemory getMemories() {
        return this.memories;
    }
    
    public SusiReader getReader() {
        return this.reader;
    }
    
    public Map<String, Integer> getUnanswered() {
        return this.memories.getUnanswered();
    }
    
    public List<TokenMapList> unanswered2tokenizedstats() {
        return this.memories.unanswered2tokenizedstats();
    }
    
    public SusiMind observe() throws IOException {
        for (int i = 0; i < watchpaths.length; i++) {
            observe(watchpaths[i]);
        }
        return this;
    }
    
    private void observe(File path) throws IOException {
        if (!path.exists()) return;
        for (File f: path.listFiles()) {
            if (f.isDirectory()) {
                // recursively step into it
                observe(f);
            }
            if (!f.isDirectory() && !f.getName().startsWith(".") && (f.getName().endsWith(".json") || f.getName().endsWith(".txt") || f.getName().endsWith(".aiml"))) {
                if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                    observations.put(f, System.currentTimeMillis());
                    try {
                        JSONObject lesson = new JSONObject();
                        if (f.getName().endsWith(".json")) {
                            lesson = readJsonLesson(f);
                        }
                        if (f.getName().endsWith(".txt") || f.getName().endsWith(".ezd")) {
                            lesson = readSkills(new BufferedReader(new FileReader(f)));
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
        
        //this.skilltrigger.forEach((term, map) -> System.out.println("***DEBUG trigger " + term + " -> " + map.toString()));
    }

    public JSONObject readJsonLesson(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return json;
    }
    
    /**
     * read an "EzD" ('Easy Dialog') file: this is just a text file. Read the docs/susi_skill_development_tutorial.md for an explanation
     * @param br
     * @return
     * @throws JSONException
     * @throws FileNotFoundException
     */
    public JSONObject readSkills(BufferedReader br) throws JSONException {
        // read the text file and turn it into a skill json; then learn that
        JSONObject json = new JSONObject();
        JSONArray skills = new JSONArray();
        json.put("skills", skills);
        String lastLine = "", line = "";
        String bang_phrases = "", bang_type = "", bang_term = ""; StringBuilder bang_bag = new StringBuilder();
        boolean prior = false;
        try {readloop: while ((line = br.readLine()) != null) {
            line = line.trim();
            
            if (bang_type.length() > 0) {
                // collect a bang
                if (line.toLowerCase().equals("eol")) {
                    // stop collection
                    if (bang_type.equals("javascript")) {
                        // create a javascript skill
                        JSONObject skill = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        skill.put("phrases", phrases);
                        for (String phrase: bang_phrases.split("\\|")) phrases.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
                        
                        // javascript process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.javascript.name());
                        process.put("expression", bang_bag.toString());
                        skill.put("process", new JSONArray().put(process));
                        
                        // answers; must contain $!$
                        skill.put("actions", new JSONArray().put(SusiAction.answerAction(bang_term.split("\\|"))));
                        skills.put(skill);
                    }
                    if (bang_type.equals("console")) {
                        // create a console skill
                        JSONObject skill = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        skill.put("phrases", phrases);
                        for (String phrase: bang_phrases.split("\\|")) phrases.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
                        
                        // console process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.console.name());
                        JSONObject bo = new JSONObject(new JSONTokener(bang_bag.toString()));
                        if (bo.has("url") && bo.has("path")) {
                            JSONObject definition = new JSONObject().put("url", bo.get("url")).put("path", bo.get("path"));
                            process.put("definition", definition);
                            skill.put("process", new JSONArray().put(process));
                            
                            // actions; we may have several actions here
                            JSONArray actions = new JSONArray();
                            skill.put("actions", actions);
                            
                            // answers; must contain names from the console result array
                            if (bang_term.length() > 0) {
                                actions.put(SusiAction.answerAction(bang_term.split("\\|")));
                            }

                            // optional additional renderings
                            if (bo.has("actions")) {
                                JSONArray bo_actions = bo.getJSONArray("actions");
                                bo_actions.forEach(action -> {
                                    JSONObject boa = (JSONObject) action;
                                    String type = boa.has("type") ? boa.getString("type") : "";
                                    if (type.equals(SusiAction.RenderType.table.toString()) && boa.has("columns") && boa.has("maximumEntries")) {
                                        actions.put(SusiAction.tableAction(boa.getJSONObject("columns"), Integer.parseInt(boa.getJSONObject("maximumEntries").toString())));
                                    } else
                                    if (type.equals(SusiAction.RenderType.piechart.toString()) &&
                                            boa.has("total") && boa.has("key") &&
                                            boa.has("value") && boa.has("unit")) {
                                        actions.put(SusiAction.piechartAction(
                                                boa.getInt("total"), boa.getString("key"),
                                                boa.getString("value"), boa.getString("unit")));
                                    } else
                                    if (type.equals(SusiAction.RenderType.rss.toString()) &&
                                            boa.has("title") && boa.has("description") && boa.has("link")) {
                                        actions.put(SusiAction.rssAction(
                                            boa.getString("title"), boa.getString("description"), boa.getString("link")));
                                    } else
                                    if (type.equals(SusiAction.RenderType.websearch.toString()) && boa.has("query")) {
                                        actions.put(SusiAction.websearchAction(boa.getString("query")));
                                    } else
                                    if (type.equals(SusiAction.RenderType.map.toString()) &&
                                            boa.has("latitude") && boa.has("longitude") && boa.has("zoom")) {
                                        actions.put(SusiAction.mapAction(
                                            boa.getDouble("latitude"), boa.getDouble("longitude"), boa.getInt("zoom")));
                                    }
                                });
                            }
                            skills.put(skill);
                        }
                    }
                    bang_phrases = "";
                    bang_type = "";
                    bang_term = "";
                    bang_bag.setLength(0);
                }
                bang_bag.append(line).append('\n');
                continue readloop;
            }
            
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
                // mid of conversation (last answer is query for next skill)
                String[] phrases = lastLine.split("\\|");
                String condition = null;
                int thenpos = -1;
                if (line.startsWith("?") && (thenpos = line.indexOf(':')) > 0) {
                    int elsepos = line.substring(thenpos + 1).indexOf(':') + thenpos + 1;
                    condition = line.substring(1, thenpos).trim();
                    if (elsepos <= thenpos) {
                        // only if, no else
                        String ifsubstring = line.substring(thenpos + 1).trim();
                        if (ifsubstring.length() > 0) {
                            String[] answers = ifsubstring.split("\\|");
                            JSONObject skill = SusiSkill.answerSkill(phrases, "IF " + condition, answers, prior);
                            skills.put(skill);
                        }
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (ifsubstring.length() > 0) {
                            String[] ifanswers = ifsubstring.split("\\|");
                            JSONObject skillif = SusiSkill.answerSkill(phrases, "IF " + condition, ifanswers, prior);
                            skills.put(skillif);
                        }
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            JSONObject skillelse = SusiSkill.answerSkill(phrases, "NOT " + condition, elseanswers, prior);
                            skills.put(skillelse);
                        }
                    }
                } else if (line.startsWith("!") && (thenpos = line.indexOf(':')) > 0) {
                    bang_phrases = lastLine;
                    bang_type = line.substring(1, thenpos).trim().toLowerCase();
                    bang_term = line.substring(thenpos + 1).trim();
                    bang_bag.setLength(0);
                    continue readloop;
                } else {
                    String[] answers = line.split("\\|");
                    JSONObject skill = SusiSkill.answerSkill(phrases, condition, answers, prior);
                    //System.out.println(skill.toString());
                    skills.put(skill);
                }
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
        JSONArray skills = new JSONArray();
        json.put("skills", skills);
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                JSONObject skill = readAIMLCategory(nl.item(i));
                if (skill != null && skill.length() > 0) skills.put(skill);
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
            return SusiSkill.answerSkill(phrases, null, answers, false);
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

        // add console skills
        JSONObject consoleServices = json.has("console") ? json.getJSONObject("console") : new JSONObject();
        consoleServices.keySet().forEach(console -> {
            JSONObject service = consoleServices.getJSONObject(console);
            if (service.has("url") && service.has("path") && service.has("parser")) {
                String url = service.getString("url");
                String path = service.getString("path");
                String parser = service.getString("parser");
                if (parser.equals("json")) {
                    ConsoleService.addGenericConsole(console, url, path);
                }
            }
        });

        // add conversation skills
        final List<Pattern> removalPattern = new ArrayList<>();
        JSONArray skillset = json.has("rules") ? json.getJSONArray("rules") : json.has("skills") ? json.getJSONArray("skills") : new JSONArray();
        skillset.forEach(j -> {
            List<SusiSkill> skills = SusiSkill.getSkills((JSONObject) j);
            skills.forEach(skill ->
                skill.getKeys().forEach(key -> {
                    Set<SusiSkill> l = this.skilltrigger.get(key);
                    if (l == null) {
                        l = new HashSet<>();
                        this.skilltrigger.put(key, l);
                    }
                    l.add(skill);
                    skill.getPhrases().forEach(phrase -> removalPattern.add(phrase.getPattern()));
                    //skill.getPhrases().forEach(phrase -> this.memories.removeUnanswered(phrase.getPattern()));
                    //System.out.println("***DEBUG: ADD SKILL FOR KEY " + key + ": " + skill.toString());
                })
            );
        });
        
        
        // finally remove patterns in the memory that are known in a background process
        new Thread(new Runnable() {
            @Override
            public void run() {
                removalPattern.forEach(pattern -> SusiMind.this.memories.removeUnanswered(pattern));
            }
        }).start();
        
        return this;
    }
    
    /**
     * extract the mind system from the skilltrigger
     * @return
     */
    public JSONObject getMind() {
        JSONObject mind = new JSONObject(true);
        this.skilltrigger.forEach((key, skillmap) -> {
            JSONArray skills = new JSONArray();
            mind.put(key, skills);
            skillmap.forEach(skill -> {
                JSONObject r = new JSONObject(true);
                r.putAll(skill.toJSON());
                r.put("hash", skill.hashCode());
                skills.put(r);
            });
        });
        return mind;
    }
    
    /**
     * This is the core principle of creativity: being able to match a given input
     * with problem-solving knowledge.
     * This method finds ideas (with a query instantiated skills) for a given query.
     * The skills are selected using a scoring system and pattern matching with the query.
     * Not only the most recent user query is considered for skill selection but also
     * previously requested queries and their answers to be able to set new skill selections
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
            Set<SusiSkill> skill_for_category = this.skilltrigger.get(token.categorized);
            Set<SusiSkill> skill_for_original = token.original.equals(token.categorized) ? null : this.skilltrigger.get(token.original);
            Set<SusiSkill> r = new HashSet<>();
            if (skill_for_category != null) r.addAll(skill_for_category);
            if (skill_for_original != null) r.addAll(skill_for_original);
            r.forEach(skill -> ideas.add(new SusiIdea(skill).setIntent(token)));
        });
        
        for (SusiIdea idea: ideas) DAO.log("idea.phrase-1: score=" + idea.getSkill().getScore().score + " : " + idea.getSkill().getPhrases().toString() + " " + idea.getSkill().getActionsClone());
        
        // add catchall skills always (those are the 'bad ideas')
        Collection<SusiSkill> ca = this.skilltrigger.get(SusiSkill.CATCHALL_KEY);
        if (ca != null) ca.forEach(skill -> ideas.add(new SusiIdea(skill)));
        
        // create list of all ideas that might apply
        TreeMap<Long, List<SusiIdea>> scored = new TreeMap<>();
        AtomicLong count = new AtomicLong(0);
        ideas.forEach(idea -> {
            int score = idea.getSkill().getScore().score;
            long orderkey = Long.MAX_VALUE - ((long) score) * 1000L + count.incrementAndGet();
            List<SusiIdea> r = scored.get(orderkey);
            if (r == null) {r = new ArrayList<>(); scored.put(orderkey, r);}
            r.add(idea);
        });

        // make a sorted list of all ideas
        ideas.clear(); scored.values().forEach(r -> ideas.addAll(r));
        
        for (SusiIdea idea: ideas) DAO.log("idea.phrase-2: score=" + idea.getSkill().getScore().score + " : " + idea.getSkill().getPhrases().toString() + " " + idea.getSkill().getActionsClone());
        
        // test ideas and collect those which match up to maxcount
        List<SusiIdea> plausibleIdeas = new ArrayList<>(Math.min(10, maxcount));
        for (SusiIdea idea: ideas) {
            SusiSkill skill = idea.getSkill();
            Collection<Matcher> m = skill.matcher(query);
            if (m.isEmpty()) continue;
            // TODO: evaluate leading SEE flow commands right here as well
            plausibleIdeas.add(idea);
            if (plausibleIdeas.size() >= maxcount) break;
        }

        for (SusiIdea idea: plausibleIdeas) {
            DAO.log("idea.phrase-3: score=" + idea.getSkill().getScore().score + " : " + idea.getSkill().getPhrases().toString() + " " + idea.getSkill().getActionsClone());
            DAO.log("idea.phrase-3:   log=" + idea.getSkill().getScore().log );
        }

        return plausibleIdeas;
    }
    
    /**
     * react on a user input: this causes the selection of deduction skills and the evaluation of the process steps
     * in every skill up to the moment where enough skills have been applied as consideration. The reaction may also
     * cause the evaluation of operational steps which may cause learning effects within the SusiMind.
     * @param query
     * @param maxcount
     * @return
     */
    public List<SusiArgument> react(String query, int maxcount, String client, SusiThought observation) {
        // get the history a list of thoughts
        SusiArgument observation_argument = new SusiArgument();
        if (observation != null && observation.length() > 0) observation_argument.think(observation);
        List<SusiCognition> cognitions = this.memories.getCognitions(client);
        // latest cognition is first in list
        cognitions.forEach(cognition -> observation_argument.think(cognition.recallDispute()));
        // perform a mindmeld to create a single thought out of the recalled argument
        // the mindmeld will squash the latest thoughts into one so it does not pile up to exponential growth
        SusiThought recall = observation_argument.mindmeld(false);
        
        // normalize the query
        query = SusiPhrase.normalizeExpression(query);
        
        // find an answer
        List<SusiArgument> answers = new ArrayList<>();
        List<SusiIdea> ideas = creativity(query, recall, 100);
        for (SusiIdea idea: ideas) {
            SusiArgument argument = idea.getSkill().consideration(query, recall, idea.getIntent(), this, client);
            if (argument != null) answers.add(argument);
            if (answers.size() >= maxcount) break;
        }
        return answers;
    }
    
    public class Reaction {
        private String expression;
        private SusiThought mindstate;
        
        public Reaction(String query, String client, SusiThought observation) throws RuntimeException {
            List<SusiArgument> datalist = react(query, 1, client, observation);
            if (datalist.size() == 0) throw new RuntimeException("datalist is empty");
            SusiArgument bestargument = datalist.get(0);
            if (bestargument.getActions().isEmpty()) throw new RuntimeException("action list is empty");
            SusiAction action = bestargument.getActions().get(0);
            this.expression = action.execution(bestargument, SusiMind.this, client).getStringAttr("expression");
            this.mindstate = bestargument.mindstate();
            //SusiThought mindmeld = bestargument.mindmeld(true);
        }
        
        public String getExpression() {
            return this.expression;
        }
        
        public SusiThought getMindstate() {
            return this.mindstate;
        }
        
        public String toString() {
            return this.getExpression();
        }
    }

    public Set<String> getSkillsetNames(String client) {
        return this.memories.getSkillsetNames(client);
    }
    
    public JsonTray getSkillset(String client, String name) throws IOException {
        return this.memories.getSkillset(client, name);
    }
    
    public static void main(String[] args) {
        try {
            File init = new File(new File("conf"), "susi");
            File watch = new File(new File("data"), "susi");
            SusiMind mem = new SusiMind(watch, init, watch);
            JSONObject lesson = mem.readJsonLesson(new File("conf/susi/susi_cognition_000.json"));
            mem.learn(lesson);
            System.out.println(mem.new Reaction("I feel funny", "localhost", new SusiThought()).getExpression());
            System.out.println(mem.new Reaction("Help me!", "localhost", new SusiThought()).getExpression());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
}
