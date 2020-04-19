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

package ai.susi.mind;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonPath;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiArgument.Reflection;
import ai.susi.mind.SusiMind.ReactionException;
import ai.susi.server.api.susi.ConsoleService;
import ai.susi.tools.DateParser;
import ai.susi.tools.TimeoutMatcher;
import ai.susi.tools.HttpClient.Response;
import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Theory;

/**
 * Automated reasoning systems need inference methods to move from one proof state to another.
 * In Susi reasoning we also have inference methods which retrieve data from external data sources
 * which is a data enrichment, but also inference methods which may filter, transform and reduce the
 * Susi though elements in an argument. Inferences are applied step by step and act like a data stream.
 * Concatenated inferences are like piped commands or stream lambdas. Each time an inference is applied
 * an unification step is done first to instantiate the variables inside an inference with the thought
 * argument from the steps before.
 */
public class SusiInference {

    public static enum Type {
        console, flow, memory, javascript, prolog;
        public int getSubscore() {
            return this.ordinal() + 1;
        }
    }

    private JSONObject json;
    private static final ScriptEngine javascript =  new ScriptEngineManager().getEngineByName("nashorn");

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

    public SusiInference(String expression, Type type, int line) {
        assert expression != null;
        this.json = new JSONObject(true);
        this.json.put("type", type.name());
        this.json.put("expression", expression);
        this.json.put("line", line);
    }

    public SusiInference(JSONObject definition, Type type) {
        this.json = new JSONObject(true);
        this.json.put("type", type.name());
        this.json.put("definition", definition);
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

    public JSONObject getDefinition() {
        return this.json.has("definition") ? this.json.getJSONObject("definition") : null;
    }

    private final static SusiProcedures flowProcedures = new SusiProcedures();
    private final static SusiProcedures memoryProcedures = new SusiProcedures();
    private final static SusiProcedures javascriptProcedures = new SusiProcedures();
    private final static SusiProcedures prologProcedures = new SusiProcedures();
    static {
        flowProcedures.put(Pattern.compile("SQUASH"), (flow, matcher) -> {
            // perform a full mindmeld
            if (flow == null) return new SusiThought();
            SusiThought squashedArgument = flow.mindmeld(true);
            flow.amnesia();
            return squashedArgument;
        });
        flowProcedures.put(Pattern.compile("FIRST"), (flow, matcher) -> {
            // extract only the first row of a thought
            SusiThought recall = flow == null ? new SusiThought() : flow.rethink(); // removes/replaces the latest thought from the flow!
            if (recall.getCount() > 0) recall.setData(new JSONArray().put(recall.getData().getJSONObject(0)));
            return recall;
        });
        flowProcedures.put(Pattern.compile("REST"), (flow, matcher) -> {
            // remove the first row of a thought and return the remaining
            SusiThought recall = flow == null ? new SusiThought() : flow.rethink(); // removes/replaces the latest thought from the flow!
            if (recall.getCount() > 0) recall.getData().remove(0);
            return recall;
        });
        flowProcedures.put(Pattern.compile("PLAN\\h+?([^:]*?)\\h*?:\\h*?([^:]*)\\h*?"), (flow, matcher) -> {
            // get attributes
            String time = flow.unify(matcher.group(1), false, 0);
            String reflection = flow.unify(matcher.group(2), false, 0);
            SusiThought mindstate = flow.mindmeld(true);

            // parse the time
            String timezoneOffsets = mindstate.getObservation("timezoneOffset");
            int timezoneOffset = 0;
            if (timezoneOffsets != null) try {timezoneOffset = Integer.parseInt(timezoneOffsets);} catch (NumberFormatException e) {}
            final long start = System.currentTimeMillis();
            final Date date = DateParser.parseAnyText(time, timezoneOffset);
            if (date == null) return null;
            final long delay = date.getTime() - start;

            // create planned action
            JSONObject actionj = SusiAction.answerAction(0, flow.getLanguage(), reflection);
            try {
                SusiAction planned_utterance = new SusiAction(actionj);
                SusiThought planned_thought = flow.applyAction(planned_utterance); // this also instantiates the answer in the planned_utterance
                planned_thought.addAction(planned_utterance); // we want the planned_utterance as well as part of the flow
                planned_thought.getActions(true).forEach(action -> {
                    // add a delay to the actions
                    action.setLongAttr("plan_delay", delay);
                    action.setDateAttr("plan_date", date);
                });
                flow.think(planned_thought);
            } catch (ReactionException | SusiActionException e) {
                return new SusiThought(); // empty thought as fail
            }

            SusiThought queued = flow.mindmeld(true);
            return queued;
        });
        memoryProcedures.put(Pattern.compile("SET\\h+?([^=]*?)\\h+?=\\h+?([^=]*)\\h*?"), (flow, matcher) -> {
            String remember = matcher.group(1), matching = matcher.group(2);
            return see(flow, flow.unify("%1% AS " + remember, false, 0), flow.unify(matching, false, 0), Pattern.compile("(.*)"));
        });
        memoryProcedures.put(Pattern.compile("SET\\h+?([^=]*?)\\h+?=\\h+?([^=]*?)\\h+?MATCHING\\h+?(.*)\\h*?"), (flow, matcher) -> {
            String remember = matcher.group(1), matching = matcher.group(2), pattern = matcher.group(3);
            return see(flow, flow.unify(remember, false, 0), flow.unify(matching, false, 0), Pattern.compile(flow.unify(pattern, false, 0)));
        });
        memoryProcedures.put(Pattern.compile("CLEAR\\h+?(.*)\\h*?"), (flow, matcher) -> {
            String clear = matcher.group(1);
            return see(flow, "%1% AS " + flow.unify(clear, false, 0), "", Pattern.compile("(.*)"));
        });
        memoryProcedures.put(Pattern.compile("IF\\h+?([^=]*)\\h*?"), (flow, matcher) -> {
            String expect = matcher.group(1);
            SusiThought t = see(flow, "%1% AS EXPECTED", flow.unify(expect, false, 0), Pattern.compile("(.+)"));
            if (t.isFailed() || t.hasEmptyObservation("EXPECTED")) return new SusiThought(); // empty thought -> fail
            return t;
        });
        memoryProcedures.put(Pattern.compile("IF\\h+?([^=]*?)\\h*=\\h*([^=]*)\\h*?"), (flow, matcher) -> {
            String expect = matcher.group(1), matching = matcher.group(2);
            SusiThought t = see(flow, "%1% AS EXPECTED", flow.unify(expect, false, 0), Pattern.compile(flow.unify(matching, false, 0)));
            if (t.isFailed() || t.hasEmptyObservation("EXPECTED")) return new SusiThought(); // empty thought -> fail
            return t;
        });
        memoryProcedures.put(Pattern.compile("NOT\\h*"), (flow, matcher) -> {
            SusiThought t = see(flow, "%1% AS EXPECTED", "", Pattern.compile("(.*)"));
            return new SusiThought().addObservation("REJECTED", "");
        });
        memoryProcedures.put(Pattern.compile("NOT\\h+?([^=]*)\\h*?"), (flow, matcher) -> {
            String reject = matcher.group(1);
            SusiThought t = see(flow, "%1% AS EXPECTED", flow.unify(reject, false, 0), Pattern.compile("(.*)"));
            if (t.isFailed() || t.hasEmptyObservation("EXPECTED")) return new SusiThought().addObservation("REJECTED", reject);
            return new SusiThought(); // empty thought -> fail
        });
        memoryProcedures.put(Pattern.compile("NOT\\h+?([^=]*?)\\h*=\\h*([^=]*)\\h*?"), (flow, matcher) -> {
            String reject = matcher.group(1), matching = matcher.group(2);
            SusiThought t = see(flow, "%1% AS EXPECTED", flow.unify(reject, false, 0), Pattern.compile(flow.unify(matching, false, 0)));
            if (t.isFailed() || t.hasEmptyObservation("EXPECTED")) return new SusiThought().addObservation("REJECTED(" + matching + ")", reject);
            return new SusiThought(); // empty thought -> fail
        });
        javascriptProcedures.put(Pattern.compile("(?s:(.*))"), (flow, matcher) -> {
            String term = matcher.group(1);
            term = flow.unify(term, false, Integer.MAX_VALUE);
            while (term.indexOf('`') >= 0) try {
                Reflection reflection = new Reflection(term, flow, false);
                term = reflection.expression;
            } catch (ReactionException e) {
                e.printStackTrace();
                break;
            }
            try {
                StringWriter stdout = new StringWriter();
                javascript.getContext().setWriter(new PrintWriter(stdout));
                javascript.getContext().setErrorWriter(new PrintWriter(stdout));
                Object o;
                try {
                    o = javascript.eval(term);
                } catch (ScriptException e) {
                    o = e.getMessage();
                }
                String bang = o == null ? "" : o.toString().trim();
                if (bang.length() == 0) bang = stdout.getBuffer().toString().trim();
                return new SusiThought().addObservation("!", bang);
            } catch (Throwable ee) {
                DAO.severe(ee);
                return new SusiThought(); // empty thought -> fail
            }
        });
        prologProcedures.put(Pattern.compile("(?s:(.*))"), (flow, matcher) -> {
            String term = matcher.group(1);
            try {
                Prolog engine = new Prolog();
                try {
                    engine.setTheory(new Theory(term));
                    SolveInfo solution = engine.solve("associatedWith(X, Y, Z)."); // example
                    if (solution.isSuccess()) { // example
                        System.out.println(solution.getTerm("X"));
                        System.out.println(solution.getTerm("Y"));
                        System.out.println(solution.getTerm("Z"));
                    }
                } catch (InvalidTheoryException ex) {
                    DAO.log("invalid theory - line: "+ex.line);
                } catch (Exception ex){
                    DAO.log("invalid theory.");
                }
                return new SusiThought().addObservation("!", "");
            } catch (Throwable e) {
                DAO.severe(e);
                return new SusiThought(); // empty thought -> fail
            }
        });
        // more procedures:
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
            Matcher m = pattern.matcher(flow.unify(expr, false, 0));
            int gc = -1;
            if (new TimeoutMatcher(m).matches()) {
                SusiTransfer transfer = new SusiTransfer(transferExpr);
                JSONObject choice = new JSONObject();
                if ((gc = m.groupCount()) > 0) {
                    for (int i = 0; i < gc; i++) choice.put("%" + (i+1) + "%", m.group(i));
                } else {
                    choice.put("%1%", expr);
                }
                JSONObject seeing = transfer.extract(choice);
                for (String key: seeing.keySet()) {
                    String observed = seeing.getString(key);
                    nextThought.addObservation(key, observed);
                }
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
    public SusiThought applyProcedures(SusiArgument flow) {
        Type type = this.getType();
        if (type == SusiInference.Type.console) {
            String expression = this.getExpression();
            // we have two ways to define a console rule:
            // with a "defintion" object which should have an "url" and "path" defined
            // with a "expression" object which has a susi db access string included
            if (expression.length() == 0) {
                // this might have an anonymous console rule inside
                JSONObject definition = this.getDefinition();
                if (definition == null) return new SusiThought();

                // execute the console rule right here
                SusiThought json = new SusiThought();

                // inject data object if one is given
                if (definition.has("data") && definition.get("data") instanceof JSONArray) {
                    JSONArray data = definition.getJSONArray("data");
                    if (data != null) {
                        json.setData(new SusiTransfer("*").conclude(data));
                        json.setHits(json.getCount());
                    }
                }

                // load more data using an url and a path
                if (definition.has("url")) try {
                    String url = flow.unify(definition.getString("url"), true, Integer.MAX_VALUE);
                    try {while (url.indexOf('`') >= 0) {
                        SusiArgument.Reflection reflection = new SusiArgument.Reflection(url, flow, true);
                        url = reflection.expression;
                    }} catch (ReactionException e) {}
                    String path = definition.has("path") ? flow.unify(definition.getString("path"), false, Integer.MAX_VALUE) : null;

                    // make a custom request header
                    Map<String, String> request_header = new HashMap<>();
                    if (path != null) request_header.put("Accept","application/json");
                    Map<String, Object> request =  definition.has("request") ? definition.getJSONObject("request").toMap() : null;
                    if (request != null) request.forEach((key, value) -> request_header.put(key, value.toString()));
                    /*
                    request_header.put("Accept-Encoding","gzip, deflate, br");
                    request_header.put("Accept-Language","de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
                    request_header.put("Cache-Control","max-age=0");
                    request_header.put("Connection","keep-alive");
                    request_header.put("Cookie","WR_SID=e6de8822.59648a8a2de5a");
                    request_header.put("Host","api.wolframalpha.com");
                    request_header.put("Sec-Fetch-Mode","navigate");
                    request_header.put("Sec-Fetch-Site","none");
                    request_header.put("Sec-Fetch-User","?1");
                    request_header.put("Upgrade-Insecure-Requests","1");
                    request_header.put("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                     */
                    // issue the request
                    Response httpresponse = null;
                    JSONArray data = new JSONArray();
                    try {
                        httpresponse = new Response(url, request_header);
                    } catch (IOException e) {
                        DAO.log("no response from API: " + url);
                    }

                    byte[] b = httpresponse == null ? null : httpresponse.getData();
                    if (b != null && path != null) try {
                        data = JsonPath.parse(b, path);
                    } catch (JSONException e) {
                        DAO.log("JSON data from API cannot be parsed: " + url);
                    }

                    // parse response
                    Map<String, Object> response =  definition.has("response") ? definition.getJSONObject("response").toMap() : null;
                    if (response != null && httpresponse != null) {
                        JSONObject obj_from_http_response_header = new JSONObject(true);
                        Map<String, List<String>> response_header = httpresponse.getResponse();
                        response.forEach((httpresponse_key, susi_varname) -> {
                            List<String> values = response_header.get(httpresponse_key);
                            if (values != null && !values.isEmpty()) obj_from_http_response_header.put(susi_varname.toString(), values.iterator().next());
                        });
                        if (!obj_from_http_response_header.isEmpty()) data.put(obj_from_http_response_header);
                    }

                    // evaluate the result data
                    if (data != null && !data.isEmpty()) {
                        JSONArray learned = new SusiTransfer("*").conclude(data);
                        if (learned.length() > 0 && definition.has("actions") &&
                            learned.get(0) instanceof JSONObject &&
                            definition.get("actions") instanceof JSONArray) {
                            learned.getJSONObject(0).put("actions", definition.getJSONArray("actions"));
                        }
                        json.setData(learned);
                    }
                    json.setHits(json.getCount());
                } catch (Throwable e) {
                    DAO.severe("SusiInference.applyProcedures", e);
                } else if (definition.has("actions") && definition.get("actions") instanceof JSONArray) {
                    // case where we have no console rules but only an actions object: this should not fail
                    // to provoke that this cannot fail it must produce data, othervise the data created by the inference
                    // is empty and an empty thought fails. We use this situation to make a log of the actions we did
                    JSONArray learned = new JSONArray().put(new JSONObject().put("actions", definition.getJSONArray("actions")));
                    json.setData(learned);
                    json.setHits(json.getCount());
                }
                return json;
            } else {
                try {return ConsoleService.dbAccess.deduce(flow, flow.unify(expression, false, Integer.MAX_VALUE));} catch (Exception e) {}
            }
        }
        if (type == SusiInference.Type.flow) {
            String expression = flow.unify(this.getExpression(), false, Integer.MAX_VALUE);
            try {return flowProcedures.deduce(flow, expression);} catch (Exception e) {}
        }
        if (type == SusiInference.Type.memory) {
            String expression = flow.unify(this.getExpression(), false, Integer.MAX_VALUE);
            try {return memoryProcedures.deduce(flow, expression);} catch (Exception e) {}
        }
        if (type == SusiInference.Type.javascript) {
            String expression = flow.unify(this.getExpression(), false, Integer.MAX_VALUE);
            try {return javascriptProcedures.deduce(flow, expression);} catch (Exception e) {}
        }
        if (type == SusiInference.Type.prolog) {
            String expression = flow.unify(this.getExpression(), false, Integer.MAX_VALUE);
            try {return prologProcedures.deduce(flow, expression);} catch (Exception e) {}
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
