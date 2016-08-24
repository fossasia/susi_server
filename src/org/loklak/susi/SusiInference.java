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

import org.json.JSONObject;
import org.loklak.api.search.ConsoleService;

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
            return this.ordinal();
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
        flowSkill.put(Pattern.compile("FIRST"), (flow, matcher) -> {
            // extract only the first row of a thought
            SusiThought nextThought = new SusiThought();
            SusiThought mindstate = flow.mindstate();
            if (flow != null && mindstate.getCount() > 0) nextThought.getData().put(mindstate.getData().get(0));
            return nextThought;
        });
        flowSkill.put(Pattern.compile("FIRST\\h+?(.*?)\\h*?"), (flow, matcher) -> {
            SusiThought nextThought = new SusiThought();
            SusiThought mindstate = flow.mindstate();
            if (flow != null && mindstate.getCount() > 0) nextThought.getData().put(mindstate.getData().get(0));
            return nextThought;
        });
        flowSkill.put(Pattern.compile("REST"), (flow, matcher) -> {
            // remove the first row of a thought and return the remaining
            SusiThought nextThought = new SusiThought();
            SusiThought mindstate = flow.mindstate();
            if (flow != null && mindstate.getCount() > 0) nextThought.getData().put(mindstate.getData().remove(0));
            return nextThought;
        });
        flowSkill.put(Pattern.compile("REST\\h+?(.*?)\\h*?"), (flow, matcher) -> {
            SusiThought nextThought = new SusiThought();
            SusiThought mindstate = flow.mindstate();
            if (flow != null && mindstate.getCount() > 0) nextThought.getData().put(mindstate.getData().remove(0));
            return nextThought;
        });
        flowSkill.put(Pattern.compile("SEE\\h+?(.*?)\\h+?FROM\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            return see(flow, matcher.group(1), matcher.group(2), Pattern.compile(flow.unify(matcher.group(3))));
        });
        flowSkill.put(Pattern.compile("SEE\\h+?(.*?)\\h+?FROM\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            return see(flow, matcher.group(1), matcher.group(2), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(3)))));
        });
        flowSkill.put(Pattern.compile("EXPECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            return see(flow, "*", matcher.group(1), Pattern.compile(flow.unify(matcher.group(2))));
        });
        flowSkill.put(Pattern.compile("EXPECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            return see(flow, "*", matcher.group(1), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(2)))));
        });
        flowSkill.put(Pattern.compile("REJECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?REGEX\\h*?"), (flow, matcher) -> {
            SusiThought t = see(flow, "*", matcher.group(1), Pattern.compile(flow.unify(matcher.group(2))));
            if (t.getCount() == 0) return new SusiThought().addObservation("regex-" + matcher.group(2), matcher.group(1));
            return new SusiThought(); // empty thought -> fail
        });
        flowSkill.put(Pattern.compile("REJECT\\h+?'(.*?)'\\h+?MATCHING\\h+?'(.*?)'\\h+?PATTERN\\h*?"), (flow, matcher) -> {
            SusiThought t = see(flow, "*", matcher.group(1), Pattern.compile(SusiPhrase.parsePattern(flow.unify(matcher.group(2)))));
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
    
    private static final SusiThought see(SusiArgument flow, String transferExpr, String expr, Pattern pattern) {
        // there will be an empty thought as response if the pattern does not match
        // having no match will cause a fail (triggered by empty thought), the rule is aborted
        // example: see $1$ as idea from ""
        SusiThought nextThought = new SusiThought();
        try {
            Matcher m = pattern.matcher(flow.unify(expr));
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
     * Inference must be applicable to thought arguments. This method executes the inference process on an existing 
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
        // maybe the argument is not applicable, then the latest mindstate is empty application
        return flow.mindstate();
}
    
    public String toString() {
        return this.getJSON().toString();
    }
    
    public JSONObject getJSON() {
        return this.json;
    }
}
