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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.tools.TimeoutMatcher;

/**
 * An Argument is a series of thoughts, also known as a 'proof' in automated reasoning.
 * Within the Susi AI infrastructure this may be considered as the representation of
 * the short-time memory of thinking inside Susi.
 */
public class SusiArgument implements Iterable<SusiThought> {

    private final ArrayList<SusiThought> recall;
    private final List<SusiAction> actions;
    private final List<String> skills;
    
    /**
     * Create an empty argument
     */
    public SusiArgument() {
        this.recall = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.skills = new ArrayList<>();
    }
    
    public SusiArgument clone() {
        SusiArgument c = new SusiArgument();
        this.recall.forEach(thought -> c.recall.add(thought));
        this.actions.forEach(action -> c.actions.add(action));
        return c;
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
        SusiThought meltedMind = new SusiThought();
        if (reverse)
            for (int i = this.recall.size() -1; i >= 0; i--) meltedMind.mergeData(this.recall.get(i).getData());
        else 
            for (int i = 0; i < this.recall.size(); i++) meltedMind.mergeData(this.recall.get(i).getData());
        meltedMind.setTimes(times()); // remember the length of the argument to create a perception of time based on number of thoughts
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
        for (SusiThought t: this) {
            // this uses our iterator which iterates in reverse order. That means, latest thought is first returned
            if (depth-- < 0) break;
            statement = t.unify(statement, urlencode);
            if (!new TimeoutMatcher(SusiThought.variable_pattern.matcher(statement)).find()) return statement;
        }
        if (new TimeoutMatcher(SusiThought.variable_pattern.matcher(statement)).find()) return null; // failure!
        return statement;
    }
    
    public String unify(String statement, boolean urlencode) {
        return unify(statement, urlencode, Integer.MAX_VALUE);
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
    
    /**
     * Every argument may have a set of (re-)actions assigned.
     * Those (re-)actions are methods to do something with the argument.
     * @param action one (re-)action on this argument
     * @return the argument
     */
    public SusiArgument addAction(final SusiAction action) {
        this.actions.add(action);
        return this;
    }

    /**
     * An argument is constructed using a skill which may contain a set of intents.
     * This method should be called with a privacy-fixed path to the skill file.
     * @param skill a relative path to the skill
     * @return the argument
     */
    public SusiArgument addSkill(final String skill) {
    	this.skills.add(skill);
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
    public SusiThought finding(SusiMind mind, String client, SusiLanguage language) {
        Collection<JSONObject> actions = this.getActions().stream()
                .map(action -> action.execution(this, mind, client, language).toJSONClone())
                .collect(Collectors.toList());
        // the 'execution' method has a possible side-effect on the argument - it can append objects to it
        // therefore the mindmeld must be done after action application to get those latest changes
        SusiThought answer = this.mindmeld(true);
        answer.put("actions", actions);
        answer.put("skills", this.getSkills());
        return answer;
    }
    
    /**
     * To be able to apply (re-)actions to this thought, the actions on the information can be retrieved.
     * @return the (re-)actions which are applicable to this thought.
     */
    public List<SusiAction> getActions() {
        return this.actions;
    }
    
    public List<String> getSkills() {
    	return this.skills;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        JSONArray recallJson = new JSONArray();
        this.recall.forEach(thought -> recallJson.put(thought));
        JSONArray actionsJson = new JSONArray();
        this.actions.forEach(action -> actionsJson.put(action.toJSONClone()));
        JSONArray skillJson = new JSONArray();
        this.skills.forEach(skill -> skillJson.put(skill));
        json.put("recall", recallJson);
        json.put("action", actionsJson);
        json.put("skill", skillJson);
        return json;
    }

    public String toString() {
        return this.toJSON().toString(2);
    }
    
    public static void main(String[] args) {
        SusiArgument a = new SusiArgument().think(new SusiThought().addObservation("a", "letter-a"));
        System.out.println(a.unify("the letter $a$", true));
    }
}
