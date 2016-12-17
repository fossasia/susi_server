/**
 *  SusiRule
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;

/**
 * A Rule in the Susi AI framework is a collection of phrases, inference processes and actions that are applied
 * on external sense data if the phrases identify that this rule set would be applicable on the sense data.
 * A set of rules would express 'knowledge' on how to handle activities from the outside of the AI and react on
 * such activities.
 */
public class SusiRule {

    public final static String CATCHALL_KEY = "*";
    public final static int    DEFAULT_SCORE = 10;
    
    private List<SusiPhrase> phrases;
    private List<SusiInference> inferences;
    private List<SusiAction> actions;
    private Set<String> keys;
    private String comment;
    private int user_subscore;
    private int score;
    private int id;
    
    /**
     * Generate a set of rules from a single rule definition. This may be possible if the rule contains an 'options'
     * object which creates a set of rules, one for each option. The options combine with one set of phrases
     * @param json - a multi-rule definition
     * @return a set of rules
     */
    public static List<SusiRule> getRules(JSONObject json) {
        if (!json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        final List<SusiRule> rules = new ArrayList<>();
        if (json.has("options")) {
            JSONArray options = json.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = new JSONObject();
                option.put("phrases", json.get("phrases"));
                JSONObject or = options.getJSONObject(i);
                for (String k: or.keySet()) option.put(k, or.get(k));
                rules.add(new SusiRule(option));
            }
        } else {
            SusiRule rule = new SusiRule(json);
            rules.add(rule);
        }
        return rules;
    }
    
    /**
     * Create a rule by parsing of the rule description
     * @param json the rule description
     * @throws PatternSyntaxException
     */
    private SusiRule(JSONObject json) throws PatternSyntaxException {
        
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
        this.score = -1; // calculate this later if required
        
        // extract the comment
        this.comment = json.has("comment") ? json.getString("comment") : "";

        // calculate the id
        String ids0 = this.actions.toString();
        String ids1 = this.phrases.toString();
        this.id = ids0.hashCode() + ids1.hashCode();
    }
    
    public int hashCode() {
        return this.id;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        json.put("id", id);
        if (this.keys != null && this.keys.size() > 0) json.put("keys", new JSONArray(this.keys));
        JSONArray p = new JSONArray(); this.phrases.forEach(phrase -> p.put(phrase.toJSON()));
        json.put("phrases", p);
        JSONArray i = new JSONArray(); this.inferences.forEach(inference -> i.put(inference.getJSON()));
        json.put("process", i);
        JSONArray a = new JSONArray(); this.actions.forEach(action -> a.put(action.toJSONClone()));
        json.put("actions", a);
        if (this.comment != null && this.comment.length() > 0) json.put("comment", comment);
        if (score > 0) json.put("score", score);
        return json;
    }
    
    public static JSONObject simpleRule(String[] phrases, String condition, String[] answers, boolean prior) {
        JSONObject json = new JSONObject();

        // write phrases
        JSONArray p = new JSONArray();
        json.put("phrases", p);
        for (String phrase: phrases) p.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
        
        // write conditions (if any)
        if (condition != null && condition.length() > 0) {
            JSONArray c = new JSONArray();
            json.put("process", c);
            c.put(SusiInference.simpleMemoryProcess(condition));   
        }
        
        // write actions
        JSONArray a = new JSONArray();
        json.put("actions", a);
        a.put(SusiAction.simpleAction(answers));        
        return json;
    }
    
    public String toString() {
        /*
        String s = 
            "{\n" +
            "  \"phrases\":[{\"type\": \"pattern\", \"expression\": \"\"}],\n" +
            "  \"actions\":[{\"type\": \"answer\", \"select\": \"random\", \"phrases\": [\n" +
            "  \"\"\n" +
            "  \"]\n" +
            "  \"}]\n" +
            "\"}\";\n";
        return s;
        */
        return this.phrases.toString();
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
            // if there is no meat inside, it will not be possible to access the rule without the catchall rule, so remember that
            if (s.size() == 0) needsCatchall.set(true);
            
            ptl.add(s);
        });
        
        // this is a kind of emergency case where we need a catchall rule because otherwise we cannot access one of the phrases
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
     * To simplify the check weather or not a rule could be applicable, a key set is provided which
     * must match with input tokens literally. This key check prevents too large numbers of phrase checks
     * thus increasing performance.
     * @return the keys which must appear in an input to allow that this rule can be applied
     */
    public Set<String> getKeys() {
        return this.keys;
    }
    
    /**
     * A rule may have a comment which describes what the rule means. It never has any computational effect.
     * @return the rule comment
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * The score is used to prefer one rule over another if that other rule has a lower score.
     * The reason that this score is used is given by the fact that we need rules which have
     * fuzzy phrase definitions and several rules might be selected because these fuzzy phrases match
     * on the same input sequence. One example is the catch-all rule which fires always but has
     * lowest priority.
     * In the context of artificial mind modeling the score plays the role of a positive emotion.
     * If the AI learns that a rule was applied and caused a better situation (see also: game playing gamefield
     * evaluation) then the rule might get the score increased. Having many rules which have a high score
     * therefore might induce a 'good feeling' because it is known that the outcome will be good.
     * @return a score which is used for sorting of the rules. The higher the better. Highest score wins.
     */
    public int getScore() {

        if (this.score >= 0) return this.score;
        
        /*
         * Score Computation:
         * see: https://github.com/loklak/loklak_server/issues/767

         * (1) primary criteria is the conversation plan:
         * purpose: {answer, question, reply} purpose would have to be defined
         * The purpose can be computed using a pattern on the answer expression: is there a '?' at the end, is it a question. Is there also a '. ' (end of sentence) in the text, is it a reply.

         * (2) secondary criteria is the existence of a pattern where we decide between prior and minor rules
         * pattern: {false, true} with/without pattern could be computed from the rule string
         * all rules with pattern are ordered in the middle between prior and minor
         * this is combined with
         * prior: {false, true} overruling (prior=true) or default (prior=false) would have to be defined
         * The prior attribute can also be expressed as an replacement of a pattern type because it is only relevant if the query is not a pattern or regular expression.
         * The resulting criteria is a property with three possible values: {minor, pattern, major}
    
         * (3) tertiary criteria is the operation type
         * op: {retrieval, computation, storage} the operation could be computed from the rule string

         * (4) quaternary criteria is the IO activity (-location)
         * io: {remote, local, ram} the storage location can be computed from the rule string
    
         * (5) the meatsize (number of characters that are non-patterns)
    
         * (6) finally the subscore can be assigned manually
         * subscore a score in a small range which can be used to distinguish rules within the same categories
         */
        
        // extract the score
        this.score = 0;

        // (1) conversation plan from the answer purpose
        final AtomicInteger dialogType_subscore = new AtomicInteger(0);
        this.actions.forEach(action -> dialogType_subscore.set(Math.max(dialogType_subscore.get(), action.getDialogType().getSubscore())));
        this.score = this.score * SusiAction.DialogType.values().length + dialogType_subscore.get();
        
        // (2) pattern score
        final AtomicInteger phrases_subscore = new AtomicInteger(0);
        this.phrases.forEach(phrase -> phrases_subscore.set(Math.max(phrases_subscore.get(), phrase.getSubscore())));
        this.score = this.score * SusiPhrase.Type.values().length + phrases_subscore.get();

        // (3) operation type - there may be no operation at all
        final AtomicInteger inference_subscore = new AtomicInteger(0);
        this.inferences.forEach(inference -> inference_subscore.set(Math.max(inference_subscore.get(), inference.getType().getSubscore())));
        this.score = this.score * (1 + SusiInference.Type.values().length) + inference_subscore.get();
        
        // (4) meatsize
        final AtomicInteger phrases_meatscore = new AtomicInteger(0);
        this.phrases.forEach(phrase -> phrases_meatscore.set(Math.max(phrases_meatscore.get(), phrase.getMeatsize())));
        this.score = this.score * 100 + phrases_meatscore.get();
        
        // (6) subscore from the user
        this.score += this.score * 1000 + Math.min(1000, this.user_subscore);
        
        /*
        System.out.println("DEBUG RULE SCORE: id=" + this.id + ", score=" + this.score +
                ", dialog=" + dialogType_subscore.get() +
                ", phrase=" + phrases_subscore.get() +
                ", inference=" + inference_subscore.get() +
                ", meatscore=" + phrases_meatscore.get() +
                ", subscore=" + user_subscore +
                ", pattern=" +phrases.get(0).toString() + (this.inferences.size() > 0 ? ", inference=" + this.inferences.get(0).getExpression() : ""));
       */
        return this.score;
    }

    /**
     * The phrases of a rule are the matching rules which must apply to make it possible that the phrase is applied.
     * This returns the phrases of the rule.
     * @return the phrases of the rule. The rule fires if ANY of the phrases apply
     */
    public List<SusiPhrase> getPhrases() {
        return this.phrases;
    }
    
    /**
     * The inferences of a rule are a set of operations that are applied if the rule is selected as response
     * mechanism. The inferences are feeded by the matching parts of the phrases to have an initial data set.
     * Inferences are lists because they represent a set of lambda operations on the data stream. The last
     * Data set is the response. The stack of data sets which are computed during the inference processing
     * is the thought argument, a list of thoughts in between of the inferences.
     * @return the (ordered) list of inferences to be applied for this rule
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
        this.actions.forEach(action -> clonedList.add(new SusiAction(action.toJSONClone())));
        return clonedList;
    }

    /**
     * The matcher of a rule is the result of the application of the rule's phrases, the pattern which allow to
     * fire the rule
     * @param s the string which shozld match
     * @return a matcher on the rule phrases
     */
    public Collection<Matcher> matcher(String s) {
        List<Matcher> l = new ArrayList<>();
        s = s.toLowerCase();
        for (SusiPhrase p: this.phrases) {
            Matcher m = p.getPattern().matcher(s);
            if (m.find()) {
                //System.out.println("MATCHERGROUP=" + m.group().toString());
                l.add(m); // TODO: exclude double-entries
            }
        }
        return l;
    }

    /**
     * If a rule is applied to an input stream, it must follow a specific process which is implemented
     * in this consideration method. It is called a consideration in the context of an AI process which
     * tries different rules to get the optimum result, thus considering different rules.
     * @param query the user input
     * @param intent the key from the user query which matched the rule keys (also considering category matching)
     * @return the result of the application of the rule, a thought argument containing the thoughts which terminated into a final mindstate or NULL if the consideration should be rejected
     */
    public SusiArgument consideration(final String query, SusiThought recall, SusiReader.Token intent, SusiMind mind, String client) {
        
        // we start with the recall from previous interactions as new flow
        final SusiArgument flow = new SusiArgument().think(recall);
        
        // that argument is filled with an idea which consist of the query where we extract the identified data entities
        alternatives: for (Matcher matcher: this.matcher(query)) {
            if (!matcher.matches()) continue;
            SusiThought keynote = new SusiThought(matcher);
            if (intent != null) {
                keynote.addObservation("intent_original", intent.original);
                keynote.addObservation("intent_canonical", intent.canonical);
                keynote.addObservation("intent_categorized", intent.categorized);
            }
            DAO.log("Susi has an idea: on " + keynote.toString() + " apply " + this.toJSON());
            flow.think(keynote);
            
            // lets apply the rules that belong to this specific consideration
            for (SusiInference inference: this.getInferences()) {
                SusiThought implication = inference.applySkills(flow);
                DAO.log("Susi is thinking about: " + implication.toString());
                // make sure that we are not stuck:
                // in case that we are stuck (== no progress was made) we terminate and return null
                if ((flow.mindstate().equals(implication) || implication.isFailed())) continue alternatives; // TODO: do this only if specific marker is in rule
                // think
                flow.think(implication);
            }
            
            // we deduced thoughts from the inferences in the rules. Now apply the actions of rule to produce results
            this.getActionsClone().forEach(action -> flow.addAction(action/*.apply(flow, mind, client)*/));
            return flow;
        }
        // fail, no alternative was successful
        return null;
    }
    
}
