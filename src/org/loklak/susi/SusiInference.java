/**
 *  SusiInference
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.api.aggregation.ConsoleService;

/**
 * Automated reasoning systems need inference methods to move from one proof state to another.
 * In Susi reasoning we also have inference methods which retrieve data from external data sources
 * which is a data enrichment, but also inference methods which may filter, transform and reduce the
 * Susi though elements in an argument. Inferences are applied step by step and act like a data stream.
 * Concatenated inferences are like piped commands or stream lambdas. Each time an inferece is applied
 * an unification step is done first to instantiate the variables inside an inference with the thought
 * argument from the steps before.
 */
public class SusiInference {
    
    public static enum Type {
        console, flow, memory;
        public int getSubscore() {
            return this.ordinal() + 1;
        }
    }
    
    private JSONObject json;

    /**
     * Instantiate an inference with the inference description. The description should usually contain two
     * properties:
     *   type: the name of the inference type. This can also be considered as the 'program language' of the inference
     *   expression: the description of the inference transformation. You can consider that beeing the 'program' of the inference
     * @param json the inference description
     */
    public SusiInference(JSONObject json) {
        this.json = json;
    }

    /**
     * Inferences may have different types. Each type selects inference methods inside the inference description.
     * While the inference description mostly has only one other attribute, the "expression" it might have more.
     * @return the inference type
     */
    public Type getType() {
        return this.json.has("type") ? Type.valueOf(this.json.getString("type")) : Type.console;
    }
    
    /**
     * The inference expression is the 'program code' which describes what to do with thought arguments.
     * This program code is therefore like a lambda expression which takes a table-like data structure
     * and produces a new table-like data structure as a result. New data structures are then later appended to
     * thought arguments and therefore expand the argument with that one new inference result
     * @return the inference expression
     */
    public String getExpression() {
        return this.json.has("expression") ? this.json.getString("expression") : "";
    }
    
    private final static SusiSkills flowSkill = new SusiSkills();
    static {
        flowSkill.put(Pattern.compile("SQUASH"), (flow, matcher) -> {
            // perform a full mindmeld
            if (flow == null) return new SusiThought();
            SusiThought squashedArgument = flow.mindmeld(true);
            flow.amnesia();
            return squashedArgument;
        });
        flowSkill.put(Pattern.compile("FIRST"), (flow, matcher) -> {
            // extract only the first row of a thought
            SusiThought recall = flow == null ? new SusiThought() : flow.rethink(); // removes/replaces the latest thought from the flow!
            if (recall.getCount() > 0) recall.setData(new JSONArray().put(recall.getData().getJSONObject(0)));
            return recall;
        });
        flowSkill.put(Pattern.compile("REST"), (flow, matcher) -> {
            // remove the first row of a thought and return the remaining
            SusiThought recall = flow == null ? new SusiThought() : flow.rethink(); // removes/replaces the latest thought from the flow!
            if (recall.getCount() > 0) recall.getData().remove(0);
            return recall;
        });
        flowSkill.put(Pattern.compile("REMEMBER\\h+?(.*?)\\h+?FROM\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            return see(flow, flow.unify(matcher.group(1), 0), flow.unify(matcher.group(2), 0), Pattern.compile(flow.unify(matcher.group(3), 0)));
        });
        flowSkill.put(Pattern.compile("REMEMBER\\h+?(.*?)\\h+?FROM\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            return see(flow, flow.unify(matcher.group(1), 0), flow.unify(matcher.group(2), 0), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(3), 0))));
        });
        flowSkill.put(Pattern.compile("EXPECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            return see(flow, "*", matcher.group(1), Pattern.compile(flow.unify(matcher.group(2), 0)));
        });
        flowSkill.put(Pattern.compile("EXPECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            return see(flow, "*", matcher.group(1), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(2), 0))));
        });
        flowSkill.put(Pattern.compile("REJECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            SusiThought t = see(flow, "*", matcher.group(1), Pattern.compile(flow.unify(matcher.group(2), 0)));
            if (t.getCount() == 0) return new SusiThought().addObservation("regex-" + matcher.group(2), matcher.group(1));
            return new SusiThought(); // empty thought -> fail
        });
        flowSkill.put(Pattern.compile("REJECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            SusiThought t = see(flow, "*", matcher.group(1), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(2), 0))));
            if (t.getCount() == 0) return new SusiThought().addObservation("pattern-" + matcher.group(2), matcher.group(1));
            return new SusiThought(); // empty thought -> fail
        });
        // more skills:
        // - map/reduce to enable loops
        // - sort asc/dec
        // - stack + join
        // - register (write temporary variable)
        // - compute (write computation into new field)
        // - cut (to stop backtracking)
    }
    
    /**
     * "see" defines a new thought based on the names given in the "transferExpr" and retrieved using the content of
     * a variable in the "expr" expression using a matching in the given pattern. It can be used to check if something
     * learned in the flow matches against a known pattern. When the matching is successful, that defines a new knowledge
     * pieces that are stored in the resulting thought thus extending an argument with new insights.
     * The "transferExpr" must be constructed using variables of the name schema "%1%", "%2%", .. which contain matches
     * of the variables from the expr retrieval in the flow with the pattern.
     * @param flow the argument flow
     * @param transferExpr SQL-like transfer expression, like "a AS akk, b AS bit". These defined variables stored in the flow as next thought
     * @param expr the name of a variable entity in the argument flow. The content of that variable is matched in the pattern
     * @param pattern the
     * @return a new thought containing variables from the matcher in the pattern
     */
    private static final SusiThought see(SusiArgument flow, String transferExpr, String expr, Pattern pattern) {
        // example: see $1$ as idea from ""
        SusiThought nextThought = new SusiThought();
        try {
            Matcher m = pattern.matcher(flow.unify(expr, 0));
            if (m.matches()) {
                SusiTransfer transfer = new SusiTransfer(transferExpr);
                JSONObject choice = new JSONObject();
                int gc = m.groupCount();
                choice.put("%0%", m.group(0));
                for (int i = 0; i < gc; i++) choice.put("%" + (i+1) + "%", m.group(i));
                JSONObject seeing = transfer.extract(choice);
                for (String key: seeing.keySet()) nextThought.addObservation(key, seeing.getString(key));
            }
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }
        return nextThought; // an empty thought is a fail signal
    }
    
    
    /**
     * The inference must be applicable to thought arguments. This method executes the inference process on an existing 
     * argument and produces another thought which may or may not be appended to the given argument to create a full
     * argument proof. Within this method also data from the argument is unified with the inference variables
     * @param flow
     * @return a new thought as result of the inference
     */
    public SusiThought applySkills(SusiArgument flow) {
        Type type = this.getType();
        if (type == SusiInference.Type.console) {
            String expression = flow.unify(this.getExpression());
            try {return ConsoleService.dbAccess.deduce(flow, expression);} catch (Exception e) {}
        }
        if (type == SusiInference.Type.flow) {
            String expression = flow.unify(this.getExpression());
            try {return flowSkill.deduce(flow, expression);} catch (Exception e) {}
        }
        // maybe the argument is not applicable, then an empty thought is produced (which means a 'fail')
        return new SusiThought();
}
    
    public String toString() {
        return this.getJSON().toString();
    }
    
    public JSONObject getJSON() {
        return this.json;
    }
}
