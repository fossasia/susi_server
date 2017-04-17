/**
 *  SusiAction
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
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.tools.TimeoutMatcher;

/**
 * An action is an application on the information deduced during inferences on mind states
 * as they are represented in an argument. If we want to produce respond sentences or if
 * we want to visualize deduces data as a graph or in a picture, thats an action.
 */
public class SusiAction {

    public static enum RenderType {answer, table, piechart, rss, self, websearch, anchor, map;}
    public static enum SelectionType {random, roundrobin;}
    public static enum DialogType {
        answer,    // a sentence which may end a conversation
        question,  // a sentence which may cause that the user answers with a fact
        reply;     // a response of an answers of the user from a question aked by sudy
        public int getSubscore() {
            return this.ordinal();
        }
    }
    
    private final static Random random = new Random(System.currentTimeMillis());
    
    private JSONObject json;

    /**
     * initialize an action using a json description.
     * @param json
     */
    public SusiAction(JSONObject json) {
        this.json = json;
    }

    public static JSONObject answerAction(String[] answers) {
        JSONObject json = new JSONObject();
        JSONArray phrases = new JSONArray();
        json.put("type", RenderType.answer.name());
        json.put("select", SelectionType.random.name());
        json.put("phrases", phrases);
        for (String answer: answers) phrases.put(answer.trim());
        return json;
    }
    
    /**
     * Get the render type. That can be used to filter specific information from the action JSON object
     * to create specific activities like 'saying' a sentence, painting a graph and so on.
     * @return the action type
     */
    public RenderType getRenderType() {
        if (renderTypeCache == null) 
            renderTypeCache = this.json.has("type") ? RenderType.valueOf(this.json.getString("type")) : null;
        return renderTypeCache;
    }
    private RenderType renderTypeCache = null;

    public DialogType getDialogType() {
        if (this.getRenderType() != RenderType.answer) return DialogType.answer;
        return getDialogType(getPhrases());
    }
    
    public static DialogType getDialogType(Collection<String> phrases) {
        DialogType type = DialogType.reply;
        for (String phrase: phrases) {
            DialogType t = getDialogType(phrase);
            if (t.getSubscore() < type.getSubscore()) type = t;
        }
        return type;
    }
    
    public static DialogType getDialogType(String phrase) {
        if (phrase.indexOf('?') > 3) { // the question mark must not be at the beginning
            return phrase.indexOf(". ") >= 0 ? DialogType.reply : DialogType.question;
        }
        return DialogType.answer;
    }
    
    /**
     * If the action involves the reproduction of phrases (=strings) then they can be retrieved here
     * @return the action phrases
     */
    public ArrayList<String> getPhrases() {
        if (phrasesCache == null) {
            ArrayList<String> a = new ArrayList<>();
            // actions may have either a single expression "expression" or a phrases object with 
            if (this.json.has("expression")) {
                a.add(this.json.getString("expression"));
            } else if (this.json.has("phrases")) {
                this.json.getJSONArray("phrases").forEach(p -> a.add((String) p));
            } else return null;
            phrasesCache = a;
        }
        return phrasesCache;
    }
    private ArrayList<String> phrasesCache = null;
    
    /**
     * if the action contains more String attributes where these strings are named, they can be retrieved here
     * @param attr the name of the string attribute
     * @return the action string
     */
    public String getStringAttr(String attr) {
        return this.json.has(attr) ? this.json.getString(attr) : "";
    }
    
    /**
     * If the action contains integer attributes, they can be retrieved here
     * @param attr the name of the integer attribute
     * @return the integer number
     */
    public int getIntAttr(String attr) {
        return this.json.has(attr) ? this.json.getInt(attr) : 0;
    }

    final static Pattern visible_assignment = Pattern.compile("(?:(?:.*)[\\?\\!\\h,\\.;-])+([^\\^]+?)>([_a-zA-Z0-9]+)(?:[\\?\\!\\h,\\.;-](?:.*))?+");
    final static Pattern blind_assignment = Pattern.compile("(?:.*)\\^(.*?)\\^>([_a-zA-Z0-9]+)(?:[\\?\\!\\h,\\.;-](?:.*))?+");
    final static Pattern self_referrer = Pattern.compile(".*`([^`]*?)`.*");
    
    /**
     * Action descriptions are templates for data content. Strings may refer to arguments from
     * a thought deduction using variable templates. I.e. "$name$" inside an action string would
     * refer to an data entity in an thought argument which has the name "name". Applying the
     * Action to a thought will instantiate such variable templates and produces a new String
     * attribute named "expression"
     * @param thoughts an argument from previously applied inferences
     * @return the action with the attribute "expression" instantiated by unification of the thought with the action
     */
    public SusiAction execution(SusiArgument thoughts, SusiMind mind, String client) {
        if ((this.getRenderType() == RenderType.answer || this.getRenderType() == RenderType.self) && this.json.has("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = getPhrases();
            String phrase = a.get(random.nextInt(a.size()));
            String expression = thoughts.unify(phrase);
            if (expression != null) {
                // transform the answer according to the data
                // this is the final chance that we can add another thought according to a memorizing skill in the answer string
                Matcher m;

                // self-referrer evaluate contents from the answers expressions as recursion: susi is asked again
                while (new TimeoutMatcher(m = self_referrer.matcher(expression)).matches()) {
                    String observation = m.group(1);
                    expression = expression.substring(0, m.start(1) - 1) + mind.react(observation, client, new SusiThought()) + expression.substring(m.end(1) + 1);
                }
                
                // assignments set variables from the result expressions. These can be visible or invisible
                while (new TimeoutMatcher(m = visible_assignment.matcher(expression)).matches()) {
                    String observation = m.group(1);
                    String variable = m.group(2);
                    expression = expression.substring(0, m.end(1)) + expression.substring(m.end(2));
                    // write the variable v as side-effect into the thoughts argument
                    thoughts.think(new SusiThought().addObservation(variable, observation));
                }
                while (new TimeoutMatcher(m = blind_assignment.matcher(expression)).matches()) {
                    String observation = m.group(1);
                    String variable = m.group(2);
                    expression = expression.substring(0, m.start(1) - 1) + expression.substring(m.end(2));
                    // write the variable v as side-effect into the thoughts argument
                    thoughts.think(new SusiThought().addObservation(variable, observation));
                }
                
                // find an response type: self-recursion or answer
                if (this.getRenderType() == RenderType.answer) {
                    // the expression is answered to the communication partner
                    this.json.put("expression", expression);
                }
                if (this.getRenderType() == RenderType.self) {
                    // recursive call susi with the answer
                    expression = mind.react(expression, client, new SusiThought());
                    this.json.put("expression", expression);
                    this.phrasesCache = null; // important, otherwise the expression is not recognized
                    // patch the render type
                    this.json.put("type", RenderType.answer.name());
                    this.renderTypeCache = RenderType.answer;
                }
            }
        }
        if (this.getRenderType() == RenderType.websearch && this.json.has("query")) {
            this.json.put("query", thoughts.unify(getStringAttr("query")));
        }
        if (this.getRenderType() == RenderType.anchor && this.json.has("link") && this.json.has("text")) {
            this.json.put("link", thoughts.unify(getStringAttr("link")));
            this.json.put("text", thoughts.unify(getStringAttr("text")));
        }
        if (this.getRenderType() == RenderType.map && this.json.has("latitude") && this.json.has("longitude") && this.json.has("zoom")) {
            this.json.put("latitude", thoughts.unify(getStringAttr("latitude")));
            this.json.put("longitude", thoughts.unify(getStringAttr("longitude")));
            this.json.put("zoom", thoughts.unify(getStringAttr("zoom")));
        }
        return this;
    }
    
    /**
     * An action is backed with a JSON data structure. That can be retrieved here.
     * @return the json structure of the action
     */
    public JSONObject toJSONClone() {
        JSONObject j = new JSONObject(true);
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key))); // make a clone
        if (j.has("expression")) {
            j.remove("phrases");
            j.remove("select");
        }
        return j;
    }
    
    /**
     * toString
     * @return return the json representation of the object as a string
     */
    public String toString() {
        return toJSONClone().toString();
    }
}
