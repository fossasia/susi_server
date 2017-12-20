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

    public static enum RenderType {
        answer,        // show or say a text
        table,         // show a table
        piechart,      // show a pie chart
        rss,           // show a link list with description (aka search result listing)
        self,          // reflection (internal function)
        websearch,     // do a web search on the client, show like rss rendering
        anchor,        // show/say a link
        map,           // show a map
        timer_set,     // set a timer on the client
        timer_reset,   // un-set a timer on the client
        audio_record,  // record audio
        audio_play,    // play audio (recorded, asset on client or asset from web)
        audio_stop,    // stop playing of audio OR recording of audio
        video_record,  // record a video
        video_play,    // play the video (recorded, asset on client or asset from web)
        video_stop,    // stop playing or video OR recording of video
        image_take,    // take an image
        image_show,    // show an image (recorded, asset on client or asset from web)
        emotion,       // show an emotion (either change tone of tts or change visible style)
        button_push,   // push a button (either on the client device or an IoT appliance connected to the client)
        io             // set an IO status on connected IoT device
        ;
    
        public String action() {
            String name = this.name();
            int p = name.indexOf('_');
            return p < 0 ? name : name.substring(0, p);
        }
        
        public String context() {
            String name = this.name();
            int p = name.indexOf('_');
            return p < 0 ? null : name.substring(p + 1);
        }
    }
    
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

    /**
     * answer action: produces a written or spoken answer.
     * If several answers are given one of then can be picked by random by the client.
     * @param answers
     * @return the action
     */
    public static JSONObject answerAction(String... answers) {
        JSONArray phrases = new JSONArray();
        for (String answer: answers) phrases.put(answer.trim());
        JSONObject json = new JSONObject()
            .put("type", RenderType.answer.name())
            .put("select", SelectionType.random.name())
            .put("phrases", phrases);
        return json;
    }
    
    /**
     * table action: to draw a table. The columns of the table must be selected and named
     * @param cols a mapping from column names in th data object to the display names for the client rendering
     * @return the action
     */
    public static JSONObject tableAction(JSONObject cols, int count) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.table.name())
            .put("columns", cols)
            .put("count", count);
        return json;
    }
    
    /**
     * piechart action: draw a pie chart
     * @param total the total count of the sum of all pie shares, i.e. 100 if the shares are percent values
     * @param keyName the key name of the data column which points to the values
     * @param valueDescription a descriptive naming of the values
     * @param valueUnit the unit of the values, i.e. "%"
     * @return the action
     */
    public static JSONObject piechartAction(int total, String keyName, String valueDescription, String valueUnit) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.piechart.name())
            .put("total", total)
            .put("key", keyName)
            .put("value", valueDescription)
            .put("unit", valueUnit);
        return json;
    }
    
    /**
     * rss action: draw a search result-like list. The client must show this in the same way as a websearch type action would do
     * @param titleName the name of the title column
     * @param descriptionName the name of the description column
     * @param linkName the name of the link column
     * @return the action
     */
    public static JSONObject rssAction(String titleName, String descriptionName, String linkName, int count) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.rss.name())
            .put("title", titleName)
            .put("description", descriptionName)
            .put("link", linkName)
            .put("count", count);
        return json;
    }
    
    /**
     * websearch action: draw a search result list. This must look the same as the rss action
     * @param query the search query
     * @return the action
     */
    public static JSONObject websearchAction(String query) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.websearch.name())
            .put("query", query);
        return json;
    }
    
    /**
     * anchor action: draw a single anchor with descriptive text as anchor text
     * @param link the link
     * @param text the anchor text
     * @return the action
     */
    public static JSONObject anchorAction(String link, String text) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.anchor.name())
            .put("link", link)
            .put("text", text);
        return json;
    }
    
    /**
     * map action: draw a map for a given location and zoom level. The location is
     * the center of the map. The client should draw a marker on the map at the given
     * location.
     * @param latitude 
     * @param longitude
     * @param zoom zoom level, same as for openstreetmap
     * @return the action
     */
    public static JSONObject mapAction(double latitude, double longitude, int zoom) {
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.map.name())
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("zoom", zoom);
        return json;
    }

    /**
     * set timer: set an alarm in the client making the request.
     *@param hour
     * @param minute
     * @param second
     * @return the action
     */
    public static JSONObject timerSetAction(int hour, int minute, int second) {
        JSONObject json = new JSONObject(true)
                .put("type", RenderType.timer_set.name())
                .put("hour", hour)
                .put("minute", minute)
                .put("second", second);
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

    final static Pattern visible_assignment = Pattern.compile("(?:.*)[\\?\\!\\h,\\.;-]+([^\\^]+?)>([_a-zA-Z0-9]+)(?:[\\?\\!\\h,\\.;-](?:.*?))?+");
    final static Pattern blind_assignment = Pattern.compile("(?:.*?)\\^(.*?)\\^>([_a-zA-Z0-9]+)(?:[\\?\\!\\h,\\.;-](?:.*?))?+");
    final static Pattern self_referrer = Pattern.compile(".*?`([^`]*?)`.*?");
    
    /**
     * Action descriptions are templates for data content. Strings may refer to arguments from
     * a thought deduction using variable templates. I.e. "$name$" inside an action string would
     * refer to an data entity in an thought argument which has the name "name". Applying the
     * Action to a thought will instantiate such variable templates and produces a new String
     * attribute named "expression"
     * @param thoughts an argument from previously applied inferences
     * @return the action with the attribute "expression" instantiated by unification of the thought with the action
     */
    public SusiAction execution(SusiArgument thoughts, SusiMind mind, String client, SusiLanguage language) {
        if ((this.getRenderType() == RenderType.answer || this.getRenderType() == RenderType.self) && this.json.has("phrases")) {
            // transform the answer according to the data
            ArrayList<String> a = getPhrases();
            String phrase = a.get(random.nextInt(a.size()));
            String expression = thoughts.unify(phrase, false);
            if (expression != null) {
                // transform the answer according to the data
                // this is the final chance that we can add another thought according to a memorizing intent in the answer string
                Matcher m;

                // self-referrer evaluate contents from the answers expressions as recursion: susi is asked again
                while (new TimeoutMatcher(m = self_referrer.matcher(expression)).matches()) {
                    String observation = m.group(1);
                    SusiMind.Reaction reaction = mind.new Reaction(observation, language, client, new SusiThought());
                    String selfanswer = reaction.getExpression();
                    thoughts.think(reaction.getMindstate());
                    expression = expression.substring(0, m.start(1) - 1) + selfanswer + expression.substring(m.end(1) + 1);
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
                    SusiMind.Reaction reaction = mind.new Reaction(expression, language, client, new SusiThought());
                    expression = reaction.getExpression();
                    thoughts.think(reaction.getMindstate());
                    this.json.put("expression", expression);
                    this.phrasesCache = null; // important, otherwise the expression is not recognized
                    // patch the render type
                    this.json.put("type", RenderType.answer.name());
                    this.renderTypeCache = RenderType.answer;
                }
            }
        }
        if (this.getRenderType() == RenderType.websearch && this.json.has("query")) {
            this.json.put("query", thoughts.unify(getStringAttr("query"), false));
        }
        if (this.getRenderType() == RenderType.anchor && this.json.has("link") && this.json.has("text")) {
            this.json.put("link", thoughts.unify(getStringAttr("link"), false));
            this.json.put("text", thoughts.unify(getStringAttr("text"), false));
        }
        if (this.getRenderType() == RenderType.map && this.json.has("latitude") && this.json.has("longitude") && this.json.has("zoom")) {
            this.json.put("latitude", thoughts.unify(getStringAttr("latitude"), false));
            this.json.put("longitude", thoughts.unify(getStringAttr("longitude"), false));
            this.json.put("zoom", thoughts.unify(getStringAttr("zoom"), false));
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
