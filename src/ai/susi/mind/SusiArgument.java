/**
 *  SusiThoughts
 *  Copyright 30.06.2016 by Michael Peter Christen, @0rb1t3r
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.mind.SusiAction.RenderType;
import ai.susi.mind.SusiMind.ReactionException;
import ai.susi.server.ClientIdentity;
import ai.susi.tools.TimeoutMatcher;

/**
 * An Argument is a series of thoughts, also known as a 'proof' in automated reasoning.
 * Within the Susi AI infrastructure this may be considered as the representation of
 * the short-time memory of thinking inside Susi.
 */
public class SusiArgument implements Iterable<SusiThought>, Cloneable {

    // framework information
    private final ClientIdentity identity;
    private final SusiLanguage language;
    private final SusiMind[] minds;

    // working data
    private final ArrayList<SusiThought> recall;
    private final LinkedHashMap<SusiSkill.ID, Integer> skills;

    /**
     * Create an empty argument
     */
    public SusiArgument(ClientIdentity identity, SusiLanguage language, SusiMind... minds) {
        this.identity = identity;
        this.language = language;
        this.minds = minds;
        this.recall = new ArrayList<>();
        this.skills = new LinkedHashMap<>();
    }

    public SusiArgument clone() {
        SusiArgument c = new SusiArgument(this.identity, this.language, this.minds);
        this.recall.forEach(thought -> c.recall.add(thought));
        this.skills.forEach((skill, line) -> c.skills.put(skill, line));
        return c;
    }

    public ClientIdentity getClientIdentity() {
        return this.identity;
    }

    public SusiLanguage getLanguage() {
        return this.language;
    }

    public SusiMind[] getMinds() {
        return this.minds;
    }

    /**
     * Get an impression of time which elapsed since the start of reasoning in this argument.
     * This uses the idea that 'time' is not a physical effect but simply the result of a delta operation
     * on memory states (thus a 'psychic perception') while 'time' in the physical world (in this model)
     * is just the wrong word for 'causality'. In that context: maybe the "constant perception of time"
     * is what humans call "self-awareness". This could be the core of an artificial self-awareness.
     * @return the number of thoughts within this argument, to be considered as perception of time
     */
    public int times() {
        return this.recall.size();
    }

    /**
     * The 'mindstate' is the current state of an argument. Its the latest thought.
     * This is the same operation as a 'top' for stacks.
     * @return the latest thought in a series of proof steps in an argument
     */
    public SusiThought mindstate() {
        return remember(0);
    }

    /**
     * The mindmeld is the combination of all thoughts into one. It is a required operation in case
     * that a previous argument is recalled and used to start a new one. This prevents that thinking
     * creates ever increasing argument list; instead old arguments can be 'squashed' into one, like
     * it's done with git commits.
     * The mindmeld also should have the ability to 'overwrite' old values with new ones. That is done
     * by considering the new values in a order which makes them more visible than the value before.
     * @param reverse if true, then the latest thought of the argument becomes the primary data
     * @return the squashed thoughts from an argument as one thought
     */
    public SusiThought mindmeld(boolean reverse) {
        return mindmeld(this.recall, reverse);
    }

    public static SusiThought mindmeld(ArrayList<SusiThought> recall, boolean reverse) {
        SusiThought meltedMind = new SusiThought();
        if (reverse)
            for (int i = recall.size() -1; i >= 0; i--) meltedMind.assertz(recall.get(i).getData());
        else 
            for (int i = 0; i < recall.size(); i++) meltedMind.assertz(recall.get(i).getData());
        meltedMind.setTimes(recall.size()); // remember the length of the argument to create a perception of time based on number of thoughts
        return meltedMind;
    }

    /**
     * Remembering the thoughts is essential to recall which thoughts leads to the current mindstate
     * @param timesBack the number of thoughts backwards from the current mindstate
     * @return the thought in the past according to the elapsed time of the thoughts
     */
    public SusiThought remember(int timesBack) {
        int state = this.recall.size() - timesBack - 1;
        if (state < 0) return new SusiThought(); // empty mind!
        return this.recall.get(state);
    }

    /**
     * Re-Thinking removes the latest thought from the mind stack. It may be manipulated
     * by any inference rules and then pushed again to the recall stack. This is needed to
     * manipulate the backtracking stack within each element of an argument during inference
     * processing.
     * This is the same operation as a 'pop' on a stack.
     * @return the latest thought in the argument, while the argument list shrinks by that element
     */
    public SusiThought rethink() {
        if (this.recall.size() <= 0) return new SusiThought(); // empty mind!
        SusiThought rethought = this.recall.remove(this.recall.size() - 1);
        return rethought;
    }

    /**
     * Creating amnesia means to forget all thoughts in an argument.
     * This can be used to squash the argument into one which contains the
     * mindmeld thought from a state before the amnesia is called.
     * @return the current (now empty) argument
     */
    public SusiArgument amnesia() {
        this.recall.clear();
        return this;
    }

    /**
     * Thinking is a series of thoughts, every new thought appends another thought to the argument.
     * A special situation may (or may not) occur if one thinking step does not produce a result.
     * Depending on the inference intent set that may mean that the consideration of the intent containing
     * the inferences was wrong and should be abandoned. This happens if mindstate().equals(thought).
     * This is the same operation as a 'push' on a stack.
     * @param thought the next thought
     * @return self, the current argument
     */
    public SusiArgument think(SusiThought thought) {
        this.recall.add(thought);
        return this;
    }

    /**
     * to remember larger sets of thoughts, we can also think arguments. All of the thoughts of the
     * new arguments are pushed ontop of the recall thought stack.
     * @param argument
     * @return self, the current argument
     */
    public SusiArgument think(SusiArgument argument) {
        argument.recall.forEach(thought -> think(thought));
        return this;
    }

    /**
     * Unification applies a piece of memory within the current argument to a statement
     * which creates an instantiated statement
     * TODO: this should support backtracking, thus producing optional several unifications and turning this into a choice point
     * @param statement
     * @param depth the maximum depth into the flow. depth == 0 means 'only the last thought'
     * @return the instantiated statement with elements of the argument applied
     */
    public String unify(String statement, boolean urlencode, int depth) {
        assert statement != null;
        if (statement == null) return null; // this should not happen
        retry: while (true) {
            explorepast: for (SusiThought t: this) {
                // this uses our iterator which iterates in reverse order.
                // That means, latest thought is first returned.
                // It also means that we are exploring the past, most recent events first.
                if (depth-- < 0) break;
                String nextStatement = t.unifyOnce(statement, urlencode);
                if (nextStatement.equals(statement)) continue explorepast;
                statement = nextStatement;
                if (!SusiThought.hasVariablePattern(statement)) return statement; // possible early success
                continue retry;
            }
            break retry;
        }
        if (SusiThought.hasVariablePattern(statement)) return null; // failure!
        return statement;
    }

    /**
     * the iterator returns the thoughts in reverse order, latest thought first
     */
    @Override
    public Iterator<SusiThought> iterator() {
        return new Iterator<SusiThought>() {
            private int p = recall.size(); 
            @Override public boolean hasNext() {return p > 0;}
            @Override public SusiThought next() {return recall.get(--p);}
        };
    }

    private final static Random random = new Random(System.currentTimeMillis());

    /**
     * Action descriptions are templates for data content. Strings may refer to arguments from
     * a thought deduction using variable templates. I.e. "$name$" inside an action string would
     * refer to an data entity in an thought argument which has the name "name". Applying the
     * Action to a thought will instantiate such variable templates and produces a new String
     * attribute named "expression"
     * @param thoughts an argument from previously applied inferences
     * @param client the client requesting the answer
     * @param language the language of the client
     * @param minds a hierarchy of minds which overlap each other. top mind is the first element in the list of minds
     * @throws ReactionException
     */
    
    /**
     * Actions are templates containing variables which can be instantiated with data from an argument.
     * Applying an action will result in side-effects within the action:
     * - the action gets variables instantiated.
     * The application may also have side-effects on the argument itself:
     * - variable assignments from inside of answer statements may enrich the argument thought chain
     * - resolving reflections inside answer statement may create more actions which are added to the argument
     * @param action
     * @return
     * @throws ReactionException
     */
    public SusiThought applyAction(final SusiAction action) throws ReactionException {
        SusiThought deducedThought = new SusiThought();
        if (action.getRenderType() == RenderType.answer && action.hasAttr("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = action.getPhrases();
            String expression = a.get(random.nextInt(a.size()));

            boolean unificationSuccess = true;
            boolean visibleAssignmentSuccess = true;
            boolean invisibleAssignmentSuccess = true;
            boolean reflectionSuccess = true;
            Matcher m;

            while (unificationSuccess || visibleAssignmentSuccess || invisibleAssignmentSuccess || reflectionSuccess) {

                // unification of the phrase with the thoughts
                // this prepares reflection elements to be instantiated before the reflection is called
                unificationSuccess = false;
                if (expression.indexOf('$') >= 0) {
                    String unification = this.unify(expression, false, Integer.MAX_VALUE);
                    if (unification == null) throw new ReactionException("expression '" + expression + "' cannot be unified with thoughts");
                    unificationSuccess = true;
                    expression = unification;
                }

                // assignments: set variables from the result expressions.
                // These can be a visible assignment or an invisible assignment
                // assignment must be done in advance of reflections
                // because the reflection may use the assigned variables.
                visibleAssignmentSuccess = false;
                TimeoutMatcher tm = new TimeoutMatcher(m = SusiAction.visible_assignment.matcher(expression));
                visibleAssignment: while (tm.find()) {
                    String observation = m.group(1);
                    if (observation.indexOf('$') > 0 || observation.indexOf('`') > 0) continue visibleAssignment;  // there is a unmatched variable or unresolved reflection in the value
                    String variable = m.group(2);
                    expression = expression.substring(0, m.end(1)) + expression.substring(m.end(2));
                    // write the variable v as side-effect into the thoughts argument
                    deducedThought.addObservation(variable, observation);
                    visibleAssignmentSuccess = true;
                }

                invisibleAssignmentSuccess = false;
                tm = new TimeoutMatcher(m = SusiAction.blind_assignment.matcher(expression));
                invisibleAssignment: while (tm.find()) {
                    String observation = m.group(1);
                    if (observation.indexOf('$') > 0 || observation.indexOf('`') > 0) continue invisibleAssignment;  // there is a unmatched variable or unresolved reflection in the value
                    String variable = m.group(2);
                    expression = expression.substring(0, m.start(1) - 1) + expression.substring(m.end(2));
                    // write the variable v as side-effect into the thoughts argument
                    deducedThought.addObservation(variable, observation);
                    invisibleAssignmentSuccess = true;
                }

                // reflection: evaluate contents from the answers expressions as recursion.
                // Susi is asking itself in another thinking request.
                reflectionSuccess = false;
                while (expression.indexOf('`') >= 0) {
                    Reflection reflection = new Reflection(expression, this, false);
                    deducedThought = reflection.deducedThought;
                    expression = reflection.expression;
                    reflectionSuccess = true;
                }
            }

            // if anything is left after this process, it is our expression
            if (expression != null && expression.length() > 0) {
                // the expression is answered to the communication partner
                action.setStringAttr("expression", expression);
                //this.json.put("language", language.name());
            }
        }
        if (action.getRenderType() == RenderType.websearch && action.hasAttr("query")) {
            action.setStringAttr("query", this.unify(action.getStringAttr("query"), false, Integer.MAX_VALUE));
        }
        if (action.getRenderType() == RenderType.anchor && action.hasAttr("link") && action.hasAttr("text")) {
            action.setStringAttr("link", this.unify(action.getStringAttr("link"), false, Integer.MAX_VALUE));
            action.setStringAttr("text", this.unify(action.getStringAttr("text"), false, Integer.MAX_VALUE));
        }
        if (action.getRenderType() == RenderType.map && action.hasAttr("latitude") && action.hasAttr("longitude") && action.hasAttr("zoom")) {
            action.setStringAttr("latitude", this.unify(action.getStringAttr("latitude"), false, Integer.MAX_VALUE));
            action.setStringAttr("longitude", this.unify(action.getStringAttr("longitude"), false, Integer.MAX_VALUE));
            action.setStringAttr("zoom", this.unify(action.getStringAttr("zoom"), false, Integer.MAX_VALUE));
        }
        if ((action.getRenderType() == RenderType.video_play || action.getRenderType() == RenderType.audio_play) && action.hasAttr("identifier")) {
            action.setStringAttr("identifier", this.unify(action.getStringAttr("identifier"), false, Integer.MAX_VALUE));
        }
        if ((action.getRenderType() == RenderType.audio_volume) && action.hasAttr("volume")) {
            String volume = this.unify(action.getStringAttr("volume"), false, Integer.MAX_VALUE);
            int p = volume.indexOf(' ');
            if (p >= 0) volume = volume.substring(0, p).trim();
            int v = 50;
            try {
                v = Integer.parseInt(volume);
            } catch (NumberFormatException e) {
            }
            v = Math.min(100, Math.max(0, v));
            action.setStringAttr("volume", Integer.toString(v));
        }
        deducedThought.addAction(action);
        return deducedThought;
    }

    public static class Reflection {
        SusiThought deducedThought;
        List<SusiAction> reactionActions;
        String expression;

        public Reflection(String query, SusiArgument argument, boolean urlencode) throws ReactionException {
            this.expression = query;
            boolean reflectionSuccess = false;
            Matcher m;
            while ((m = appropriateReflectionMatcher(expression)) != null) {
                String observation = m.group(1);
                if (observation.indexOf('>') > 0) continue;  // there is an assignment in the value
                SusiMind.Reaction reaction = null;
                ReactionException ee = null;
                SusiThought mindstate = argument.mindmeld(true);
                mindlevels: for (SusiMind mind: argument.getMinds()) {
                    try {
                        reaction = mind.new Reaction(observation, argument.getLanguage(), argument.getClientIdentity(), false, mindstate, argument.getMinds());
                        break mindlevels;
                    } catch (ReactionException e) {
                        ee = e;
                        continue mindlevels;
                    }
                }
                if (reaction == null) throw ee == null ? new ReactionException("could not find an answer") : ee;
                this.deducedThought = reaction.getMindstate();
                this.reactionActions = new ArrayList<>();
                for (SusiAction reactionAction: reaction.getActions()) {
                    // we add only non-answer actions, because the answer actions are added within the expression (see below)!
                    if (reactionAction.getRenderType() != RenderType.answer) reactionActions.add(reactionAction);
                    if (reactionAction.isSabta()) throw new ReactionException("sabta in reflection answer"); // the reflection found no answer, just one which pretended to be a proper answer
                }
                List<String> reflExprs = reaction.getExpressions();
                if (reflExprs.size() == 0) {
                    this.expression = "";
                } else {
                    String reflExpr = reflExprs.get(random.nextInt(reflExprs.size()));
                    if (urlencode) try {reflExpr = URLEncoder.encode(reflExpr, "UTF-8");} catch (UnsupportedEncodingException e) {}
                    this.expression = this.expression.substring(0, m.start(1) - 1) + reflExpr + this.expression.substring(m.end(1) + 1);
                    this.expression = this.expression.trim();
                }
                reflectionSuccess = true;
            }
            if (!reflectionSuccess) throw new ReactionException("no reflection inside expression: " + query);
        }
    }

    private static Matcher appropriateReflectionMatcher(String expression) {
        Matcher nested_matcher = SusiAction.reflection_nested.matcher(expression);
        Matcher parallel_matcher = SusiAction.reflection_parallel.matcher(expression);
        if (!nested_matcher.matches() && !parallel_matcher.matches()) return null;
        if (nested_matcher.matches() && !parallel_matcher.matches()) return nested_matcher;
        if (!nested_matcher.matches() && parallel_matcher.matches()) return parallel_matcher;
        // both match; find some good reasons to choose from one
        String nested_observation = nested_matcher.group(1);
        if (nested_observation.length() == 0) return parallel_matcher;
        String nested_observation_trim = nested_observation.trim();
        if (nested_observation_trim.length() == 0) return parallel_matcher;
        String parallel_observation = parallel_matcher.group(1);
        if (parallel_observation.length() == 0) return nested_matcher;
        String parallel_observation_trim = parallel_observation.trim();
        if (parallel_observation_trim.length() == 0) return nested_matcher;
        // beside these trivial heuristics above, we can see that mostly reflection phrases should not have superfluous spaces at the end
        if (nested_observation.length() == nested_observation_trim.length() && parallel_observation.length() != parallel_observation_trim.length()) return nested_matcher;
        if (nested_observation.length() != nested_observation_trim.length() && parallel_observation.length() == parallel_observation_trim.length()) return parallel_matcher;
        // special cases
        for (int i = 0; i < nested_observation.length(); i++) if (nested_observation.charAt(i) < '0') return parallel_matcher;
        // we don't know :(
        return nested_matcher;
    }

    /**
     * An argument is constructed using a skill which may contain a set of intents.
     * This method should be called with a privacy-fixed path to the skill file.
     * @param skill a relative path to the skill
     * @return the argument
     */
    public SusiArgument addSkill(final SusiSkill.ID skillid, int line) {
        assert skillid != null;
        this.skills.put(skillid, line);
        return this;
    }

    /**
     * Compute a finding on an argument: this will cause the execution of all actions of an argument.
     * Then the argument is mind-melted which creates a new thought. The findings from all actions of the
     * argument are attached as 'actions' object: a list of transformed actions where the original action
     * is replaced by the execution result.
     * @param client
     * @param mind
     * @return a new thought containing an action object which resulted from the argument computation
     */
    public SusiThought finding(ClientIdentity identity, SusiLanguage language, boolean debug, SusiMind... mind) throws ReactionException {
        // all actions must be instantiated with variables from this arguments
        List<SusiAction> extractedActions = this.getActionsClone();
        JSONArray appliedActions = new JSONArray();
        for (SusiAction action: extractedActions) { // we need a clone from the here (but not from the actions itself) because we modify/extend the actions list object inside the loop
            SusiThought t = this.applyAction(action);
            if (!t.isFailed()) {
                for (SusiAction a: t.getActions(true)) appliedActions.put(a.toJSONClone());
                t.removeActions(); // we do not want to copy the actions to our argument here, we do this later (below)
                this.think(t); // remember data that has been computed within action evaluation
            }
        }
        // the 'applyAction' method has a possible side-effect on the argument - it can append objects to it
        // therefore the mindmeld must be done after action application to get those latest changes
        SusiThought answer = this.mindmeld(true);
        answer.put("actions", appliedActions);  // this overwrites the result of the mindmeld
        List<String> skillpaths = new ArrayList<>();
        this.skills.forEach((skill, line) -> skillpaths.add(skill.getPath()));
        answer.put("skills", skillpaths);
        JSONObject persona = new JSONObject();
        SusiSkill skill = mind[0].getActiveSkill(); // no need to loop here over all minds because personas are only on top-level minds
        if (skill != null) persona.put("skill", skill.toJSON());
        answer.put("persona", persona);
        return answer;
    }

    /**
     * To be able to apply (re-)actions to this thought, the actions on the information can be retrieved.
     * @return the (re-)actions which are applicable to this thought.
     */
    public List<SusiAction> getActionsClone() {
        List<SusiAction> actionClone = new ArrayList<>();
        this.recall.forEach(thought -> thought.getActions(true).forEach(action -> actionClone.add(action.clone())));
        return actionClone;
    }

    public JSONArray getActionsJSON() {
        JSONArray a = new JSONArray();
        getActionsClone().forEach(action -> a.put(action.toJSONClone()));
        return a;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        JSONArray recallJson = new JSONArray();
        this.recall.forEach(thought -> recallJson.put(thought));
        JSONArray actionsJson = new JSONArray();
        getActionsClone().forEach(action -> actionsJson.put(action.toJSONClone()));
        JSONArray skillJson = new JSONArray();
        this.skills.forEach((skill, line) -> skillJson.put(new JSONObject().put(skill.toString(), line)));
        json.put("recall", recallJson);
        json.put("action", actionsJson);
        json.put("skill", skillJson);
        return json;
    }

    public String toString() {
        return this.toJSON().toString(2);
    }

    public static void main(String[] args) {
        SusiArgument a = new SusiArgument(ClientIdentity.ANONYMOUS, SusiLanguage.en).think(new SusiThought().addObservation("a", "letter-a"));
        System.out.println(a.unify("the letter $a$", true, Integer.MAX_VALUE));
        SusiArgument b = new SusiArgument(ClientIdentity.ANONYMOUS, SusiLanguage.en).think(new SusiThought().addObservation("b", "letter-b"));
        System.out.println(b.unify("the letter $a$", true, Integer.MAX_VALUE));
        SusiArgument c = new SusiArgument(ClientIdentity.ANONYMOUS, SusiLanguage.en).think(new SusiThought().addObservation("b", "letter-b"));
        System.out.println(c.unify("the letter c", true, Integer.MAX_VALUE));
    }
}
