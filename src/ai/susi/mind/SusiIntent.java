/**
 *  SusiIntent
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.tools.TimeoutMatcher;

/**
 * An intent in the Susi AI framework is a collection of phrases, inference processes and actions that are applied
 * on external sense data if the phrases identify that this intent set would be applicable on the sense data.
 * A set of intent would is a intent on how to handle activities from the outside of the AI and react on
 * such activities.
 */
public class SusiIntent {

    public final static String CATCHALL_KEY = "*";
    public final static int    DEFAULT_SCORE = 10;
    
    private final List<SusiPhrase> phrases;
    private final List<SusiInference> inferences;
    private final List<SusiAction> actions;
    private final Set<String> keys;
    private final String comment;
    private final int user_subscore;
    private Score score;
    private final int id;
    private final String skill, example, expect, description;
    private SusiLanguage language;
    
    /**
     * Generate a set of intents from a single intent definition. This may be possible if the intent contains an 'options'
     * object which creates a set of intents, one for each option. The options combine with one set of phrases
     * @param json - a multi-intent definition
     * @return a set of intents
     */
    public static List<SusiIntent> getIntents(JSONObject json, File origin) {
        if (!json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        final List<SusiIntent> intents = new ArrayList<>();
        if (json.has("options")) {
            JSONArray options = json.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = new JSONObject();
                option.put("phrases", json.get("phrases"));
                JSONObject or = options.getJSONObject(i);
                for (String k: or.keySet()) option.put(k, or.get(k));
                intents.add(new SusiIntent(option, origin));
            }
        } else {
            try {
                SusiIntent intent = new SusiIntent(json, origin);
                intents.add(intent);
            } catch (PatternSyntaxException e) {
                Logger.getLogger("SusiIntent").warning("Regular Expression error in Susi Intent: " + json.toString(2));
            }
        }
        return intents;
    }
    
    /**
     * Create an intent by parsing of the intent description
     * @param json the intent description
     * @throws PatternSyntaxException
     */
    private SusiIntent(JSONObject json, File origin) throws PatternSyntaxException {
        
        // extract the phrases and the phrases subscore
        if (!json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        JSONArray p = (JSONArray) json.remove("phrases");
        this.phrases = new ArrayList<>(p.length());
        p.forEach(q -> this.phrases.add(new SusiPhrase((JSONObject) q)));
        
        // extract the actions and the action subscore
        if (!json.has("actions")) throw new PatternSyntaxException("actions missing", "", 0);
        p = (JSONArray) json.remove("actions");
        this.actions = new ArrayList<>(p.length());
        p.forEach(q -> this.actions.add(new SusiAction((JSONObject) q)));
        
        // extract the inferences and the process subscore; there may be no inference at all
        if (json.has("process")) {
            p = (JSONArray) json.remove("process");
            this.inferences = new ArrayList<>(p.length());
            p.forEach(q -> this.inferences.add(new SusiInference((JSONObject) q)));
        } else {
            this.inferences = new ArrayList<>(0);
        }
        
        // extract (or compute) the keys; there may be none key given, then they will be computed
        this.keys = new HashSet<>();
        JSONArray k;
        if (json.has("keys")) {
            k = json.getJSONArray("keys");
            if (k.length() == 0 || (k.length() == 1 && k.getString(0).length() == 0)) k = computeKeysFromPhrases(this.phrases);
        } else {
            k = computeKeysFromPhrases(this.phrases);
        }
        
        k.forEach(o -> this.keys.add((String) o));

        this.user_subscore = json.has("score") ? json.getInt("score") : DEFAULT_SCORE;
        this.score = null; // calculate this later if required
        
        // extract the comment
        this.comment = json.has("comment") ? json.getString("comment") : "";

        // remember the origin
        String skillpath = origin.getAbsolutePath();
    	int i = skillpath.indexOf("/susi");
    	if (i < 0) skillpath = ""; else {
    	    skillpath = skillpath.substring(i);
    	    if (skillpath.startsWith("/susi/")) skillpath = skillpath.substring(5);
    	}
    	this.skill = skillpath;
    	
    	// compute the language from the origin
    	this.language = SusiLanguage.unknown;
    	try {
    	    if (this.skill.startsWith("/susi_server/conf/susi/")) {
    	        this.language = SusiLanguage.valueOf(this.skill.substring(23, 25));
    	    } else if (this.skill.startsWith("/susi_skill_data")) {
    	        String[] paths = this.skill.split("/");
                if (paths.length > 5) this.language = SusiLanguage.valueOf(paths[5]);
    	    }
    	} catch (IllegalArgumentException e) {
            this.language = SusiLanguage.unknown;
    	}
        
    	// quality control
        this.example = json.has("example") ? json.getString("example") : "";
        this.expect = json.has("expect") ? json.getString("expect") : "";
        this.description = json.has("description") ? json.getString("description") : "";
    	
        // calculate the id
        String ids0 = this.actions.toString();
        String ids1 = this.phrases.toString();
        this.id = ids0.hashCode() + ids1.hashCode();
    }
    
    public String getSkill() {
        return this.skill == null || this.skill.length() == 0 ? null : this.skill;
    }
    
    public String getExpect() {
        return this.expect == null || this.expect.length() == 0 ? null : this.expect;
    }
    
    public String getExample() {
        return this.example == null || this.example.length() == 0 ? null : this.example;
    }

    public String getDescription() {
        return  this.description == null || this.description.length() == 0 ? null : this.description;
    }
    public int hashCode() {
        return this.id;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        json.put("id", this.id);
        if (this.keys != null && this.keys.size() > 0) json.put("keys", new JSONArray(this.keys));
        JSONArray p = new JSONArray(); this.phrases.forEach(phrase -> p.put(phrase.toJSON()));
        json.put("phrases", p);
        JSONArray i = new JSONArray(); this.inferences.forEach(inference -> i.put(inference.getJSON()));
        json.put("process", i);
        JSONArray a = new JSONArray(); this.getActionsClone().forEach(action ->a.put(action.toJSONClone()));
        json.put("actions", a);
        if (this.comment != null && this.comment.length() > 0) json.put("comment", this.comment);
        if (this.score != null) json.put("score", this.score.score);
        if (this.skill != null && this.skill.length() > 0) json.put("skill", this.skill);
        if (this.example != null && this.example.length() > 0) json.put("example", example);
        if (this.description != null && this.description.length() > 0) json.put("description", description);
        if (this.expect != null && this.expect.length() > 0) json.put("expect", expect);
        return json;
    }
    
    public static JSONObject answerIntent(
            String[] phrases,
            String condition,
            String[] answers,
            boolean prior,
            String example,
            String description,
            String expect) {
        JSONObject intent = new JSONObject(true);

        // write phrases
        JSONArray p = new JSONArray();
        intent.put("phrases", p);
        for (String phrase: phrases) p.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
        
        // write conditions (if any)
        if (condition != null && condition.length() > 0) {
            JSONArray c = new JSONArray();
            intent.put("process", c);
            c.put(SusiInference.simpleMemoryProcess(condition));   
        }

        // quality control
        if (example != null && example.length() > 0) intent.put("example", example);
        if (expect != null && expect.length() > 0) intent.put("expect", expect);
        if (description != null && description.length() > 0) intent.put("description", description);
        // write actions
        JSONArray a = new JSONArray();
        intent.put("actions", a);
        a.put(SusiAction.answerAction(answers));        
        return intent;
    }
    
    public String toString() {
        return this.toJSON().toString(2);
    }

    
    public long getID() {
        return this.id;
    }
    
    private final static Pattern SPACE_PATTERN = Pattern.compile(" ");
    
    /**
     * if no keys are given, we compute them from the given phrases
     * @param phrases
     * @return
     */
    private static JSONArray computeKeysFromPhrases(List<SusiPhrase> phrases) {
        Set<String> t = new LinkedHashSet<>();
        
        // create a list of token sets from the phrases
        List<Set<String>> ptl = new ArrayList<>();
        final AtomicBoolean needsCatchall = new AtomicBoolean(false);
        phrases.forEach(phrase -> {
            Set<String> s = new HashSet<>();
            for (String token: SPACE_PATTERN.split(phrase.getPattern().toString())) {
                String m = SusiPhrase.extractMeat(token.toLowerCase());
                if (m.length() > 1) s.add(m);
            }
            // if there is no meat inside, it will not be possible to access the intent without the catchall intent, so remember that
            if (s.size() == 0) needsCatchall.set(true);
            
            ptl.add(s);
        });
        
        // this is a kind of emergency case where we need a catchall intent because otherwise we cannot access one of the phrases
        JSONArray a = new JSONArray();
        if (needsCatchall.get()) return a.put(CATCHALL_KEY);
        
        // collect all token
        ptl.forEach(set -> set.forEach(token -> t.add(token)));
        
        // if no tokens are available, return the catchall key
        if (t.size() == 0) return a.put(CATCHALL_KEY);
        
        // make a copy to make it possible to use the original key set again
        Set<String> tc = new LinkedHashSet<>();
        t.forEach(c -> tc.add(c));
        
        // remove all token that do not appear in all phrases
        ptl.forEach(set -> {
            Iterator<String> i = t.iterator();
            while (i.hasNext()) if (!set.contains(i.next())) i.remove();
        });

        // if no token is left, use the original tc set and add all keys
        if (t.size() == 0) {
            tc.forEach(c -> a.put(c));
            return a;
        }
        
        // use only the first token, because that appears in all the phrases
        return new JSONArray().put(t.iterator().next());
    }
    
    /**
     * To simplify the check weather or not a intent could be applicable, a key set is provided which
     * must match with input tokens literally. This key check prevents too large numbers of phrase checks
     * thus increasing performance.
     * @return the keys which must appear in an input to allow that this intent can be applied
     */
    public Set<String> getKeys() {
        return this.keys;
    }
    
    /**
     * An intent may have a comment which describes what the intent means. It never has any computational effect.
     * @return the intent comment
     */
    public String getComment() {
        return this.comment;
    }

    public Score getScore() {
        if (this.score != null) return score;
        this.score = new Score();
        return this.score;
    }
    
    /**
     * The score is used to prefer one intent over another if that other intent has a lower score.
     * The reason that this score is used is given by the fact that we need intents which have
     * fuzzy phrase definitions and several intents might be selected because these fuzzy phrases match
     * on the same input sequence. One example is the catch-all intent which fires always but has
     * lowest priority.
     * In the context of artificial mind modeling the score plays the role of a positive emotion.
     * If the AI learns that a intent was applied and caused a better situation (see also: game playing gamefield
     * evaluation) then the intent might get the score increased. Having many intents which have a high score
     * therefore might induce a 'good feeling' because it is known that the outcome will be good.
     * @return a score which is used for sorting of the intents. The higher the better. Highest score wins.
     */
    public class Score {

        public int score;
        public String log;
        
        public Score() {
        if (SusiIntent.this.score != null) return;
        
        /*
         * Score Computation:
         * see: https://github.com/loklak/loklak_server/issues/767
         * Criteria:

         * (1) the conversation plan:
         * purpose: {answer, question, reply} purpose would have to be defined
         * The purpose can be computed using a pattern on the answer expression: is there a '?' at the end, is it a question. Is there also a '. ' (end of sentence) in the text, is it a reply.

         * (2) the existence of a pattern where we decide between prior and minor intents
         * pattern: {false, true} with/without pattern could be computed from the intent string
         * all intents with pattern are ordered in the middle between prior and minor
         * this is combined with
         * prior: {false, true} overruling (prior=true) or default (prior=false) would have to be defined
         * The prior attribute can also be expressed as an replacement of a pattern type because it is only relevant if the query is not a pattern or regular expression.
         * The resulting criteria is a property with three possible values: {minor, pattern, major}
    
         * (3) the meatsize (number of characters that are non-patterns)
    
         * (4) the whole size (total number of characters)
    
         * (5) the operation type
         * op: {retrieval, computation, storage} the operation could be computed from the intent string

         * (6) the IO activity (-location)
         * io: {remote, local, ram} the storage location can be computed from the intent string
    
         
         * (7) finally the subscore can be assigned manually
         * subscore a score in a small range which can be used to distinguish intents within the same categories
         */
        
        // extract the score
        this.score = 0;

        // (1) conversation plan from the answer purpose
        final AtomicInteger dialogType_subscore = new AtomicInteger(0);
        SusiIntent.this.actions.forEach(action -> dialogType_subscore.set(Math.max(dialogType_subscore.get(), action.getDialogType().getSubscore())));
        this.score = this.score * SusiAction.DialogType.values().length + dialogType_subscore.get();
         
        // (2) pattern score
        final AtomicInteger phrases_subscore = new AtomicInteger(0);
        SusiIntent.this.phrases.forEach(phrase -> phrases_subscore.set(Math.min(phrases_subscore.get(), phrase.getSubscore())));
        this.score = this.score * SusiPhrase.Type.values().length + phrases_subscore.get();

        // (3) meatsize: length of a phrase (counts letters)
        final AtomicInteger phrases_meatscore = new AtomicInteger(0);
        SusiIntent.this.phrases.forEach(phrase -> phrases_meatscore.set(Math.max(phrases_meatscore.get(), phrase.getMeatsize())));
        this.score = this.score * 100 + phrases_meatscore.get();
        
        // (4) whole size: length of the pattern
        final AtomicInteger phrases_wholesize = new AtomicInteger(0);
        SusiIntent.this.phrases.forEach(phrase -> phrases_wholesize.set(Math.max(phrases_wholesize.get(), phrase.getPattern().toString().length())));
        this.score = this.score * 100 + phrases_wholesize.get();
     
        // (5) operation type - there may be no operation at all
        final AtomicInteger inference_subscore = new AtomicInteger(0);
        SusiIntent.this.inferences.forEach(inference -> inference_subscore.set(Math.max(inference_subscore.get(), inference.getType().getSubscore())));
        this.score = this.score * (1 + SusiInference.Type.values().length) + inference_subscore.get();
        
        // (6) subscore from the user
        this.score += this.score * 1000 + Math.min(1000, SusiIntent.this.user_subscore);
        
        this.log = 
                "dialog=" + dialogType_subscore.get() +
                ", phrase=" + phrases_subscore.get() +
                ", inference=" + inference_subscore.get() +
                ", meatscore=" + phrases_meatscore.get() +
                ", wholesize=" + phrases_wholesize.get() +
                ", subscore=" + user_subscore +
                ", pattern=" + phrases.get(0).toString() + (SusiIntent.this.inferences.size() > 0 ? (", inference=" + SusiIntent.this.inferences.get(0).getExpression()) : "");
        }
    }

    /**
     * The phrases of an intent are the matching intents which must apply to make it possible that the phrase is applied.
     * This returns the phrases of the intent.
     * @return the phrases of the intent. The intent fires if ANY of the phrases apply
     */
    public List<SusiPhrase> getPhrases() {
        return this.phrases;
    }
    
    /**
     * The inferences of a intent are a set of operations that are applied if the intent is selected as response
     * mechanism. The inferences are feeded by the matching parts of the phrases to have an initial data set.
     * Inferences are lists because they represent a set of lambda operations on the data stream. The last
     * Data set is the response. The stack of data sets which are computed during the inference processing
     * is the thought argument, a list of thoughts in between of the inferences.
     * @return the (ordered) list of inferences to be applied for this intent
     */
    public List<SusiInference> getInferences() {
        return this.inferences;
    }

    /**
     * Actions are operations that are activated when inferences terminate and something should be done with the
     * result. Actions describe how data should be presented, i.e. painted in graphs or just answer lines.
     * Because actions may get changed during computation, we return a clone here
     * @return a list of possible actions. It might be possible to use only a subset, but it is recommended to activate all of them
     */
    public List<SusiAction> getActionsClone() {
        List<SusiAction> clonedList = new ArrayList<>();
        this.actions.forEach(action -> {
            JSONObject actionJson = action.toJSONClone();
            if (this.language != SusiLanguage.unknown) actionJson.put("language", this.language.name());
            clonedList.add(new SusiAction(actionJson));
        });
        return clonedList;
    }

    /**
     * The matcher of a intent is the result of the application of the intent's phrases,
     * the pattern which allow to apply the intent
     * @param s the string which should match
     * @return a matcher on the intent phrases
     */
    public Collection<Matcher> matcher(String s) {
        List<Matcher> l = new ArrayList<>();
        s = s.toLowerCase();
        for (SusiPhrase p: this.phrases) {
            Matcher m = p.getPattern().matcher(s);
            if (new TimeoutMatcher(m).find()) {
                //System.out.println("MATCHERGROUP=" + m.group().toString());
                l.add(m); // TODO: exclude double-entries
            }
        }
        return l;
    }

    /**
     * If a intent is applied to an input stream, it must follow a specific process which is implemented
     * in this consideration method. It is called a consideration in the context of an AI process which
     * tries different procedures to get the optimum result, thus considering different intents.
     * @param query the user input
     * @param token the key from the user query which matched the intent tokens (also considering category matching)
     * @return the result of the application of the intent, a thought argument containing the thoughts which terminated into a final mindstate or NULL if the consideration should be rejected
     */
    public SusiArgument consideration(final String query, SusiThought recall, SusiReader.Token token, SusiMind mind, String client) {
        
        // we start with the recall from previous interactions as new flow
        final SusiArgument flow = new SusiArgument().think(recall);
        
        // that argument is filled with an idea which consist of the query where we extract the identified data entities
        alternatives: for (Matcher matcher: this.matcher(query)) {
            if (!new TimeoutMatcher(matcher).matches()) continue;
            SusiThought keynote = new SusiThought(matcher);
            if (token != null) {
                keynote.addObservation("token_original", token.original);
                keynote.addObservation("token_canonical", token.canonical);
                keynote.addObservation("token_categorized", token.categorized);
            }
            DAO.log("Susi has an idea: on " + keynote.toString() + " apply " + this.toJSON());
            flow.think(keynote);
            
            // lets apply the intents that belong to this specific consideration
            for (SusiInference inference: this.getInferences()) {
                SusiThought implication = inference.applyProcedures(flow);
                DAO.log("Susi is thinking about: " + implication.toString());
                // make sure that we are not stuck:
                // in case that we are stuck (== no progress was made) we terminate and return null
                if ((flow.mindstate().equals(implication) || implication.isFailed())) continue alternatives; // TODO: do this only if specific marker is in intent
                // think
                flow.think(implication);
            }
            
            // we deduced thoughts from the inferences in the intents. Now apply the actions of intent to produce results
            this.getActionsClone().forEach(action -> flow.addAction(action/*.execution(flow, mind, client)*/));
            
            // add skill source
            if (this.skill != null && this.skill.length() > 0) flow.addSkill(this.skill);
            
            return flow;
        }
        // fail, no alternative was successful
        return null;
    }
    
}
