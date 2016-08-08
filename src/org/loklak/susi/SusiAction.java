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
import java.util.Random;

import org.json.JSONObject;

/**
 * An action is an application on the information deduced during inferences on mind states
 * as they are represented in an argument. If we want to produce respond sentences or if
 * we want to visualize deduces data as a graph or in a picture, thats an action.
 */
public class SusiAction {

    public static enum ActionType {answer, table, piechart;}
    public static enum ActionAnswerType {random, roundrobin;}
    
    private final static Random random = new Random(System.currentTimeMillis());
    
    private JSONObject json;

    /**
     * initialize an action using a json description.
     * @param json
     */
    public SusiAction(JSONObject json) {
        this.json = json;
    }
    
    /**
     * Get the action type. That can be used to filter specific infirmation from the action JSON object
     * to create specific activities like 'saying' a sentence, painting a graph and so on.
     * @return the action type
     */
    public ActionType getType() {
        return this.json.has("type") ? ActionType.valueOf(this.json.getString("type")) : null;
    }
    
    /**
     * If the action involves the reproduction of phrases (=strings) then they can be retrieved here
     * @return the action phrases
     */
    public ArrayList<String> getPhrases() {
        ArrayList<String> a = new ArrayList<>();
        if (!this.json.has("phrases")) return a;
        this.json.getJSONArray("phrases").forEach(p -> a.add((String) p));
        return a;
    }
    
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
    public SusiAction apply(SusiArgument thoughts) {
        if (this.getType() == ActionType.answer && this.json.has("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = getPhrases();
            String phrase = a.get(random.nextInt(a.size()));
            String expression = thoughts.unify(phrase);
            if (expression != null) this.json.put("expression", expression);
        }
        return this;
    }
    
    /**
     * An action is backed with a JSON data structure. That can be retrieved here.
     * @return the json structure of the action
     */
    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key))); // make a clone
        if (j.has("expression")) {
            j.remove("phrases");
            j.remove("select");
            this.json.remove("expression"); // thats a bad hack, TODO: better concurrency
        }
        return j;
    }
    
    /**
     * toString
     * @return return the json representation of the object as a string
     */
    public String toString() {
        return toJSON().toString();
    }
}
