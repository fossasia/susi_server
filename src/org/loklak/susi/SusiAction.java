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

package org.loklak.susi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * An action is an application on the information deduced during inferences on mind states
 * as they are represented in an argument. If we want to produce respond sentences or if
 * we want to visualize deduces data as a graph or in a picture, thats an action.
 */
public class SusiAction {

    public static enum RenderType {answer, table, piechart, rss, self, websearch, anchor, map;}
    public static enum SelectionType {random, roundrobin;}
    public static enum DialogType {
        answer, question, reply;
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

    public static JSONObject simpleAction(String[] answers) {
        JSONObject json = new JSONObject();
        JSONArray phrases = new JSONArray();
        json.put("type", RenderType.answer.name());
        json.put("select", SelectionType.random.name());
        json.put("phrases", phrases);
        for (String answer: answers) phrases.put(answer);
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
        DialogType type = DialogType.answer;
        for (String phrase: phrases) {
            type = getDialogType(phrase);
            if (type != DialogType.answer) return type;
        }
        return DialogType.answer;
    }
    
    public static DialogType getDialogType(String phrase) {
        if (phrase.endsWith("?")) {
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
    
    /**
     * Action descriptions are templates for data content. Strings may refer to arguments from
     * a thought deduction using variable templates. I.e. "$name$" inside an action string would
     * refer to an data entity in an thought argument which has the name "name". Applying the
     * Action to a thought will instantiate such variable templates and produces a new String
     * attribute named "expression"
     * @param thoughts an argument from previously applied inferences
     * @return the action with the attribute "expression" instantiated by unification of the thought with the action
     */
    public SusiAction apply(SusiArgument thoughts, SusiMind mind, String client) {
        if ((this.getRenderType() == RenderType.answer || this.getRenderType() == RenderType.self) && this.json.has("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = getPhrases();
            String phrase = a.get(random.nextInt(a.size()));
            String expression = thoughts.unify(phrase);
            if (expression != null) {
                if (this.getRenderType() == RenderType.answer) {
                    // transform the answer according to the data
                    this.json.put("expression", expression);
                }
                if (this.getRenderType() == RenderType.self) {
                    // recursive call susi with the answer
                    List<SusiArgument> datalist = mind.react(expression, 1, client, null);
                    SusiArgument bestargument = datalist.get(0);
                    expression = bestargument.getActions().get(0).getStringAttr("expression");
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
