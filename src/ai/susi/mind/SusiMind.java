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

import ai.susi.DAO;
import ai.susi.mind.SusiIntent.Score;
import ai.susi.mind.SusiLinguistics.Token;
import ai.susi.mind.SusiPattern.SusiMatcher;
import ai.susi.server.ClientIdentity;
import ai.susi.server.api.susi.ConsoleService;
import ai.susi.tools.AIML2Susi;
import ai.susi.tools.DateParser;
import ai.susi.tools.OnlineCaution;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The mind learns skills and uses creativity to map intents with user utterances
 */
public class SusiMind {

    private final Map<String, Set<SusiSkill>> focusSkills; // a map from the on-word to the skill json object
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
            assert path.exists() : "does not exist: " + path.toString();
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
        this.activeSkill = null;
    }

    public SusiMind addLayer(Layer layer) {
        if (layer != null) {
            if (!layer.path.exists()) {
                layer.path.mkdirs();
            }
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

    public Set<SusiSkill> getFocusSkills(String skillCallName) {
        Set<SusiSkill> skills = this.focusSkills.get(skillCallName.toLowerCase());
        return skills;
    }

    public Map<SusiSkill.ID, SusiSkill> getSkillMetadata() {
        return this.skillMetadata;
    }

    private AtomicLong latestObserve = new AtomicLong(0);

    public SusiMind observe() throws IOException {
        if (System.currentTimeMillis() < latestObserve.get() + 10000) return this; // do not run this too often
        latestObserve.set(System.currentTimeMillis());
        for (int i = 0; i < layers.size(); i++) {
            observe(layers.get(i));
        }
        return this;
    }

    private void observe(Layer layer) throws IOException {
        observe(layer.path, layer.os);
    }

    private void observe(File f, boolean acceptWildcardIntent) throws IOException {
        assert f.exists() : f.getAbsolutePath();
        if (!f.exists()) return;

        Thread.currentThread().setName("ObserveLearn: " + f.getAbsolutePath());

        if (!f.isDirectory() && !f.getName().startsWith(".") && (f.getName().endsWith(".json") || f.getName().endsWith(".txt") || f.getName().endsWith(".aiml"))) {
            if (!observations.containsKey(f) || f.lastModified() > observations.get(f)) {
                DAO.log("observing " + f.toString());
                observations.put(f, System.currentTimeMillis());
                try {
                    if (f.getName().endsWith(".json")) {
                        JSONObject lesson = new JSONObject(new JSONTokener(new FileReader(f)));
                        learn(lesson, f, false);
                    }
                    if (f.getName().endsWith(".txt") || f.getName().endsWith(".ezd") || f.getName().endsWith(".lot")) {
                        SusiSkill.ID skillid = new SusiSkill.ID(f);
                        SusiSkill skill = new SusiSkill(new BufferedReader(new FileReader(f)), skillid, acceptWildcardIntent);
                        learn(skill, skillid, false);
                    }
                    if (f.getName().endsWith(".aiml")) {
                        SusiSkill.ID skillid = new SusiSkill.ID(f);
                        List<SusiIntent> intents = AIML2Susi.readAIMLSkill(f, skillid.language());
                        intents.forEach(intent -> {
                            intent.getKeys().forEach(key -> {
                                Set<SusiIntent> l = this.intenttrigger.get(key);
                                if (l == null) {
                                    l = ConcurrentHashMap.newKeySet();
                                    this.intenttrigger.put(key, l);
                                }
                                l.add(intent);
                            });
                        
                        });
                    }
                } catch (Throwable e) {
                    DAO.severe("BAD JSON FILE: " + f.getAbsolutePath() + ", " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        if (f.isDirectory() ) {
           for (File g: f.listFiles()) {
           	observe(g, acceptWildcardIntent);
           }
        }
        //this.intenttrigger.forEach((term, map) -> System.out.println("***DEBUG trigger " + term + " -> " + map.toString()));
    }

    public SusiMind learn(SusiSkill skill, SusiSkill.ID skillid, boolean acceptFocusSkills) {
        assert skill != null;
        assert skillid != null;

        // handle focus skills
        if (!acceptFocusSkills && skill.getOn() != null && skill.getOn().length > 0) {
            String[] on = skill.getOn();
            for (String o: on) {
                Set<SusiSkill> skills = this.focusSkills.get(o.toLowerCase());
                if (skills == null) {
                    skills = ConcurrentHashMap.newKeySet();
                    this.focusSkills.put(o.toLowerCase(), skills);
                }
                skills.add(skill);
            }
            return this;
        }

        // add conversation intents
        final List<SusiPattern> removalPattern = new ArrayList<>();
        List<SusiIntent> intents = skill.getIntents();
        intents.forEach(intent -> {
            // add removal pattern
            //System.out.println("** INTENT KEYS: " + intent.getKeys().toString());
            intent.getKeys().forEach(key -> {
                Set<SusiIntent> l = this.intenttrigger.get(key);
                if (l == null) {
                    l = ConcurrentHashMap.newKeySet();
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

        this.skillMetadata.put(skillid, skill);

        // finally remove patterns in the memory that are known in a background process
        if (this.memories != null) new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("removeUnanswered");
                for (SusiPattern pattern: removalPattern) {
                    OnlineCaution.throttle(500);
                    SusiMind.this.memories.removeUnanswered(pattern.toString());
                }
            }
        }).start();

        return this;
    }

    public SusiMind learn(JSONObject json, File origin, boolean acceptFocusSkills) {

        // detect the language
        SusiSkill.ID skillid = new SusiSkill.ID(origin);
        SusiLanguage language = skillid.language();
        json.put("origin", origin.getAbsolutePath());

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
        if (json.has("on"))
            skill.setOn(json.getJSONArray("on"));
        if (json.has("description"))
            skill.setDescription(json.getString("description"));
        if (json.has("image"))
           skill.setImage(json.getString("image"));
        if (json.has("skill_name"))
           skill.setSkillName(json.getString("skill_name"));
        if (json.has("protected"))
            skill.setProtectedSkill(json.getBoolean("protected"));
        if (json.has("author"))
            skill.setAuthor(json.getString("author"));
        if (json.has("author_url"))
           skill.setAuthorURL(json.getString("author_url"));
        if (json.has("author_email"))
           skill.setAuthorEmail(json.getString("author_email"));
        if (json.has("developer_privacy_policy"))
           skill.setDeveloperPrivacyPolicy(json.getString("developer_privacy_policy"));
        if (json.has("terms_of_use"))
            skill.setTermsOfUse(json.getString("terms_of_use"));
        if (json.has("dynamic_content"))
            skill.setDynamicContent(json.getBoolean("dynamic_content"));

        // handle focus skills
        if (!acceptFocusSkills && json.has("on")) {
            JSONArray on = json.getJSONArray("on");
            for (int i = 0; i < on.length(); i++) {
                String o = on.getString(i);
                Set<SusiSkill> skills = this.focusSkills.get(o.toLowerCase());
                if (skills == null) {
                    skills = ConcurrentHashMap.newKeySet();
                    this.focusSkills.put(o.toLowerCase(), skills);
                }
                skills.add(skill);
            }
            return this;
        }

        // add conversation intents
        final List<SusiPattern> removalPattern = new ArrayList<>();
        JSONArray intentset = json.has("rules") ? json.getJSONArray("rules") : json.has("intents") ? json.getJSONArray("intents") : new JSONArray();
        intentset.forEach(j -> {
            List<SusiIntent> intents = SusiIntent.getIntents(skillid, (JSONObject) j);
            intents.forEach(intent -> {
                // add removal pattern
                intent.getKeys().forEach(key -> {
                    Set<SusiIntent> l = this.intenttrigger.get(key);
                    if (l == null) {
                        l = ConcurrentHashMap.newKeySet();
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
                removalPattern.forEach(pattern -> SusiMind.this.memories.removeUnanswered(pattern.toString()));
            }
        }).start();

        return this;
    }

    public void learn(List<SusiIntent> intents) {
        intents.forEach(intent -> {
            // add removal pattern
            intent.getKeys().forEach(key -> {
                Set<SusiIntent> l = this.intenttrigger.get(key);
                if (l == null) {
                    l = ConcurrentHashMap.newKeySet();
                    this.intenttrigger.put(key, l);
                }
                l.add(intent);
            });

        });
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
    public List<SusiIdea> creativity(String query, SusiLanguage userLanguage, SusiThought latest_thought, int maxcount, boolean debug) {
        // debugging: write down which intent triggers are stored:
        //System.out.println("** INTENTTRIGGER: " + this.intenttrigger.keySet().toString());

        // tokenize query to have hint for idea collection
        final Set<SusiIdea> ideas = new HashSet<>();
        List<Token> tokenlist = SusiLinguistics.tokenizeSentence(userLanguage, query);
        tokenlist.forEach(token -> {
            Set<SusiIntent> intent_for_category = this.intenttrigger.get(token.categorized);
            Set<SusiIntent> intent_for_original = token.original.equals(token.categorized) ? null : this.intenttrigger.get(token.original);
            Set<SusiIntent> r = ConcurrentHashMap.newKeySet();
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
            SusiIntent intent = idea.getIntent();
            Score score = intent.getScore(query, userLanguage);
            assert score != null : "query = " + query;
            if (score != null) {
                long s = score.score;
                long orderkey = Long.MAX_VALUE - s * 1000L + count.incrementAndGet(); // reverse the ordering: first element has then highest score
                List<SusiIdea> r = scored.get(orderkey);
                if (r == null) {r = new ArrayList<>(); scored.put(orderkey, r);}
                r.add(idea);
            }
        });

        // make a sorted list of all ideas
        final List<SusiIdea> sortedIdeas = new ArrayList<>(scored.size());
        scored.values().forEach(r -> sortedIdeas.addAll(r));

        //for (SusiIdea idea: ideas) DAO.log("idea.phrase-2: score=" + idea.getIntent().getScore(userLanguage).score + " : " + idea.getIntent().getUtterances().toString() + " " + idea.getIntent().getActionsClone());

        // test ideas and collect those which match up to maxcount
        List<SusiIdea> plausibleIdeas = new ArrayList<>(Math.min(10, maxcount));
        for (SusiIdea idea: sortedIdeas) {
            SusiIntent intent = idea.getIntent();
            Collection<SusiMatcher> matchers = intent.matcher(query);
            if (matchers.isEmpty()) continue;
            idea.setMatchers(matchers);
            // TODO: evaluate leading SEE flow commands right here as well
            plausibleIdeas.add(idea);
            if (plausibleIdeas.size() >= maxcount) break;
        }

        if (debug) for (SusiIdea idea: plausibleIdeas) {
            Score score = idea.getIntent().getScore(query, userLanguage);
            assert score != null;
            if (score == null) continue;
            DAO.log("creativity: skill=" + idea.getIntent().getSkillID());
            DAO.log("creativity: score=" + score.score + " : " + idea.getIntent().getUtterances().toString() + " " + idea.getIntent().getActionsClone());
            DAO.log("creativity:   log=" + score.log );
        }

        return plausibleIdeas;
    }


    public static SusiThought reactMinds(
            final String query,
            final SusiLanguage userLanguage,
            final ClientIdentity identity,
            final boolean debug,
            final SusiThought observation,
            final SusiMind... mindLayers) {
        SusiThought thought = null;
        int mindcount = 0;
        while (thought == null && mindcount < mindLayers.length) {
            SusiMind mindlayer = mindLayers[mindcount++];
            thought = mindlayer.react(query, userLanguage, identity, debug, observation, mindLayers);
        }
        return thought;
    }

    /**
     * react on a user input: this causes the selection of deduction intents and the evaluation of the process steps
     * in every intent up to the moment where enough intents have been applied as consideration. The reaction may also
     * cause the evaluation of operational steps which may cause learning effects within the SusiMind.
     * @param query the user input
     * @param client authentication string of the user
     * @param observation an initial thought - that is what susi experiences in the context. I.e. location and language of the user
     * @return
     */
    public SusiThought react(String query, SusiLanguage userLanguage, ClientIdentity identity, boolean debug, SusiThought observation, SusiMind... minds) {
        // get the history a list of thoughts
        long t0 = System.currentTimeMillis();
        SusiArgument observation_argument = new SusiArgument(identity, userLanguage);
        if (observation != null && !observation.isFailed()) observation_argument.think(observation);
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
        SusiThought answer = null;
        List<SusiIdea> ideas = creativity(query, userLanguage, recall, 100, debug); // create a list of ideas which are possible intents
        long t4 = System.currentTimeMillis();

        // test all ideas: the ideas are ranked in such a way that the best one is considered first
        JSONArray testedIdeaQueryPatterns = new JSONArray();
        ideatest: for (SusiIdea idea: ideas) {
            // compute an argument: because one intent represents a horn clause, the argument is a deduction track, a "proof" of the result.
            long t5 = System.currentTimeMillis();
            SusiArgument argument = idea.getIntent().consideration(query, recall, idea, debug, identity, userLanguage, minds);
            long t6 = System.currentTimeMillis();
            if (t6 - t5 > 100) DAO.log("=== Wasted " + (t6 - t5) + " milliseconds with intent " + idea.getIntent().toJSON());

            // arguments may fail; a failed proof is one which does not exist. Therefore an argument may be empty
            if (argument == null) {
                testedIdeaQueryPatterns.put(new JSONObject().put(idea.getIntent().getUtterancesSample(), "fail"));
                continue ideatest; // consider only sound arguments
            }
            try {
                answer = argument.finding(identity, userLanguage, debug, minds);
                testedIdeaQueryPatterns.put(new JSONObject().put(idea.getIntent().getUtterancesSample(), "success"));
                 // a valid idea
                break;
            } catch (ReactionException e) {
                // this happens when the reaction process tries to resolve a reflection and gets a SABTA response
                testedIdeaQueryPatterns.put(new JSONObject().put(idea.getIntent().getUtterancesSample(), e.getMessage()));
                // a bad argument (this is not a runtime error, it is a signal that the thought cannot be thought to the end
                continue ideatest;
            }
        }
        if (answer != null && debug) answer.addTrace(testedIdeaQueryPatterns);
        long t7 = System.currentTimeMillis();
        //DAO.log("+++ react run time: " + (t1 - t0) + " milliseconds - getCognitions");
        //DAO.log("+++ react run time: " + (t2 - t1) + " milliseconds - think");
        //DAO.log("+++ react run time: " + (t3 - t2) + " milliseconds - mindmeld");
        //DAO.log("+++ react run time: " + (t4 - t3) + " milliseconds - normalize");
        //DAO.log("+++ react run time: " + (t7 - t4) + " milliseconds - test ideas");

        // attach the ideas to the thought to have that information available for the explain command
        JSONArray a = answer == null ? null : answer.getData();
        if (a != null) {
            for (int i = 0; i < a.length(); i++) a.getJSONObject(i).remove("idea"); // in case that the ideas size is shorter than the current array length
            if (debug) {
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
        }
        return answer;
    }

    public class Reaction {
        private List<SusiAction> actions;
        private SusiThought mindstate;

        public Reaction(String query, SusiLanguage userLanguage, ClientIdentity identity, boolean debug, SusiThought observation, SusiMind... minds) throws ReactionException {
            this.mindstate = react(query, userLanguage, identity, debug, observation, minds);
            if (this.mindstate == null) throw new ReactionException("no thoughts generated"); // that should be semantically correct if the deduction fails
            this.actions = this.mindstate.getActions(false);
            if (actions.isEmpty()) throw new ReactionException("this mind has no idea what it should do.");
        }

        public List<SusiAction> getActions() {
            return this.actions;
        }

        public List<String> getExpressions() {
            final List<String> expresssions = new ArrayList<>();
            this.actions.forEach(action -> {
                String a = action.getStringAttr("expression");
                if (a != null && a.length() > 0) expresssions.add(a);
            });
            return expresssions;
        }

        public SusiThought getMindstate() {
            return this.mindstate;
        }

        public String toString() {
            return this.getExpressions().toString();
        }
    }

    public static class ReactionException extends Exception {
        private static final long serialVersionUID = 724048490861319902L;
        public ReactionException(String message) {
            super(message);
        }
    }

    // Function overloading - if duration parameter is not passed then use 7 as
    // default value.
    public JSONObject getSkillMetadata(String model, String group, String language, String skillname) {
        return getSkillMetadata(model, group, language, skillname, 7, DAO.model_watch_dir);
    }

    public JSONObject getSkillMetadata(String model, String group, String language, String skillname, int duration) {
        return getSkillMetadata(model, group, language, skillname, duration, DAO.model_watch_dir);
    }

    public JSONObject getSkillMetadata(String model, String group, String language, String skillname, int duration,
            File parentDirectory) {
        JSONObject skillMetadata = new JSONObject(true).put("model", model).put("group", group).put("language",
                language);
        // TODO: Should throw if file not found?
        File skillpath = new SkillQuery(model, group, language, skillname, parentDirectory.toPath()).getSkillFile();
        DateFormat dateFormatType = DateParser.iso8601Format;
        skillname = skillpath.getName().replaceAll(".txt", ""); // fixes the bad name (lowercased) to the actual right
                                                                // name

        // default values
        skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
        skillMetadata.put("descriptions", JSONObject.NULL);
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
            if (skillid.hasModel(model) && skillid.hasGroup(group) && skillid.hasLanguage(language)
                    && skillid.hasName(skillname)) {
                skillMetadata.put("skill_name", skill.getSkillName() == null ? JSONObject.NULL : skill.getSkillName());
                skillMetadata.put("protected", skill.getProtectedSkill());
                skillMetadata.put("developer_privacy_policy",
                        skill.getDeveloperPrivacyPolicy() == null ? JSONObject.NULL
                                : skill.getDeveloperPrivacyPolicy());
                skillMetadata.put("descriptions",
                        skill.getDescription() == null ? JSONObject.NULL : skill.getDescription());
                skillMetadata.put("image", skill.getImage() == null ? JSONObject.NULL : skill.getImage());
                skillMetadata.put("author", skill.getAuthor() == null ? JSONObject.NULL : skill.getAuthor());
                skillMetadata.put("author_url", skill.getAuthorURL() == null ? JSONObject.NULL : skill.getAuthorURL());
                skillMetadata.put("author_email",
                        skill.getAuthorEmail() == null ? JSONObject.NULL : skill.getAuthorEmail());
                skillMetadata.put("terms_of_use",
                        skill.getTermsOfUse() == null ? JSONObject.NULL : skill.getTermsOfUse());
                skillMetadata.put("dynamic_content", skill.getDynamicContent());
                skillMetadata.put("examples", skill.getExamples() == null ? JSONObject.NULL : skill.getExamples());
                skillMetadata.put("skill_rating", DAO.getSkillRating(model, group, language, skillname));
                skillMetadata.put("supported_languages", DAO.getSupportedLanguages(model, group, language, skillname));
                skillMetadata.put("reviewed", DAO.getSkillReviewStatus(model, group, language, skillname));
                skillMetadata.put("editable", DAO.getSkillEditStatus(model, group, language, skillname));
                skillMetadata.put("staffPick", DAO.isStaffPick(model, group, language, skillname));
                skillMetadata.put("systemSkill", DAO.isSystemSkill(model, group, language, skillname));
                skillMetadata.put("usage_count", DAO.getSkillUsage(model, group, language, skillname, duration));
                skillMetadata.put("skill_tag", skillname);
                skillMetadata.put("lastModifiedTime", DAO.getSkillModifiedTime(model, group, language, skillname));
                skillMetadata.put("creationTime",
                        DAO.getSkillCreationTime(model, group, language, skillname, skillpath));
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
        if (attr != null) {
            skillMetadata.put("lastAccessTime", attr.lastAccessTime());
        }
        return skillMetadata;
    }

    public static void main(String[] args) {
        SusiMind mem = new SusiMind(null);
        SusiMind.Layer testlayer = new SusiMind.Layer("test", FileSystems.getDefault().getPath("conf", "os_skills", "test", "en", "alarm.txt").toFile(), true);
        mem.addLayer(testlayer);
        try {mem.observe();} catch (IOException e1) {}
        mem.skillMetadata.values().forEach(skill -> System.out.println(skill.toJSON().toString(2)));
        SusiThought mindstate = mem.react("set an alarm in one minute", SusiLanguage.unknown, ClientIdentity.ANONYMOUS, true, new SusiThought(), mem);
        List<SusiAction> actions = mindstate.getActions(false);
        System.out.println(actions);
    }

}