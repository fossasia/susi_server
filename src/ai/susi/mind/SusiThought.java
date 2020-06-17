/**
 *  SusiThought
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiPattern.SusiMatcher;

/**
 * A thought is a piece of data that can be remembered. The structure or the thought can be
 * modeled as a table which may be created using the retrieval of information from elsewhere
 * of the current argument.
 */
public class SusiThought {

    private final String metadata_name, data_name;
    private int times;
    private JSONObject rem;

    /**
     * create an empty thought, to be filled with single data entities.
     */
    public SusiThought() {
        this.rem = new JSONObject(true);
        this.metadata_name = "metadata";
        this.data_name = "data";
        this.times = 0;
    }

    /**
     * create a clone of a json object as a SusiThought object
     * @param json the 'other' thought, probably an exported and re-imported thought
     */
    public SusiThought(JSONObject json) {
        this();
        if (json.has(this.metadata_name)) this.rem.put(this.metadata_name, json.getJSONObject(this.metadata_name));
        if (json.has(this.data_name)) this.setData(json.getJSONArray(this.data_name));
        if (json.has("actions")) this.rem.put("actions", json.getJSONArray("actions"));
        if (json.has("skills")) this.rem.put("skills", json.getJSONArray("skills"));
    }

    /**
     * Create an initial thought using the matcher on an expression.
     * Such an expression is like the input from a text source which contains keywords
     * that are essential for the thought. The matcher extracts such information.
     * Matching informations are named using the order of the appearance of the information pieces.
     * The first information is named '1', the second '2' and so on. The whole information which contained
     * the matching information is named '0'.
     * @param matcher
     */
    public SusiThought(SusiMatcher matcher) {
        this();
        this.setOffset(0).setHits(1);
        JSONObject row = new JSONObject();
        row.put("0", matcher.group(0));
        for (int i = 0; i < matcher.groupCount(); i++) {
            row.put(Integer.toString(i + 1), matcher.group(i + 1));
        }
        this.setData(new JSONArray().put(row));
    }

    /*
    @Deprecated
    public SusiThought(String metadata_name, String data_name) {
        super(true);
        this.metadata_name = metadata_name;
        this.data_name = data_name;
    }
    */

    public boolean equals(Object o) {
        if (!(o instanceof SusiThought)) return false;
        SusiThought t = (SusiThought) o;
        return this.getData().equals(t.getData());
    }
    
    public SusiThought setTimes(int t) {
        this.times = t;
        return this;
    }
    
    public SusiThought addTimes(int t) {
        this.times += t;
        return this;
    }
    
    public int getTimes() {
        return this.times;
    }
    
    /**
     * In a series of information pieces the first information piece has number 0.
     * If the thought is a follow-up series of a previous set of information, an offset is needed.
     * That can be set here.
     * @param offset the offset to a previous set of information pieces.
     * @return the thought
     */
    public SusiThought setOffset(int offset) {
        getMetadata().put("offset", offset);
        return this;
    }
    
    public int getOffset() {
        return getMetadata().has("offset") ? getMetadata().getInt("offset") : 0;
    }
    
    /**
     * The number of information pieces in a set of informations may have a count.
     * @return hits number of information pieces
     */
    public int getCount() {
        return getData().length();
    }

    public boolean isFailed() {
        return this.rem.length() == 0 || getData().length() == 0 && getActions(true).size() == 0;
    }

    public boolean hasEmptyObservation(String key) {
        List<String> observations = this.getObservations(key);
        return observations.size() == 0 || observations.get(0).length() == 0;
    }
    
    /**
     * While the number of information pieces in a whole has a count, the number of relevant
     * information pieces may have been extracted. The hits number gives the number of relevant
     * pieces. This can be set here.
     * @param hits number of information pieces
     * @return the thought
     */
    public SusiThought setHits(int hits) {
        getMetadata().put("hits", hits);
        return this;
    }
    
    public int getHits() {
        return getMetadata().has("hits") ? getMetadata().getInt("hits") : 0;
    }

    /**
     * The process which created this thought may have a name or description string.
     * To document what happened, the process namen can be given here
     * @param query the process which formed this thought
     * @return the thought
     */
    public SusiThought setProcess(String processName) {
        getMetadata().put("process", processName);
        return this;
    }

    /**
     * If this thought was the result of a retrieval using a specific expression, that expression is
     * called the query. The query can be attached to a thought
     * @param query the expression which caused that this thought was formed
     * @return the thought
     */
    public SusiThought setQuery(String query) {
        getMetadata().put("query", query);
        return this;
    }
    
    public String getQuery() {
        return getMetadata().has("query") ? getMetadata().getString("query") : "";
    }
    
    /**
     * If the expression to create this thought had an agent that expressed the result set of the
     * information contained in this thought, it is called the scraper. The scraper name can be attached here.
     * @param scraperInfo the scraper that created this thought
     * @return the thought
     */
    public SusiThought setScraperInfo(String scraperInfo) {
        getMetadata().put("scraperInfo", scraperInfo);
        return this;
    }
    
    /**
     * All details of the creation of this thought is collected in a metadata statement.
     * @return the set of meta information for this thought
     */
    private JSONObject getMetadata() {
        JSONObject md;
        if (this.rem.has(metadata_name)) md = this.rem.getJSONObject(metadata_name); else {
            md = new JSONObject();
            this.rem.put(metadata_name, md);
        }
        if (!md.has("count")) md.put("count", getData().length());
        return md;
    }
    
    /**
     * Information contained in this thought has the form of a result set table, organized in rows and columns.
     * The columns must have all the same name in each row.
     * @param table the information for this thought.
     * @return the thought
     */
    public SusiThought setData(JSONArray table) {
        this.rem.put(data_name, table);
        JSONObject md = getMetadata();
        md.put("count", getData().length());
        return this;
    }

    /**
     * Information contained in this thought can get returned as a table, a set of information pieces.
     * @return a table of information pieces as a set of rows which all have the same column names.
     */
    public JSONArray getData() {
        if (this.rem.has(data_name)) return this.rem.getJSONArray(data_name);
        JSONArray a = new JSONArray();
        this.rem.put(data_name, a);
        return a;
    }
    
    /**
     * "assertz" is merging of data to the end of the thought data structure.
     * Such kind of merging of data is required i.e. during an mind-meld.
     * To meld two thoughts, we combine their data arrays into one.
     * The resulting table has at maximum the length of both source tables combined.
     * Merged data from the 'other' tought does not overwrite the current thought.
     * Instead, it is added as 'alternative solutions'.
     * @param table the information to be melted into our existing table.
     * @return the thought
     */
    public SusiThought assertz(JSONArray table1) {
        JSONArray table0 = this.getData();
        int t0c = 0;
        for (int i = 0; i < table1.length(); i++) {
            JSONObject j1i = table1.getJSONObject(i);
            while (t0c < table0.length()) {
                if (allObjectsSame(j1i, table0.getJSONObject(t0c))) return this;
                if (!anyObjectKeySame(j1i, table0.getJSONObject(t0c))) break;
                t0c++;
            }
            if (t0c >= table0.length()) table0.put(new JSONObject(true));
            table0.getJSONObject(t0c).putAll(table1.getJSONObject(i));
        }
        setData(table0);
        return this;
    }

    private final static boolean allObjectsSame(final JSONObject a, final JSONObject b) {
        if (a.length() != b.length()) return false;
        for (String k: a.keySet()) {
            if (!b.has(k)) return false;
            Object oa = a.get(k);
            Object ob = b.get(k);
            if (oa == null && ob == null) continue;
            if (!oa.toString().equals(ob.toString())) return false;
        }
        return true;
    }

    private final static boolean anyObjectKeySame(final JSONObject a, final JSONObject b) {
        for (String k: a.keySet()) if (b.has(k)) return true;
        return false;
    }
    
    /**
     * If during thinking we observe something that we want to memorize, we can memorize this here.
     * We insert the new data always in front of existing same data to make it visible as primary
     * backtracking option. This means that new observations are always more important than old
     * observations but do not overwrite them; they can be used again in case that the new observation
     * is not valid during inference computation.
     * @param featureName the object key
     * @param observation the object value
     * @return the thought
     */
    public SusiThought addObservation(String featureName, String observation) {
        JSONArray data = getData();
        
        // find first occurrence of key in rows
        int rowc = 0; boolean found = false;
        while (rowc < data.length()) {
            JSONObject row = data.getJSONObject(rowc);
            if (row.has(featureName)) found = true;
            if (found) break;
            rowc++;
        }
        if (found) {
            // insert feature in front of row
            if (rowc == 0) {
                // insert a row and shift everything up
                JSONArray newData = new JSONArray();
                JSONObject row = new JSONObject();
                row.put(featureName, observation);
                newData.put(row);
                for (Object o: data) newData.put(o);
                this.setData(newData);
            } else {
                JSONObject row = data.getJSONObject(rowc - 1);
                row.put(featureName, observation);
            }
        } else {
            // insert into first line
            if (data.length() == 0) {
                JSONObject row = new JSONObject();
                row.put(featureName, observation);
                data.put(row);
            } else {
                JSONObject row = data.getJSONObject(0);
                row.put(featureName, observation);
            }
        }
        return this;
    }
    
    public List<String> getObservations(String featureName) {
        List<String> list = new ArrayList<>();
        JSONArray table = this.getData();
        if (table != null && table.length() > 0) {
            for (int rc = 0; rc < table.length(); rc++) {
            JSONObject row = table.getJSONObject(rc);
                for (String key: row.keySet()) {
                    if (key.equals(featureName)) list.add(row.get(key).toString());
                }
            }
        }
        return list;
    }
    
    public String getObservation(String featureName) {
        JSONArray table = this.getData();
        if (table != null && table.length() > 0) {
            for (int rc = 0; rc < table.length(); rc++) {
                JSONObject row = table.getJSONObject(rc);
                for (String key: row.keySet()) {
                    if (key.equals(featureName)) return row.get(key).toString();
                }
            }
        }
        return null;
    }

    /**
     * Every information may have a set of (re-)actions assigned.
     * Those (re-)actions are methods to do something with the thought.
     * @param actions (re-)actions on this thought
     * @return the thought
     */
    public SusiThought addActions(List<SusiAction> actions) {
        JSONArray a = getActionsJSON();
        actions.forEach(action -> a.put(action.toJSONClone()));
        return this;
    }

    public SusiThought addAction(SusiAction action) {
        JSONArray a = getActionsJSON();
        a.put(action.toJSONClone());
        return this;
    }

    /**
     * Write a list of applied Actions to the actions array.
     * This differs from the normal actions concept in that way, that adding actions
     * otherwise means "un-applied actions". Here we ad "applied actions"
     * TODO: do not use appliedActions concept at all to be able to remove this mess
     * @param appliedActions
     * @return
     */
    @Deprecated
    public SusiThought overwriteAppliedActions(JSONArray appliedActions) {
        this.rem.put("actions", appliedActions);
        return this;
    }

    public SusiThought removeActions() {
        if (this.rem.has("actions")) this.rem.remove("actions");
        return this;
    }
    /**
     * To be able to apply (re-)actions to this thought, the actions on the information can be retrieved.
     * @return the (re-)actions which are applicable to this thought.
     */
    public List<SusiAction> getActions(final boolean ignoreWarnings) {
        final List<SusiAction> actions = new ArrayList<>();
        getActionsJSON().forEach(a -> {
            try {
                SusiAction action = new SusiAction((JSONObject) a);
                actions.add(action);
            } catch (SusiActionException e) {
                if (!ignoreWarnings) DAO.severe("invalid action - " + e.getMessage() + ": " + ((JSONObject) a).toString(0));
            }
        });
        return actions;
    }

    private JSONArray getActionsJSON() {
        JSONArray actions;
        if (!this.rem.has("actions")) {
            actions = new JSONArray();
            this.rem.put("actions", actions);
        } else {
            actions = this.rem.getJSONArray("actions");
        }
        return actions;
    }

    /**
     * describes within the JSON response how the skill that gave the answer looked like
     * @param skill
     * @return
     */
    public SusiThought addPersona(SusiSkill skill) {
        JSONObject persona = new JSONObject();
        if (skill != null) persona.put("skill", skill.toJSON());
        this.rem.put("persona", persona);
        return this;
    }

    public SusiThought addTrace(JSONArray testedIdeaQueryPatterns) {
        this.rem.put("trace", testedIdeaQueryPatterns);
        return this;
    }
    
    public SusiThought addSkill(String skillpath) {
        if (!this.rem.has("skills")) this.rem.put("skills", new JSONArray());
        JSONArray skills = this.rem.getJSONArray("skills");
        skills.put(skillpath);
        return this;
    }

    public List<String> getSkills() {
        List<String> skills = new ArrayList<>();
        getSkillsJSON().forEach(skill -> {if (skill instanceof String) skills.add((String) skill);});
        return skills;
    }

    private JSONArray getSkillsJSON() {
        JSONArray skills;
        if (!this.rem.has("skills")) {
            skills = new JSONArray();
            this.rem.put("skills", skills);
        } else {
            skills = this.rem.getJSONArray("skills");
        }
        return skills;
    }
    
    public String getLogPath() {
        if (!this.rem.has("skills")) return null;
        JSONArray skills = this.rem.getJSONArray("skills");
        if (skills.length() == 0) return null;
        Object p = skills.get(0);
        return p instanceof String ? getLogPath((String) p) : null;
    }

    public static String getLogPath(String skillName) {
        if (skillName == null) return null;
        skillName = skillName.replace(':', '_').replace('.', '_') + ".log";
        return skillName;
    }
    
    public static boolean hasVariablePattern(String statement) {
        int p = statement.indexOf('$');
        if (p < 0) return false;
        int q = statement.indexOf('$', p + 1);
        return q >= 0;
    }
    
    /**
     * Unification applies a piece of memory within the current argument to a statement
     * which creates an instantiated statement
     * @param statement the term to be matched in the thought
     * @param max the maximum number of unifications
     * @param allowUninstantiated if true, statements may be incompletely instantiated
     * @return the instantiated statements, in case allowUninstantiated=true maybe partly uninstantiated
     */
    public String[] unify(String statement, int max, boolean allowUninstantiated, boolean urlencode) {
        assert statement != null;
        if (statement.indexOf('$') < 0) return new String[]{statement}; // NOT empty!
        String threadOrigName = Thread.currentThread().getName();
        Thread.currentThread().setName("unify: term = " + statement); // makes debugging easier
        JSONArray table = this.getData();
        LinkedHashSet<String> instances = new LinkedHashSet<>();
        if (allowUninstantiated) instances.add(statement); // prevent that we don't have any statement in case uninstantiated results are allowed
        if (table != null && table.length() > 0) {
            int count = 0;
            if (table != null && table.length() > 0) for (int rownum = 0; rownum < table.length(); rownum++) {
                JSONObject row = table.getJSONObject(rownum);
                String u = unifyRow(statement, row, urlencode);
                // we first record all rows
                instances.add(u);
                if (u.indexOf('$') < 0) count++;
                if (count >= max) break;
            }

            // now try to instantiate unresolved statements
            LinkedHashSet<String> ix = new LinkedHashSet<>();
            for (String i: instances) {
                if (ix.size() >= max) break;
                if (i.indexOf('$') < 0) {
                    ix.add(i);
                    continue;
                }
                String x = null;
                if (table != null && table.length() > 0) rows: for (int rownum = 0; rownum < table.length(); rownum++) {
                    JSONObject row = table.getJSONObject(rownum);
                    i = unifyRow(i, row, urlencode);
                    if (i.indexOf('$') < 0) {
                        x = i;
                        break rows;
                    }
                }
                if (x != null) {
                    ix.add(x);
                } else if (allowUninstantiated) {
                    ix.add(i);
                }
            }
            instances = ix;
        }
        Thread.currentThread().setName(threadOrigName);
        return instances.toArray(new String[instances.size()]);
    }
    
    /**
     * unification of a statement with a row can cause unresolved variables in the statement
     * @param statement
     * @param row
     * @param urlencode
     * @return an instantiated statement, possibly incomplete
     */
    private String unifyRow(String statement, JSONObject row, boolean urlencode) {
        for (String key: row.keySet()) {
            int i;
            Object value = row.get(key);
            if (value instanceof JSONObject) {
                JSONObject subobj = (JSONObject) value;
                for (String subkey: subobj.keySet()) {
                    while ((i = statement.indexOf("$" + key + "." + subkey + "$")) >= 0) {
                        String substitution = subobj.get(subkey).toString();
                        if (urlencode) try {
                            substitution = URLEncoder.encode(substitution, "UTF-8");
                        } catch (UnsupportedEncodingException e) {}
                        statement = statement.substring(0, i) + substitution + statement.substring(i + key.length() + subkey.length() + 3);
                    }
                }
            } else {
                if ((i = statement.indexOf("$" + key + "$")) >= 0) {
                    String substitution = value.toString();
                    if (urlencode) try {
                        substitution = URLEncoder.encode(substitution, "UTF-8");
                    } catch (UnsupportedEncodingException e) {}
                    statement = statement.substring(0, i) + substitution + statement.substring(i + key.length() + 2);
                }
            }
            if (statement.indexOf('$') < 0) return statement;
        }
        return statement;
    }

    public JSONObject toJSON() {
        return this.rem;
    }

    public String toString() {
        return this.rem.toString(0);
    }

    public int hashCode() {
        // TODO: this is very expensive. find a better option to create hash codes. Also: check comparing with equals()
        return this.getData().toString().hashCode();
    }

    // below now debugging methods:

    public boolean hasUniqueActions() {
        return hasUniqueActions(this.rem);
    }

    private static boolean hasUniqueActions(JSONObject json) {
        JSONArray a = json.optJSONArray("actions");
        if (a != null && a.length() > 1) {
            Set<String> exp = new HashSet<>();
            Iterator<Object> ai = a.iterator();
            while (ai.hasNext()) {
                String e = ((JSONObject) ai.next()).optString("expression");
                if (e != null) {
                    if (exp.contains(e)) return false;
                    exp.add(e);
                }
            }
        }
        return true;
    }

    public void uniqueActions() {
        uniqueActions(this.rem);
    }

    /**
     * Bad Hack - remove this as soon as possible.
     * This was made due to the situation that double actions occur and the cause was not detected - so far.
     * As soon as the cuase is found, this method must disappear.
     * @param json
     */
    public static void uniqueActions(JSONObject json) {
        JSONArray a = json.optJSONArray("actions");
        if (a != null && a.length() > 1) {
            Set<String> exp = new HashSet<>();
            Iterator<Object> ai = a.iterator();
            while (ai.hasNext()) {
                String e = ((JSONObject) ai.next()).optString("expression");
                if (e != null) {
                    if (exp.contains(e)) ai.remove();
                    exp.add(e);
                }
            }
        }
    }

    public static void main(String[] args) {
        SusiThought t0 = new SusiThought().addObservation("a", "1").addObservation("a", "2").addObservation("b", "7");
        SusiThought t1 = new SusiThought().addObservation("a", "1").addObservation("b", "7").addObservation("a", "2");
        System.out.println("t0: " + t0.toString());
        System.out.println("t1: " + t1.toString());
        String[] x = new SusiThought().unify("$x$", 10, true, true);
        for (int i = 0; i < x.length; i++) {
            System.out.println("t0 -> x" + i + " = " + x[i]);
        }
        String[] u = t0.unify("$a$, $b$", 10, true, true);
        for (int i = 0; i < u.length; i++) {
            System.out.println("t0 -> u" + i + " = " + u[i]);
        }
        u = t1.unify("$a$, $b$", 10, true, true);
        for (int i = 0; i < u.length; i++) {
            System.out.println("t1 -> u" + i + " = " + u[i]);
        }
        u = t1.unify("$a$, $b$, $c$", 10, true, true);
        for (int i = 0; i < u.length; i++) {
            System.out.println("tx -> u" + i + " = " + u[i]);
        }
    }
}
