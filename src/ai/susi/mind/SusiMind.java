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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;
import ai.susi.server.ClientIdentity;
import ai.susi.server.api.susi.ConsoleService;
import ai.susi.tools.AIML2Susi;
import ai.susi.tools.DateParser;

/**
 * The mind learns skills and uses creativity to map intents with user utterances
 */
public class SusiMind {
    
    private final Map<String, JSONObject> focusSkills; // a map from the on-word to the skill json object
    private final Map<String, Set<SusiIntent>> intenttrigger; // a map from a keyword to a set of intents
    private final Map<SusiSkill.ID, SusiSkill> skillMetadata; // a map from skill path to description
    private final List<Layer> layers;
    private final Map<File, Long> observations; // a mapping of mind memory files to the time when the file was read the last time
    private final SusiMemory memories; // conversation logs are memories
    private SusiSkill activeSkill;

    /**
     * To implement different layers of mind declarations, we need an object that describes the layer of minds.
     * 
     * @param susi_chatlog_dir
     * @param susi_skilllog_dir
     * @param layers
     */
    public static class Layer {

        public final File path;   // the storage path of the skills which belong to one mind level
        public final String name; // the explanatory name of the mind which is used using the explain with the "explain" command
        public final boolean os;  // only custom skills or skills in os minds may declare wildcard answers. 

        public Layer(String name, File path, boolean os) {
            this.name = name;
            this.path = path;
            this.os = os;
        }
    }
    
    public SusiMind(SusiMemory memory) {
        // initialize class objects
        this.layers = new ArrayList<>();
        this.focusSkills = new HashMap<>();
        this.intenttrigger = new ConcurrentHashMap<>();
        this.observations = new HashMap<>();
        this.memories = memory;
        this.skillMetadata = new TreeMap<>();
        // learn all available intents
        try {observe();} catch (IOException e) {
            e.printStackTrace();
        }
        this.activeSkill = null;
    }
    
    public SusiMind addLayer(Layer layer) {
        if (layer != null) {
            if (!layer.path.exists()) layer.path.mkdirs();
            this.layers.add(layer);
        }
        return this;
    }
    
    public void setActiveSkill(SusiSkill skill) {
        this.activeSkill = skill;
    }
    
    public SusiSkill getActiveSkill() {
        return this.activeSkill;
    }
    
    public Set<String> getSkillExamples(SusiSkill.ID id) {
        return this.skillMetadata.get(id).getExamples();
    }

    public Map<SusiSkill.ID, SusiSkill> getSkillMetadata() {
        return this.skillMetadata;
    }
    
    public JSONObject getFocusSkill(String skillCallName) {
        return this.focusSkills.get(skillCallName.toLowerCase());
    }

    public SusiMind observe() throws IOException {
        for (int i = 0; i < layers.size(); i++) {
            observe(layers.get(i));
        }
        return this;
    }
    
    private void observe(Layer layer) throws IOException {
        observe(layer.path, layer.os);
    }
    
    private void observe(File path, boolean acceptWildcardIntent) throws IOException {
        if (!path.exists()) return;
        for (File f: path.listFiles()) {
            if (f.isDirectory()) {
                // recursively step into it
                observe(f, acceptWildcardIntent);
            }
            if (!f.isDirectory() && !f.getName().startsWith(".") && (f.getName().endsWith(".json") || f.getName().endsWith(".txt") || f.getName().endsWith(".aiml"))) {
                if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                    observations.put(f, System.currentTimeMillis());
                    try {
                        JSONObject lesson = new JSONObject();
                        if (f.getName().endsWith(".json")) {
                            lesson = new JSONObject(new JSONTokener(new FileReader(f)));
                        }
                        if (f.getName().endsWith(".txt") || f.getName().endsWith(".ezd") || f.getName().endsWith(".lot")) {
                            SusiSkill.ID skillid = new SusiSkill.ID(f);
                            SusiLanguage language = skillid.language();
                            lesson = SusiSkill.readLoTSkill(new BufferedReader(new FileReader(f)), language, skillid.getPath(), acceptWildcardIntent);
                        }
                        if (f.getName().endsWith(".aiml")) {
                            lesson = AIML2Susi.readAIMLSkill(f);
                        }
                        learn(lesson, f, false);
                    } catch (Throwable e) {
                        DAO.severe("BAD JSON FILE: " + f.getAbsolutePath() + ", " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //this.intenttrigger.forEach((term, map) -> System.out.println("***DEBUG trigger " + term + " -> " + map.toString()));
    }
    
    public SusiMind learn(JSONObject json, File origin, boolean acceptFocusSkills) {

        // detect the language
        SusiSkill.ID skillid = new SusiSkill.ID(origin);
        SusiLanguage language = skillid.language();
        json.put("origin", origin.getAbsolutePath());
        
        if (!acceptFocusSkills && json.has("on")) {
            JSONArray on = json.getJSONArray("on");
            for (int i = 0; i < on.length(); i++) {
                this.focusSkills.put(on.getString(i), json);
            }
            return this;
        }
        
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

        // start to collect data for skill metadata
        SusiSkill skill = new SusiSkill();

        // skill description
        if(json.has("on"))
            skill.setOn(json.getJSONArray("on"));
        if(json.has("description"))
            skill.setDescription(json.getString("description"));
        // skill image
        if(json.has("image"))
           skill.setImage(json.getString("image"));
        // adding skill meta data
        if(json.has("skill_name"))
           skill.setSkillName(json.getString("skill_name"));
        if(json.has("protected"))
            skill.setProtectedSkill(json.getBoolean("protected"));
        if(json.has("author"))
            skill.setAuthor(json.getString("author"));
        if(json.has("author_url"))
           skill.setAuthorURL(json.getString("author_url"));
        if(json.has("author_email"))
           skill.setAuthorEmail(json.getString("author_email"));
        if(json.has("developer_privacy_policy"))
           skill.setDeveloperPrivacyPolicy(json.getString("developer_privacy_policy"));
        if(json.has("terms_of_use"))
            skill.setTermsOfUse(json.getString("terms_of_use"));
        if(json.has("dynamic_content"))
            skill.setDynamicContent(json.getBoolean("dynamic_content"));
        
        
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

                if (intent.hasExample())
                    skill.addExample(intent.getExample());
            });
        });

        this.skillMetadata.put(skillid, skill);
        
        // finally remove patterns in the memory that are known in a background process
        if (this.memories != null) new Thread(new Runnable() {
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
    public List<SusiThought> react(String query, SusiLanguage userLanguage, int maxcount, ClientIdentity identity, SusiThought observation, SusiMind... minds) {
        // get the history a list of thoughts
        long t0 = System.currentTimeMillis();
        SusiArgument observation_argument = new SusiArgument();
        if (observation != null && observation.length() > 0) observation_argument.think(observation);
        List<SusiCognition> cognitions = this.memories == null ? new ArrayList<>() : this.memories.getCognitions(identity.getClient(), true);
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
            SusiArgument argument = idea.getIntent().consideration(query, recall, idea.getToken(), this, identity);
            long t6 = System.currentTimeMillis();
            if (t6 - t5 > 100) DAO.log("=== Wasted " + (t6 - t5) + " milliseconds with intent " + idea.getIntent().toJSON());
            
            // arguments may fail; a failed proof is one which does not exist. Therefore an argument may be empty
            if (argument == null) {
                continue ideatest; // consider only sound arguments
            }
            try {
				answers.add(argument.finding(identity, userLanguage, minds));
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

        // attach the ideas to the thought to have that information available for the explain command
        if (answers.size() > 0) {
            SusiThought t = answers.get(0);
            JSONArray a = t.getData();
            for (int i = 0; i < a.length(); i++) a.getJSONObject(i).remove("idea"); // in case that the ideas size is shorter than the current array length
            int i = 0;
            for (SusiIdea idea: ideas) {
                if (a.length() <= i) {
                    a.put(new JSONObject());
                }
                JSONObject j = a.getJSONObject(i);
                j.put("idea", idea.getIntent().toJSON());
                i++;
            }
        }
        return answers;
    }
    
    public static List<SusiThought> reactMinds(
    		final String query,
    		final SusiLanguage userLanguage,
    		final int maxcount,
    		final ClientIdentity identity,
    		final SusiThought observation,
    		final SusiMind... mindLayers) {
        List<SusiThought> thoughts = new ArrayList<>();
        int mindcount = 0;
        while (thoughts.isEmpty() && mindcount < mindLayers.length) {
            thoughts = mindLayers[mindcount++].react(query, userLanguage, maxcount, identity, observation, mindLayers);
        }
        return thoughts;
    }
    
    public class Reaction {
    	private SusiAction action;
        private SusiThought mindstate;

        public Reaction(String query, SusiLanguage userLanguage, ClientIdentity identity, SusiThought observation, SusiMind... minds) throws ReactionException {
            List<SusiThought> thoughts = react(query, userLanguage, 1, identity, observation, minds);
            this.mindstate = thoughts.get(0);
            List<SusiAction> actions = this.mindstate.getActions();
            if (actions.isEmpty()) throw new ReactionException("this mind has no idea what it should do.");
            this.action = actions.get(0);
        }
        
        public SusiAction getAction() {
            return this.action;
        }
        
        public String getExpression() {
            return this.action.getStringAttr("expression");
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

    // Function overloading - if duration parameter is not passed then use 7 as default value.
    public JSONObject getSkillMetadata(String model, String group, String language, String skillname) {
        return getSkillMetadata(model, group, language, skillname, 7);
    }
    
    public JSONObject getSkillMetadata(String model, String group, String language, String skillname, int duration) {

        JSONObject skillMetadata = new JSONObject(true)
                .put("model", model)
                .put("group", group)
                .put("language", language);
        File modelpath = new File(DAO.model_watch_dir, model);
        File grouppath = new File(modelpath, group);
        File languagepath = new File(grouppath, language);
        File skillpath = DAO.getSkillFileInLanguage(languagepath, skillname, false);
        DateFormat dateFormatType = DateParser.iso8601Format;
        skillname = skillpath.getName().replaceAll(".txt", ""); // fixes the bad name (lowercased) to the actual right name

        // default values
        skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
        skillMetadata.put("descriptions",JSONObject.NULL);
        skillMetadata.put("image", JSONObject.NULL);
        skillMetadata.put("author", JSONObject.NULL);
        skillMetadata.put("author_url", JSONObject.NULL);
        skillMetadata.put("author_email", JSONObject.NULL);
        skillMetadata.put("skill_name", JSONObject.NULL);
        skillMetadata.put("protected", false);
        skillMetadata.put("reviewed", false);
        skillMetadata.put("editable", true);
        skillMetadata.put("staffPick", false);
        skillMetadata.put("systemSkill", false);
        skillMetadata.put("terms_of_use", JSONObject.NULL);
        skillMetadata.put("dynamic_content", false);
        skillMetadata.put("examples", JSONObject.NULL);
        skillMetadata.put("skill_rating", JSONObject.NULL);
        skillMetadata.put("usage_count", 0);
        skillMetadata.put("skill_tag", JSONObject.NULL);
        skillMetadata.put("lastModifiedTime", dateFormatType.format(new Date(0)));
        skillMetadata.put("creationTime", dateFormatType.format(new Date(0)));

        // metadata
        for (Map.Entry<SusiSkill.ID, SusiSkill> entry : getSkillMetadata().entrySet()) {
            SusiSkill skill = entry.getValue();
            SusiSkill.ID skillid = entry.getKey();
            if (skillid.hasModel(model) &&
                    skillid.hasGroup(group) &&
                    skillid.hasLanguage(language) &&
                    skillid.hasName(skillname)) {

                skillMetadata.put("skill_name", skill.getSkillName() ==null ? JSONObject.NULL: skill.getSkillName());
                skillMetadata.put("protected", skill.getProtectedSkill());
                skillMetadata.put("developer_privacy_policy", skill.getDeveloperPrivacyPolicy() ==null ? JSONObject.NULL:skill.getDeveloperPrivacyPolicy());
                skillMetadata.put("descriptions", skill.getDescription() ==null ? JSONObject.NULL:skill.getDescription());
                skillMetadata.put("image", skill.getImage() ==null ? JSONObject.NULL: skill.getImage());
                skillMetadata.put("author", skill.getAuthor()  ==null ? JSONObject.NULL:skill.getAuthor());
                skillMetadata.put("author_url", skill.getAuthorURL() ==null ? JSONObject.NULL:skill.getAuthorURL());
                skillMetadata.put("author_email", skill.getAuthorEmail() ==null ? JSONObject.NULL:skill.getAuthorEmail());
                skillMetadata.put("terms_of_use", skill.getTermsOfUse() ==null ? JSONObject.NULL:skill.getTermsOfUse());
                skillMetadata.put("dynamic_content", skill.getDynamicContent());
                skillMetadata.put("examples", skill.getExamples() ==null ? JSONObject.NULL: skill.getExamples());
                skillMetadata.put("skill_rating", DAO.getSkillRating(model, group, language, skillname));
                skillMetadata.put("supported_languages", DAO.getSupportedLanguages(model, group, language, skillname));
                skillMetadata.put("reviewed", DAO.getSkillReviewStatus(model, group, language, skillname));
                skillMetadata.put("editable", DAO.getSkillEditStatus(model, group, language, skillname));
                skillMetadata.put("staffPick", DAO.isStaffPick(model, group, language, skillname));
                skillMetadata.put("systemSkill", DAO.isSystemSkill(model, group, language, skillname));
                skillMetadata.put("usage_count", DAO.getSkillUsage(model, group, language, skillname, duration));
                skillMetadata.put("skill_tag", skillname);
                skillMetadata.put("lastModifiedTime", DAO.getSkillModifiedTime(model, group, language, skillname));
                skillMetadata.put("creationTime", DAO.getSkillCreationTime(model, group, language, skillname, skillpath));
            }
        }

        // file attributes
        BasicFileAttributes attr = null;
        Path p = Paths.get(skillpath.getPath());
        try {
            attr = Files.readAttributes(p, BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(attr!=null){
            skillMetadata.put("lastAccessTime" , attr.lastAccessTime());
        }
        return skillMetadata;
    }

    
    public static void main(String[] args) {
        SusiMind mem = new SusiMind(null);
        SusiMind.Layer testlayer = new SusiMind.Layer("test", new File(new File("conf"), "susi"), true);
        mem.addLayer(testlayer);
        try {
            System.out.println(mem.new Reaction("I feel funny", SusiLanguage.unknown, new ClientIdentity("localhost"), new SusiThought(), mem).getExpression());
        } catch (ReactionException e) {
            e.printStackTrace();
        }
        try {
            System.out.println(mem.new Reaction("Help me!", SusiLanguage.unknown, new ClientIdentity("localhost"), new SusiThought(), mem).getExpression());
        } catch (ReactionException e) {
            e.printStackTrace();
        }
    }
    
}
