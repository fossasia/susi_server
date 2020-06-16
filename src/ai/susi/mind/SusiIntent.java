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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.mind.SusiAction.RenderType;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiPattern.SusiMatcher;

/**
 * An intent in the Susi AI framework is a collection of utterances, inference processes and actions that are applied
 * on external sense data if the utterances identify that this intent set would be applicable on the sense data.
 * A set of intents is called a skill; a skill describe how to handle activities from the outside of the AI and react on
 * such activities.
 */
public class SusiIntent implements Cloneable {

    public final static String CATCHALL_KEY = "*";
    public final static int    DEFAULT_SCORE = 10;

    private List<SusiUtterance> utterances;
    private List<SusiInference> inferences;
    private List<SusiAction> actions;
    private Set<String> cues;
    private Set<String> keys;
    private String comment;
    private int user_subscore;
    private Score score;
    private int hashCode, depth;
    private SusiSkill.ID skillid;
    private String example, expect, label, implication;

    public SusiIntent() {
        this.utterances = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.inferences = new ArrayList<>();
        this.keys = new HashSet<>();
        this.cues = new LinkedHashSet<>();
        this.user_subscore = DEFAULT_SCORE;
        this.score = null; // calculate this later if required
        this.comment = "";
        this.skillid = null;
        this.example = "";
        this.expect = "";
        this.label = "";
        this.implication = "";
        this.hashCode = 0; // set with lazy computation
        this.depth = 0;
    }

    public Object clone() {
        SusiIntent i = new SusiIntent();
        this.utterances.forEach(u -> i.utterances.add(u));
        this.actions.forEach(a -> i.actions.add(a));
        this.inferences.forEach(f -> i.inferences.add(f));
        this.keys.forEach(k -> i.keys.add(k));
        this.cues.forEach(c -> i.cues.add(c));
        this.user_subscore = i.user_subscore;
        i.score = this.score;
        i.comment = this.comment;
        i.skillid = this.skillid;
        i.example = this.example;
        i.expect = this.expect;
        i.label = this.label;
        i.implication = this.implication;
        i.hashCode = this.hashCode;
        i.depth = this.depth;
        return i;
    }

    /**
     * Create an intent by parsing of the intent description
     * @param json the intent description
     * @throws PatternSyntaxException
     */
    private SusiIntent(SusiSkill.ID skillid, JSONObject json) throws PatternSyntaxException {
        assert skillid != null;

        // extract the utterances and the utterances subscore
        if (!json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        JSONArray p = (JSONArray) json.get("phrases");
        this.utterances = new ArrayList<>(p.length());
        p.forEach(q -> this.utterances.add(new SusiUtterance((JSONObject) q)));

        // extract the actions and the action subscore
        if (!json.has("actions")) throw new PatternSyntaxException("actions missing", "", 0);
        p = (JSONArray) json.get("actions");
        this.actions = new ArrayList<>(p.length());
        p.forEach(a -> {
            try {
                SusiAction action = new SusiAction((JSONObject) a);
                this.actions.add(action);
            } catch (SusiActionException e) {
                DAO.severe("invalid action - " + json.toString() + ": " + ((JSONObject) a).toString(0));
            }
        });

        // extract the inferences and the process subscore; there may be no inference at all
        if (json.has("process")) {
            p = (JSONArray) json.get("process");
            this.inferences = new ArrayList<>(p.length());
            p.forEach(q -> this.inferences.add(new SusiInference((JSONObject) q)));
        } else {
            this.inferences = new ArrayList<>(0);
        }

        // extract and compute the keys; there may be none key given, then they will be computed
        this.keys = new HashSet<>();
        JSONArray k;
        if (json.has("keys")) {
            k = json.getJSONArray("keys");
            k.forEach(o -> this.keys.add((String) o));
        }
        k = computeKeysFromUtterance(this.utterances);

        k.forEach(o -> this.keys.add((String) o));

        this.cues = new LinkedHashSet<>();
        this.user_subscore = json.has("score") ? json.getInt("score") : DEFAULT_SCORE;
        this.score = null; // calculate this later if required

        // extract the comment
        this.comment = json.has("comment") ? json.getString("comment") : "";

        // remember the origin
        this.skillid = skillid;

        // quality control
        this.example = json.has("example") ? json.getString("example") : "";
        this.expect = json.has("expect") ? json.getString("expect") : "";
        if (label == null || label.length() == 0 && this.utterances.size() == 1) {
            String l = this.utterances.get(0).getPattern().toString().replaceAll(" ", "_");
            if (l.indexOf('*') < 0) label = l;
        }
        this.label = (label != null && label.length() > 0) ? label : "";
        this.hashCode = 0; // set with lazy computation
        this.depth = json.has("depth") ? json.getInt("depth") : 0;
    }

    public SusiIntent(
            List<SusiUtterance> utterances,
            boolean prior,
            int depth,
            SusiSkill.ID skillid) throws SusiActionException {
        assert skillid != null;
        if (utterances == null) throw new SusiActionException("utterances is null in " + skillid.getPath());
        if (utterances.size() == 0) throw new SusiActionException("utterances size is 0 in " + skillid.getPath());

        this.utterances = new ArrayList<>();
        for (SusiUtterance u: utterances) this.utterances.add(u);
        this.actions = new ArrayList<>();
        this.inferences = new ArrayList<>();

        this.keys = new HashSet<>();
        JSONArray k = computeKeysFromUtterance(this.utterances);
        k.forEach(o -> this.keys.add((String) o));

        this.cues = new LinkedHashSet<>();
        this.user_subscore = DEFAULT_SCORE;
        this.score = null; // calculate this later if required
        this.comment = "";
        this.skillid = skillid;
        this.example = "";
        this.expect = "";
        if ((this.label == null || this.label.length() == 0) && this.utterances.size() == 1) try {
            String l = this.utterances.get(0).getPattern().toString();
            if (l.indexOf('*') < 0) this.label = l.replace(' ', '_');
        } catch (IndexOutOfBoundsException e) {
            throw new SusiActionException(e.toString() + " in " + skillid.toString());
        }
        this.label = (this.label != null && this.label.length() > 0) ? this.label : "";
        this.implication = "";
        this.hashCode = 0; // will be computed later
        this.depth = depth;
    }

    public SusiIntent setExample(String example) {
        this.example = example;
        return this;
    }

    public SusiIntent setExpect(String expect) {
        this.expect = expect;
        return this;
    }

    public SusiIntent setLabel(String label) {
        this.label = label;
        return this;
    }

    public SusiIntent setImplication(String implication) {
        this.implication = implication;
        return this;
    }

    public SusiIntent setSkillid(SusiSkill.ID skillid) {
        this.skillid = skillid;
        return this;
    }

    public SusiIntent setUser_subscore(int user_subscore) {
        this.user_subscore = user_subscore;
        return this;
    }

    public SusiIntent setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public SusiIntent setUtterances(List<SusiUtterance> utterances) {
        this.utterances = utterances;
        // extract (or compute) the keys; there may be none key given, then they will be computed
        this.keys = new HashSet<>();
        JSONArray k = computeKeysFromUtterance(this.utterances);
        k.forEach(o -> this.keys.add((String) o));
        return this;
    }
    
    public SusiIntent addInference(SusiInference inference) {
        this.inferences.add(inference);
        return this;
    }

    public SusiIntent setInferences(List<SusiInference> inferences) {
        this.inferences = inferences;
        return this;
    }

    public SusiIntent addInferences(SusiInference... inferences) {
        for (int i = 0; i < inferences.length; i++) this.inferences.add(inferences[i]);
        return this;
    }

    public SusiIntent addUtterances(SusiUtterance... utterances) {
        for (int i = 0; i < utterances.length; i++) this.utterances.add(utterances[i]);
        return this;
    }

    public SusiIntent addUtterances(List<SusiUtterance> utterances) {
        utterances.forEach(utterance -> this.utterances.add(utterance));
        return this;
    }

    public SusiIntent setActions(List<SusiAction> actions) {
        this.actions = actions;
        return this;
    }

    public SusiIntent addAction(SusiAction action) {
        this.actions.add(action);
        return this;
    }

    public SusiIntent addActions(SusiAction... actions) {
        for (int i = 0; i < actions.length; i++) this.actions.add(actions[i]);
        return this;
    }

    public SusiIntent addActions(List<SusiAction> actions) {
        actions.forEach(action -> this.actions.add(action));
        return this;
    }

    public SusiIntent addCues(String... cues) {
        addloop: for (int i = 0; i < cues.length; i++) {
            // check for double entries
            for (String c: this.cues) {
                if (c.equals(cues[i])) continue addloop;
            }
            this.cues.add(cues[i]);
        }
        return this;
    }

    public SusiIntent addCues(List<String> cues) {
        cues.forEach(cue -> this.addCues(cue));
        return this;
    }

    public Set<String> getCues() {
        return this.cues;
    }

    public SusiIntent setDepth(int d) {
        this.depth = d;
        return this;
    }

    public SusiSkill.ID getSkillID() {
        return this.skillid;
    }

    public int getDepth() {
        return this.depth;
    }

    public String getExpect() {
        return this.expect == null || this.expect.length() == 0 ? null : this.expect;
    }

    public boolean hasExample() {
        return this.example != null && this.example.length() > 0;
    }

    public String getExample() {
        return this.example == null || this.example.length() == 0 ? null : this.example;
    }

    @Override
    public int hashCode() {
        if (this.hashCode != 0) return this.hashCode;
        StringBuilder sb = new StringBuilder();
        sb.append(this.skillid == null ? "" : this.skillid.getPath());
        sb.append(this.utterances.toString());
        sb.append(this.inferences.toString());
        sb.append(this.actions.toString());
        this.hashCode = sb.toString().hashCode();
        return this.hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SusiIntent)) return false;
        SusiIntent si = (SusiIntent) o;
        if (this.skillid == null && si.skillid != null) return false;
        if (this.skillid != null && si.skillid == null) return false;
        if (this.skillid != null && si.skillid != null && !this.skillid.equals(si.skillid)) return false;
        if (!this.utterances.toString().equals(si.utterances.toString())) return false;
        if (!this.inferences.toString().equals(si.inferences.toString())) return false;
        if (!this.actions.toString().equals(si.actions.toString())) return false;
        return true;
    }

    public String toString() {
        return this.toJSON().toString(2);
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        json.put("id", this.hashCode());
        json.put("depth", this.depth);
        if (this.score != null) json.put("score", this.score.score);
        if (this.skillid != null && this.skillid.getPath().length() > 0) {
            json.put("skill_source", this.skillid.getPath());
            json.put("skill_link", SusiCognition.getSkillLink(this.skillid.getPath()));
            json.put("skill_line", this.utterances.iterator().next().getLine());
        }
        if (this.keys != null && this.keys.size() > 0) json.put("keys", new JSONArray(this.keys));
        JSONArray phrases = new JSONArray();
        this.utterances.forEach(utterance -> phrases.put(utterance.toJSON()));
        json.put("phrases", phrases);
        JSONArray process = new JSONArray();
        this.inferences.forEach(inference -> process.put(inference.getJSON()));
        json.put("process", process);
        JSONArray a = new JSONArray(); this.getActionsClone().forEach(action ->a.put(action.toJSONClone()));
        json.put("actions", a);
        JSONArray c = new JSONArray(); this.cues.forEach(cue -> c.put(cue));
        json.put("cues", c);
        if (this.comment != null && this.comment.length() > 0) json.put("comment", this.comment);
        if (this.example != null && this.example.length() > 0) json.put("example", example);
        if (this.expect != null && this.expect.length() > 0) json.put("expect", expect);
        if (this.label != null && this.label.length() > 0) json.put("label", label);
        if (this.implication != null && this.implication.length() > 0) json.put("implication", implication);
        return json;
    }

    public String toLoT() {
        StringBuilder sb = new StringBuilder();

        // utterances
        for (int i = 0; i < this.utterances.size(); i++) {
            SusiPattern pattern = this.utterances.get(i).getPattern();
            sb.append(pattern.toLoT());
            if (i < this.utterances.size() - 1) sb.append(" | ");
        }
        sb.append("\n");

        // actions
        for (SusiAction action: this.actions) {
            if (action.getRenderType() == RenderType.answer) {
                List<String> phrases = action.getPhrases();
                for (int i = 0; i < phrases.size(); i++) {
                    sb.append(phrases.get(i));
                    if (i < phrases.size() - 1) sb.append(" | ");
                }
            }
        }
        sb.append("\n\n");

        return sb.toString();
    }

    /**
     * Generate a set of intents from a single intent definition. This may be possible if the intent contains an 'options'
     * object which creates a set of intents, one for each option. The options combine with one set of utterances
     * @param json - a multi-intent definition
     * @return a set of intents
     */
    public static List<SusiIntent> getIntents(final SusiSkill.ID skillid, final JSONObject json) {
        if (!json.has("phrases")) {
            throw new PatternSyntaxException("phrases missing", "", 0);
        }
        final List<SusiIntent> intents = new ArrayList<>();
        if (json.has("options")) {
            JSONArray options = json.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = new JSONObject();
                option.put("phrases", json.get("phrases"));
                JSONObject or = options.getJSONObject(i);
                for (String k: or.keySet()) option.put(k, or.get(k));
                intents.add(new SusiIntent(skillid, option));
            }
        } else {
            try {
                SusiIntent intent = new SusiIntent(skillid, json);
                intents.add(intent);
            } catch (PatternSyntaxException e) {
                Logger.getLogger("SusiIntent").warning("Regular Expression error (\"" + e.getPattern() + "\") in Susi Intent " + skillid.getPath() + ": \n" + json.toString(2));
            }
        }
        return intents;
    }

    /**
     * @deprecated use class constructor instead
     */
    public static JSONObject answerIntent(
            String[] utterances,
            String condition,
            String[] answers,
            boolean prior,
            int depth,
            String example,
            String expect,
            String label,
            String implication,
            SusiLanguage language) {
        JSONObject intent = new JSONObject(true);

        // write utterances
        JSONArray p = new JSONArray();
        intent.put("phrases", p);
        for (String utterance: utterances) {
            JSONObject simplePhrase = SusiUtterance.simplePhrase(utterance.trim(), prior);
            p.put(simplePhrase);
        }

        // write conditions (if any)
        if (condition != null && condition.length() > 0) {
            JSONArray c = new JSONArray();
            intent.put("process", c);
            c.put(new SusiInference(condition, SusiInference.Type.memory, 0).getJSON());
        }

        // quality control
        if (example != null && example.length() > 0) intent.put("example", example);
        if (expect != null && expect.length() > 0) intent.put("expect", expect);
        // in case that the utterances has only one alternative and does not contain any pattern, the label can be computed from it
        if ((label == null || label.length() == 0) && utterances.length == 1) {
            String l = utterances[0].replaceAll(" ", "_");
            if (l.indexOf('*') < 0) label = l;
        }
        if (label != null && label.length() > 0) intent.put("label", label);
        if (implication != null && implication.length() > 0) intent.put("implication", implication);
        intent.put("depth", depth);
        // write actions
        JSONArray a = new JSONArray();
        intent.put("actions", a);
        a.put(SusiAction.answerAction(0, language, answers));
        return intent;
    }

    /**
     * if no keys are given, we compute them from the given utterances
     * @param utterances
     * @return
     */
    private static JSONArray computeKeysFromUtterance(List<SusiUtterance> utterances) {
        Set<String> t = new LinkedHashSet<>();

        // create a list of token sets from the utterances
        List<Set<String>> ptl = new ArrayList<>();
        final AtomicBoolean needsCatchall = new AtomicBoolean(false);
        utterances.forEach(utterance -> {
            Set<String> s = new HashSet<>();
            for (String token: utterance.getPattern().token()) {
                String m = SusiUtterance.extractMeat(token.toLowerCase());
                if (m.length() > 1) s.add(m);
            }
            // if there is no meat inside, it will not be possible to access the intent without the catchall intent, so remember that
            if (s.size() == 0) needsCatchall.set(true);

            ptl.add(s);
        });

        // this is a kind of emergency case where we need a catchall intent because otherwise we cannot access one of the utterances
        JSONArray a = new JSONArray();
        if (needsCatchall.get()) return a.put(CATCHALL_KEY);

        // collect all token
        ptl.forEach(set -> set.forEach(token -> t.add(token)));

        // if no tokens are available, return the catchall key
        if (t.size() == 0) return a.put(CATCHALL_KEY);

        // make a copy to make it possible to use the original key set again
        Set<String> tc = new LinkedHashSet<>();
        t.forEach(c -> tc.add(c));

        // remove all token that do not appear in all utterances
        ptl.forEach(set -> {
            Iterator<String> i = t.iterator();
            while (i.hasNext()) if (!set.contains(i.next())) i.remove();
        });

        // if no token is left, use the original tc set and add all keys
        if (t.size() == 0) {
            tc.forEach(c -> a.put(c));
            return a;
        }

        // use only the first token, because that appears in all the utterances
        return new JSONArray().put(t.iterator().next());
    }

    /**
     * To simplify the check weather or not a intent could be applicable, a key set is provided which
     * must match with input tokens literally. This key check prevents too large numbers of utterance checks
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

    /**
     * The utterances of an intent are the matching intents which must apply to make it possible that the utterance is applied.
     * This returns the utterances of the intent.
     * @return the utterances of the intent. The intent fires if ANY of the utterances apply
     */
    public List<SusiUtterance> getUtterances() {
        return this.utterances;
    }

    /**
     * getting a untterance sample can be used for debugging.
     * @return a String containing the regular expression of the utterances
     */
    public String getUtterancesSample() {
        StringBuilder sb = new StringBuilder();
        for (SusiUtterance utterance: this.utterances) {
            sb.append(" | ").append(utterance.getPattern().toString());
        }
        return sb.length() == 0 ? "" : sb.toString().substring(3);
    }

    /**
     * The inferences of a intent are a set of operations that are applied if the intent is selected as response
     * mechanism. The inferences are feeded by the matching parts of the utterances to have an initial data set.
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
        this.actions.forEach(a -> {
            JSONObject actionJson = a.toJSONClone();
            try {
                SusiAction action = new SusiAction(actionJson);
                clonedList.add(action);
            } catch (SusiActionException e) {
                DAO.severe("invalid action - " + e.getMessage() + ": " + actionJson.toString(0));
            }
        });
        return clonedList;
    }

    /**
     * The matcher of a intent is the result of the application of the intent's utterances,
     * the pattern which allow to apply the intent
     * @param s the string which should match
     * @return a matcher on the intent utterances
     */
    public LinkedHashSet<SusiMatcher> matcher(String s) {
        LinkedHashSet<SusiMatcher> l = new LinkedHashSet<>();
        s = s.toLowerCase();
        for (SusiUtterance p: this.utterances) {
            SusiMatcher m = p.getPattern().matcher(s);
            if (m.matches()) {
                //System.out.println("MATCHERGROUP=" + m.group().toString());
                l.add(m);
            }
        }
        return l;
    }

    /**
     * get the intent score
     * @param expression the user expression where we are looking for an answer
     * @param language this is the language the user is speaking
     * @return an intent score: the higher, the better; null if the expression cannot be matched
     */
    public Score getScore(SusiLanguage language) {
        if (this.score != null) return score;
        this.score = new Score(language);
        if (this.score.score == Integer.MIN_VALUE) this.score = null;
        return this.score;
    }

    /**
     * The score is used to prefer one intent over another if that other intent has a lower score.
     * The reason that this score is used is given by the fact that we need intents which have
     * fuzzy utterance definitions and several intents might be selected because these fuzzy utterances match
     * on the same input sequence. One example is the catch-all intent which fires always but has
     * lowest priority.
     * In the context of artificial mind modeling the score plays the role of a positive emotion.
     * If the AI learns that a intent was applied and caused a better situation (see also: game playing gamefield
     * evaluation) then the intent might get the score increased. Having many intents which have a high score
     * therefore might induce a 'good feeling' because it is known that the outcome will be good.
     * @param expression the user expression where we are looking for an answer
     * @param language this is the language the user is speaking
     * @return a score which is used for sorting of the intents. The higher the better. Highest score wins.
     */
    public class Score {

        public long score;
        public String log;

        public Score(SusiLanguage userLanguage) {
        if (SusiIntent.this.score != null) return;

        /*
         * Score Computation:
         * see: https://github.com/loklak/loklak_server/issues/767
         * Criteria:

         * (0) the language
         * We do not want to switch skills for languages because people sometimes
         * speak several languages. Therefore we use a likelihood that someone who speaks language A
         * also speaks language B. 

         * (1) the existence of a pattern where we decide between prior and minor intents
         * pattern: {false, true} with/without pattern could be computed from the intent string
         * all intents with pattern are ordered in the middle between prior and minor
         * this is combined with
         * prior: {false, true} overruling (prior=true) or default (prior=false) would have to be defined
         * The prior attribute can also be expressed as an replacement of a pattern type because it is only relevant if the query is not a pattern or regular expression.
         * The resulting criteria is a property with three possible values: {minor, pattern, major}

         * (2) the meatsize (number of characters that are non-patterns)

         * (3) the whole size (total number of characters)

         * (4) the conversation plan:
         * purpose: {answer, question, reply} purpose would have to be defined
         * The purpose can be computed using a pattern on the answer expression: is there a '?' at the end, is it a question. Is there also a '. ' (end of sentence) in the text, is it a reply.

         * (5) the operation type
         * op: {retrieval, computation, storage} the operation could be computed from the intent string

         * (6) the IO activity (-location)
         * io: {remote, local, ram} the storage location can be computed from the intent string

         * (7) finally the subscore can be assigned manually
         * subscore a score in a small range which can be used to distinguish intents within the same categories
         */

        // compute the score

        // (0) language
        final int language_subscore = SusiIntent.this.skillid == null ? 0 : (int) (100 * SusiIntent.this.skillid.language().likelihoodCanSpeak(userLanguage));
        this.score = language_subscore;

        // (1) pattern score
        // (2) meatsize: length of a utterance (counts letters)
        // (3) whole size: length of the pattern
        //final AtomicInteger utterances_subscore = new AtomicInteger(Integer.MAX_VALUE);
        //final AtomicInteger utterances_meatscore = new AtomicInteger(Integer.MAX_VALUE);
        final AtomicInteger utterances_wholesize = new AtomicInteger(Integer.MAX_VALUE);
        SusiIntent.this.utterances.forEach(utterance -> utterances_wholesize.set(Math.min(utterances_wholesize.get(), utterance.getPattern().toString().length())));
        /*
        for (SusiUtterance utterance : SusiIntent.this.utterances) {
            SusiPattern t = utterance.getPattern();
            SusiMatcher m = t.matcher(expression);
            if (!m.matches()) continue;

            utterances_subscore.set(Math.min(utterances_subscore.get(), utterance.getSubscore()));
            utterances_meatscore.set(Math.min(utterances_meatscore.get(), utterance.getMeatsize()));
            String p = utterance.getPattern().toString();
            utterances_wholesize.set(Math.min(utterances_wholesize.get(), p.length()));
        }
        if (utterances_subscore.get() == Integer.MAX_VALUE ||
            utterances_meatscore.get() == Integer.MAX_VALUE ||
            utterances_wholesize.get() == Integer.MAX_VALUE) {
            // no match of any utterance, do not consider this
            this.score = Integer.MIN_VALUE;
            this.log = "";
            return;
        }
        this.score = this.score * SusiUtterance.Type.values().length + utterances_subscore.get();
        this.score = this.score * 100 + utterances_meatscore.get();
        */
        this.score = this.score * 100 + utterances_wholesize.get();

        // (4) conversation plan from the answer purpose
        final AtomicInteger dialogType_subscore = new AtomicInteger(Integer.MAX_VALUE);
        if (!(utterances.size() == 1 && utterances.get(0).equals("(.*)"))) {
            SusiIntent.this.actions.forEach(action -> dialogType_subscore.set(Math.min(dialogType_subscore.get(), action.getDialogType().getSubscore())));
        }
        this.score = this.score * SusiAction.DialogType.values().length + dialogType_subscore.get();

        // (5) action render type score
        final AtomicInteger actionRenderType_subscore = new AtomicInteger(0);
        SusiIntent.this.actions.forEach(action -> actionRenderType_subscore.set(Math.max(actionRenderType_subscore.get(), action.getRenderType().getScore())));
        this.score = this.score * 256 + actionRenderType_subscore.get();

        // (6) operation type - there may be no operation at all
        final AtomicInteger inference_subscore = new AtomicInteger(0);
        SusiIntent.this.inferences.forEach(inference -> inference_subscore.set(Math.max(inference_subscore.get(), inference.getType().getSubscore())));
        this.score = this.score * (1 + SusiInference.Type.values().length) + inference_subscore.get();

        // (7) subscore from the user
        this.score += this.score * 1000 + Math.min(1000, SusiIntent.this.user_subscore);

        this.log = 
                "language=" + language_subscore +
                //", utterance=" + utterances_subscore.get() +
                //", meatscore=" + utterances_meatscore.get() +
                ", wholesize=" + utterances_wholesize.get() +
                ", dialog=" + dialogType_subscore.get() +
                ", inference=" + inference_subscore.get() +
                ", subscore=" + user_subscore +
                ", pattern=" + (utterances.size() > 0 ? utterances.get(0).toString() : "") + (SusiIntent.this.inferences.size() > 0 ? (", inference=" + SusiIntent.this.inferences.get(0).getExpression()) : "");

        //System.out.println("*** " + SusiIntent.this.utterances + ": " + this.log);
        }
    }

}
