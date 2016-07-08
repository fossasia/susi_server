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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private int score;

    /**
     * Create a rule by parsing of the rule description
     * @param json the rule description
     * @throws PatternSyntaxException
     */
    public SusiRule(JSONObject json) throws PatternSyntaxException {
        
        // extract the phrases
        if (!json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        JSONArray p = (JSONArray) json.remove("phrases");
        this.phrases = new ArrayList<>(p.length());
        p.forEach(q -> this.phrases.add(new SusiPhrase((JSONObject) q)));

        // extract the actions
        if (!json.has("actions")) throw new PatternSyntaxException("actions missing", "", 0);
        p = (JSONArray) json.remove("actions");
        this.actions = new ArrayList<>(p.length());
        p.forEach(q -> this.actions.add(new SusiAction((JSONObject) q)));
        
        // extract the inferences; there may be none
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
            if (k.length() == 0) k = computeKeysFromPhrases(this.phrases);
        } else {
            k = computeKeysFromPhrases(this.phrases);
        }
        
        k.forEach(o -> this.keys.add((String) o));
        
        // extract the comment
        this.comment = json.has("comment") ? json.getString("comment") : "";
        
        // extract the score
        this.score = json.has("score") ? json.getInt("score") : DEFAULT_SCORE;
    }
    
    /*
    public String toString() {
        String s = 
            "{\n" +
            "  \"phrases\":[{\"type\": \"pattern\", \"expression\": \"\"}],\n" +
            "  \"actions\":[{\"type\": \"answer\", \"select\": \"random\", \"phrases\": [\n" +
            "  \"\"\n" +
            "  \"]\n" +
            "  \"}]\n" +
            "\"}\";\n";
        return s;
    }
    */
    
    /**
     * if no keys are given, we compute them from the given phrases
     * @param phrases
     * @return
     */
    private static JSONArray computeKeysFromPhrases(List<SusiPhrase> phrases) {
        return new JSONArray().put(CATCHALL_KEY);
    }
    
    /**
     * To simplify the check wether or not a rule could be applicable, a key set is provided which
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
     * @return a list of possible actions. It might be possible to use only a subset, but it is recommended to activate all of them
     */
    public List<SusiAction> getActions() {
        return this.actions;
    }

    /**
     * The matcher of a rule is the result of the application of the rule's phrases, the pattern which allow to
     * fire the rule
     * @param s the string which shozld match
     * @return a matcher on the rule phrases
     */
    public Matcher matcher(String s) {
        s = s.toLowerCase();
        for (SusiPhrase p: this.phrases) {
            Matcher m = p.getPattern().matcher(s);
            if (m.find()) return m;
        }
        return null;
    }

    /**
     * If a rule is applied to an input stream, it must follow a specific process which is implemented
     * in this consideration method. It is called a consideration in the context of an AI process which
     * tries different rules to get the optimum result, thus considering different rules.
     * @param query the user input
     * @return the result of the application of the rule, a thought argument containing the thoughts which terminated into a final mindstate or NULL if the consideration should be rejected
     */
    public SusiArgument consideration(final String query) {
        
        // we start with an empty argument
        final SusiArgument argument = new SusiArgument();
        
        // that argument is filled with an idea which consist of the query where we extract the identified data entities
        SusiThought idea = new SusiThought(this.matcher(query));
        argument.think(idea);
        
        // lets apply the rules that belong to this specific consideration
        for (SusiInference inference: this.getInferences()) {
            SusiThought nextThought = inference.applyon(argument);
            // make sure that we are not stuck
            if (argument.mindstate().equals(nextThought) || nextThought.getCount() == 0) return null; // TODO: do this only if specific marker is in rule
            // think
            argument.think(nextThought);
        }
        
        // we deduced thoughts from the inferences in the rules. Now apply the actions of rule to produce results
        List<SusiAction> actions = new ArrayList<>();
        for (SusiAction action: this.getActions()) {
            actions.add(action.apply(argument));
        }
        argument.mindstate().setActions(actions);
        return argument;
    }
}
