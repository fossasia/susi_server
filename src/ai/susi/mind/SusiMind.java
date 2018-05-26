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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiMemory.TokenMapList;
import ai.susi.server.api.susi.ConsoleService;
import ai.susi.tools.AIML2Susi;

/**
 * The mind learns skills and uses creativity to map intents with user utterances
 */
public class SusiMind {
    
    public final static int ATTENTION_TIME = 5;
    
    private final Map<String, Set<SusiIntent>> intenttrigger; // a map from a keyword to a set of intents
    private final Map<SusiSkill.ID, Set<String>> skillexamples; // a map from an skill path to one example
    private final Map<SusiSkill.ID, SusiSkill> skillMetadata; // a map from skill path to description
    private final Map<SusiSkill.ID, String> skillImage; // a map from skill path to skill image
    private final List<File> watchpaths;
    private final File susi_chatlog_dir, susi_skilllog_dir; // a path where the memory looks for new additions of knowledge with memory files
    private final Map<File, Long> observations; // a mapping of mind memory files to the time when the file was read the last time
    private final SusiMemory memories; // conversation logs are memories
    private SusiSkill activeSkill;

    public SusiMind(File susi_chatlog_dir, File susi_skilllog_dir, File... watchpaths) {
        // initialize class objects
        this.watchpaths = new ArrayList<>();
        for (int i = 0; i < watchpaths.length; i++) addWatchpath(watchpaths[i]);
        this.susi_chatlog_dir = susi_chatlog_dir;
        this.susi_skilllog_dir = susi_skilllog_dir;
        if (this.susi_chatlog_dir != null) this.susi_chatlog_dir.mkdirs();
        if (this.susi_skilllog_dir != null) this.susi_skilllog_dir.mkdirs();
        this.intenttrigger = new ConcurrentHashMap<>();
        this.observations = new HashMap<>();
        this.memories = new SusiMemory(susi_chatlog_dir, susi_skilllog_dir, ATTENTION_TIME);
        this.skillexamples = new TreeMap<>();
        this.skillMetadata = new TreeMap<>();
        this.skillImage = new TreeMap<>();
        // learn all available intents
        try {observe();} catch (IOException e) {
            e.printStackTrace();
        }
        this.activeSkill = null;
    }
    
    public SusiMind addWatchpath(File path) {
        if (path != null) {
            path.mkdirs();
            this.watchpaths.add(path);
        }
        return this;
    }
    
    public void initializeMemory() {
        this.memories.initializeMemory();
    }

    public SusiMemory getMemories() {
        return this.memories;
    }
    
    public void setActiveSkill(SusiSkill skill) {
        this.activeSkill = skill;
    }
    
    public SusiSkill getActiveSkill() {
        return this.activeSkill;
    }
    
    public Map<String, Integer> getUnanswered() {
        return this.memories.getUnanswered();
    }
    
    public List<TokenMapList> unanswered2tokenizedstats() {
        return this.memories.unanswered2tokenizedstats();
    }
    
    public Map<SusiSkill.ID, Set<String>> getSkillExamples() {
        return this.skillexamples;
    }

    public Map<SusiSkill.ID, SusiSkill> getSkillMetadata() {
        return this.skillMetadata;
    }

    public SusiMind observe() throws IOException {
        for (int i = 0; i < watchpaths.size(); i++) {
            observe(watchpaths.get(i));
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
                            lesson = SusiSkill.readJsonSkill(f);
                        }
                        if (f.getName().endsWith(".txt") || f.getName().endsWith(".ezd") || f.getName().endsWith(".lot")) {
                            SusiSkill.ID skillid = new SusiSkill.ID(f);
                            SusiLanguage language = skillid.language();
                            lesson = SusiSkill.readLoTSkill(new BufferedReader(new FileReader(f)), language, Integer.toString(skillid.hashCode()));
                        }
                        if (f.getName().endsWith(".aiml")) {
                            lesson = AIML2Susi.readAIMLSkill(f);
                        }
                        learn(lesson, f);
                    } catch (Throwable e) {
                        DAO.severe("BAD JSON FILE: " + f.getAbsolutePath() + ", " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //this.intenttrigger.forEach((term, map) -> System.out.println("***DEBUG trigger " + term + " -> " + map.toString()));
    }
    
    public SusiMind learn(JSONObject json, File origin) {
        
        // detect the language
        SusiSkill.ID skillid = new SusiSkill.ID(origin);
        SusiLanguage language = skillid.language();
        
        // teach the language parser
        SusiLinguistics.learn(language, json);

        // add console intents
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

        // add conversation intents
        final List<Pattern> removalPattern = new ArrayList<>();
        JSONArray intentset = json.has("rules") ? json.getJSONArray("rules") : json.has("intents") ? json.getJSONArray("intents") : new JSONArray();
        intentset.forEach(j -> {
            List<SusiIntent> intents = SusiIntent.getIntents(language, (JSONObject) j, skillid);
            intents.forEach(intent -> {
                // add removal pattern
                intent.getKeys().forEach(key -> {
                    Set<SusiIntent> l = this.intenttrigger.get(key);
                    if (l == null) {
                        l = new HashSet<>();
                        this.intenttrigger.put(key, l);
                    }
                    l.add(intent);
                    intent.getUtterances().forEach(utterance -> removalPattern.add(utterance.getPattern()));
                    //intent.getPhrases().forEach(utterance -> this.memories.removeUnanswered(utterance.getPattern()));
                    //System.out.println("***DEBUG: ADD INTENT FOR KEY " + key + ": " + intent.toString());
                });
                // Susi skill object for skill metadata
                SusiSkill skill = new SusiSkill();

                // collect intent example and test the intents using the example/expect terms
                if (intent.getExample() != null) {
                    //DAO.log("intent for '" + intent.getExample() + "' in \n" + intent.getSkill() + "\n");
                    Set<String> examples = this.skillexamples.get(intent.getSkill());
                    if (examples == null) {
                        examples = new LinkedHashSet<>();
                        this.skillexamples.put(intent.getSkill(), examples);
                    }
                    examples.add(intent.getExample());
                    skill.setExamples(examples);

                }
                // skill description

                if(json.has("description"))
                    skill.setDescription(json.getString("description"));
                // skill image
                if(json.has("image"))
                   skill.setImage(json.getString("image"));
                // adding skill meta data
                if(json.has("skill_name"))
                   skill.setSkillName(json.getString("skill_name"));
                if(json.has("author"))
                    skill.setAuthor(json.getString("author"));
                if(json.has("author_url"))
                   skill.setAuthorURL(json.getString("author_url"));
                if(json.has("developer_privacy_policy"))
                   skill.setDeveloperPrivacyPolicy(json.getString("developer_privacy_policy"));
                if(json.has("terms_of_use"))
                    skill.setTermsOfUse(json.getString("terms_of_use"));
                if(json.has("dynamic_content"))
                    skill.setDynamicContent(json.getBoolean("dynamic_content"));
                this.skillMetadata.put(intent.getSkill(), skill);

                //if (intent.getExample() != null && intent.getExpect() != null) {}
            });
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
     * extract the mind system from the intenttrigger
     * @return
     */
    public JSONObject getMind() {
        JSONObject mind = new JSONObject(true);
        this.intenttrigger.forEach((key, intentmap) -> {
            JSONArray intents = new JSONArray();
            mind.put(key, intents);
            intentmap.forEach(intent -> {
                JSONObject r = new JSONObject(true);
                r.putAll(intent.toJSON());
                r.put("hash", intent.hashCode());
                intents.put(r);
            });
        });
        return mind;
    }
    
    /**
     * This is the core principle of creativity: being able to match a given input
     * with problem-solving knowledge.
     * This method finds ideas (with a query instantiated intents) for a given query.
     * The intents are selected using a scoring system and pattern matching with the query.
     * Not only the most recent user query is considered for intent selection but also
     * previously requested queries and their answers to be able to set new intent selections
     * in the context of the previous conversation.
     * @param query the user input
     * @param previous_argument the latest conversation with the same user
     * @param maxcount the maximum number of ideas to return
     * @return an ordered list of ideas, first idea should be considered first.
     */
    public List<SusiIdea> creativity(String query, SusiLanguage userLanguage, SusiThought latest_thought, int maxcount) {
        // tokenize query to have hint for idea collection
        final List<SusiIdea> ideas = new ArrayList<>();
        SusiLinguistics.tokenizeSentence(userLanguage, query).forEach(token -> {
            Set<SusiIntent> intent_for_category = this.intenttrigger.get(token.categorized);
            Set<SusiIntent> intent_for_original = token.original.equals(token.categorized) ? null : this.intenttrigger.get(token.original);
            Set<SusiIntent> r = new HashSet<>();
            if (intent_for_category != null) r.addAll(intent_for_category);
            if (intent_for_original != null) r.addAll(intent_for_original);
            r.forEach(intent -> ideas.add(new SusiIdea(intent).setToken(token)));
        });
        
        //for (SusiIdea idea: ideas) DAO.log("idea.phrase-1: score=" + idea.getIntent().getScore(userLanguage).score + " : " + idea.getIntent().getUtterances().toString() + " " + idea.getIntent().getActionsClone());
        
        // add catchall intents always (those are the 'bad ideas')
        Collection<SusiIntent> ca = this.intenttrigger.get(SusiIntent.CATCHALL_KEY);
        if (ca != null) ca.forEach(intent -> ideas.add(new SusiIdea(intent)));
        
        // create list of all ideas that might apply
        TreeMap<Long, List<SusiIdea>> scored = new TreeMap<>();
        AtomicLong count = new AtomicLong(0);
        ideas.forEach(idea -> {
            long score = idea.getIntent().getScore(userLanguage).score;
            long orderkey = Long.MAX_VALUE - score * 1000L + count.incrementAndGet();
            List<SusiIdea> r = scored.get(orderkey);
            if (r == null) {r = new ArrayList<>(); scored.put(orderkey, r);}
            r.add(idea);
        });

        // make a sorted list of all ideas
        ideas.clear(); scored.values().forEach(r -> ideas.addAll(r));
        
        //for (SusiIdea idea: ideas) DAO.log("idea.phrase-2: score=" + idea.getIntent().getScore(userLanguage).score + " : " + idea.getIntent().getUtterances().toString() + " " + idea.getIntent().getActionsClone());
        
        // test ideas and collect those which match up to maxcount
        List<SusiIdea> plausibleIdeas = new ArrayList<>(Math.min(10, maxcount));
        for (SusiIdea idea: ideas) {
            SusiIntent intent = idea.getIntent();
            Collection<Matcher> m = intent.matcher(query);
            if (m.isEmpty()) continue;
            // TODO: evaluate leading SEE flow commands right here as well
            plausibleIdeas.add(idea);
            if (plausibleIdeas.size() >= maxcount) break;
        }

        for (SusiIdea idea: plausibleIdeas) {
            DAO.log("idea.phrase-3: score=" + idea.getIntent().getScore(userLanguage).score + " : " + idea.getIntent().getUtterances().toString() + " " + idea.getIntent().getActionsClone());
            DAO.log("idea.phrase-3:   log=" + idea.getIntent().getScore(userLanguage).log );
        }

        return plausibleIdeas;
    }
    
    /**
     * react on a user input: this causes the selection of deduction intents and the evaluation of the process steps
     * in every intent up to the moment where enough intents have been applied as consideration. The reaction may also
     * cause the evaluation of operational steps which may cause learning effects within the SusiMind.
     * @param query the user input
     * @param maxcount the maximum number of answers (typical is only one)
     * @param client authentication string of the user
     * @param observation an initial thought - that is what susi experiences in the context. I.e. location and language of the user
     * @return
     */
    public List<SusiThought> react(String query, SusiLanguage userLanguage, int maxcount, String client, SusiThought observation, SusiMind... minds) {
        // get the history a list of thoughts
        long t0 = System.currentTimeMillis();
        SusiArgument observation_argument = new SusiArgument();
        if (observation != null && observation.length() > 0) observation_argument.think(observation);
        List<SusiCognition> cognitions = this.memories.getCognitions(client);
        long t1 = System.currentTimeMillis();
        // latest cognition is first in list
        cognitions.forEach(cognition -> observation_argument.think(cognition.recallDispute()));
        long t2 = System.currentTimeMillis();
        // perform a mindmeld to create a single thought out of the recalled argument
        // the mindmeld will squash the latest thoughts into one so it does not pile up to exponential growth
        SusiThought recall = observation_argument.mindmeld(false);
        long t3 = System.currentTimeMillis();
        
        // normalize the query
        query = SusiUtterance.normalizeExpression(query);
        
        // find an answer
        List<SusiThought> answers = new ArrayList<>();
        List<SusiIdea> ideas = creativity(query, userLanguage, recall, 100); // create a list of ideas which are possible intents
        long t4 = System.currentTimeMillis();
        
        // test all ideas: the ideas are ranked in such a way that the best one is considered first
        ideatest: for (SusiIdea idea: ideas) {
            // compute an argument: because one intent represents a horn clause, the argument is a deduction track, a "proof" of the result.
            long t5 = System.currentTimeMillis();
            SusiArgument argument = idea.getIntent().consideration(query, recall, idea.getToken(), this, client);
            long t6 = System.currentTimeMillis();
            if (t6 - t5 > 100) DAO.log("=== Wasted " + (t6 - t5) + " milliseconds with intent " + idea.getIntent().toJSON());
            
            // arguments may fail; a failed proof is one which does not exist. Therefore an argument may be empty
            if (argument == null) {
                continue ideatest; // consider only sound arguments
            }
            try {
				answers.add(argument.finding(client, userLanguage, minds));
			} catch (ReactionException e) {
				// a bad argument (this is not a runtime error, it is a signal that the thought cannot be thought to the end
				continue ideatest;
			} // a valid idea
            if (answers.size() >= maxcount) break; // and stop if we are done
        }
        long t7 = System.currentTimeMillis();
        //DAO.log("+++ react run time: " + (t1 - t0) + " milliseconds - getCognitions");
        //DAO.log("+++ react run time: " + (t2 - t1) + " milliseconds - think");
        //DAO.log("+++ react run time: " + (t3 - t2) + " milliseconds - mindmeld");
        //DAO.log("+++ react run time: " + (t4 - t3) + " milliseconds - normalize");
        //DAO.log("+++ react run time: " + (t7 - t4) + " milliseconds - test ideas");
        return answers;
    }
    
    public static List<SusiThought> reactMinds(String query, SusiLanguage userLanguage, int maxcount, String client, SusiThought observation, SusiMind... minds) {
    	return minds[0].react(query, userLanguage, maxcount, client, observation, minds);
    }
    
    public class Reaction {
        private String expression;
        private SusiThought mindstate;
        
        public Reaction(String query, SusiLanguage userLanguage, String client, SusiThought observation, SusiMind... minds) throws ReactionException {
            List<SusiThought> thoughts = react(query, userLanguage, 1, client, observation, minds);
            thoughts = SusiThought.filterExpressionAction(thoughts);
            
            if (thoughts.size() == 0) throw new ReactionException("empty mind, no idea");
            this.mindstate = thoughts.get(0);
            List<SusiAction> actions = this.mindstate.getActions();
            SusiAction action = actions.get(0);
            this.expression = action.getStringAttr("expression");
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
    
    public static class ReactionException extends Exception {
		private static final long serialVersionUID = 724048490861319902L;
		public ReactionException(String message) {
            super(message);
        }
    }

    public Set<String> getIntentsetNames(String client) {
        return this.memories.getIntentsetNames(client);
    }
    
    public JsonTray getIntentset(String client, String name) throws IOException {
        return this.memories.getIntentset(client, name);
    }
    
    public static void main(String[] args) {
        File skill = new File(new File("conf"), "susi");
        File log = new File(new File("data"), "susi");
        SusiMind mem = new SusiMind(log, null, skill);
        try {
            System.out.println(mem.new Reaction("I feel funny", SusiLanguage.unknown, "localhost", new SusiThought(), mem).getExpression());
        } catch (ReactionException e) {
            e.printStackTrace();
        }
        try {
            System.out.println(mem.new Reaction("Help me!", SusiLanguage.unknown, "localhost", new SusiThought(), mem).getExpression());
        } catch (ReactionException e) {
            e.printStackTrace();
        }
    }
    
}
