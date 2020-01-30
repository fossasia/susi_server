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

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.tools.DateParser;

/**
 * An action is an application on the information deduced during inferences on mind states
 * as they are represented in an argument. If we want to produce respond sentences or if
 * we want to visualize deduces data as a graph or in a picture, thats an action.
 * 
 * Thoughts:
 * We need a device logic ontop of actions, like:
 * - an action must be performed synchronously or concurrently
 * - an action may be interruted or not
 * - actions which may be interrupted must be identifiable with a (temporary?) action ID
 * - are there follow-up actions on interrupted actions?
 * - can several actions be performed concurrently and then synchronized again with a join-step for a common follow-up action?
 * We need a declaration to express this; a logic in the client to perform this and an expression in the skills to declare this.
 */
public class SusiAction implements Cloneable {

    public static class SusiActionException extends Exception {
        private static final long serialVersionUID = -754075705722756817L;
        public SusiActionException(String message) {
            super(message);
        }
    }

    public static enum RenderType {
        answer(196),   // show or say a text
        stop(255),     // stop any actions that are running right now
        pause(255),    // pause something that woud otherwise beeing able to be stopped
        resume(255),   // resume what has been paused
        restart,       // start over of what is happening right now
        previous,      // step to one thing in the sequence before
        next,          // step in the next thing in the sequence
        table,         // show a table
        piechart,      // show a pie chart
        rss,           // show a link list with description (aka search result listing)
        websearch(0),  // do a web search on the client, show like rss rendering
        anchor,        // show/say a link
        map,           // show a map
        timer_set,     // set a timer on the client
        timer_reset,   // un-set a timer on the client
        audio_volume(200),  // audio volume settings
        audio_record,  // record audio
        audio_play,    // play audio (recorded, asset on client or asset from web)
        //audio_stop,    // stop playing of audio OR recording of audio (disabled because we will do that with the stop render type)
        video_record,  // record a video
        video_play,    // play the video (recorded, asset on client or asset from web)
        //video_stop,    // stop playing or video OR recording of video (disabled because we will do that with the stop render type)
        image_take,    // take an image
        image_show,    // show an image (recorded, asset on client or asset from web)
        emotion,       // show an emotion (either change tone of tts or change visible style)
        button_push,   // push a button (either on the client device or an IoT appliance connected to the client)
        io,             // set an IO status on connected IoT device
        shuffle         // shuffle the current playlist
        ;

        private final int score;

        private RenderType() {
            this.score = 128;
        }
        private RenderType(int score) {
            this.score = score;
        }

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

        public int getScore() {
            return this.score;
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

    private JSONObject json;

    /**
     * Initialize an action using a json description.
     * This method is only used for the definition of intents, not for the production of answers.
     * The Exceptions are thrown for valiation purpose in case that the action declaration is not sound.
     * @param json the action declaration
     */
    public SusiAction(JSONObject json) throws SusiActionException {
        this.json = json;
        // check if the action is valid. If it is not valid, throw an exception
        // {"type":"answer","select":"random","phrases":["Here is the exact location of the event $2$"]}
        if (!json.has("type")) throw new SusiActionException("the action needs a type object");
        try {
            RenderType renderType = RenderType.valueOf(json.getString("type"));
            switch (renderType) {
                case answer:
                    if (json.has("expression")) break;
                    // This is a very special and (call it hacked) declaration:
                    // - a declared answer has no expression by default!
                    // - the select and phrases attributes are used to generate an expression on the fly during answering
                    // So that means that an expression is usually not used for an action declaration.
                    if (!json.has("select")) this.json.put("select", "random"); // was: throw new SusiActionException("the answer action needs a select object");
                    if (!json.has("phrases")) throw new SusiActionException("the answer action needs a phrases object");
                    break;
                case stop:
                    //stop has no attributes
                    break;
                case pause:
                    //pause has no attributes
                    break;
                case resume:
                    //resume has no attributes
                    break;
                case restart:
                    //restart has no attributes
                    break;
                case previous:
                    //previous has no attributes
                    break;
                case next:
                    //next has no attributes
                    break;
                case shuffle:
                    //shuffle has no attributes
                    break;
                case table:
                    if (!json.has("columns")) throw new SusiActionException("the table action needs a columns object");
                    if (!json.has("count")) json.put("count", -1);
                    break;
                case piechart:
                    if (!json.has("total")) throw new SusiActionException("the piechart action needs a total object");
                    if (!json.has("key")) throw new SusiActionException("the piechart action needs a key object");
                    if (!json.has("value")) throw new SusiActionException("the piechart action needs a value object");
                    if (!json.has("unit")) throw new SusiActionException("the piechart action needs a unit object");
                    break;
                case rss:
                    if (!json.has("title")) throw new SusiActionException("the rss action needs a title object");
                    if (!json.has("description")) throw new SusiActionException("the rss action needs a description object");
                    if (!json.has("link")) throw new SusiActionException("the rss action needs a link object");
                    if (!json.has("count")) json.put("count", -1);
                    break;
                case websearch:
                    if (!json.has("query")) throw new SusiActionException("the websearch action needs a query object");
                    break;
                case anchor:
                    if (!json.has("link")) throw new SusiActionException("the anchor action needs a link object");
                    if (!json.has("text")) throw new SusiActionException("the anchor action needs a text object");
                    break;
                case map:
                    if (!json.has("latitude")) throw new SusiActionException("the map action needs a latitude object");
                    if (!json.has("longitude")) throw new SusiActionException("the map action needs a longitude object");
                    if (!json.has("zoom")) throw new SusiActionException("the map action needs a zoom object");
                    break;
                case timer_set:
                    // the time number is the unix time, UTC. Clients must translate this into their local time.
                        if (!json.has("time")) throw new SusiActionException("the timer_set action needs a time object");
                    break;
                case timer_reset:
                    //timer_reset has no attributes
                    break;
                case audio_volume:
                    if (!json.has("volume")) throw new SusiActionException("the audio_volume action needs a volume object");
                    break;
                case audio_record:
                    throw new SusiActionException("this audio_record action is not yet defined");
                case audio_play:
                    // in most cases the identifier must be an URL. The URL can point to a file location as well.
                    // if the identifier is a file, the URL is like file:///user/admin/audio/example.mp3
                    // the type may be i..: "url", "youtube". If youtube is given then the identifier is the number of the youtube video.
                    // we could give an exact url of the youtube stream, but thay may change over time while the youtube video stays the same.
                    // There is the option to give susi_server application-relative paths. These must be relaive file URLs, like
                    // file://conf/audio/all_systems_are_go_all_lights_are_green.mp3
                    // this is translated into an absolute path during this processing
                    if (!json.has("identifier")) throw new SusiActionException("the audio_play action needs an identifier object");
                    if (!json.has("identifier_type")) throw new SusiActionException("the audio_play action needs an identifier_type object");
                    String audio_type = json.getString("identifier_type");
                    if (!audio_type.equals("url") && !audio_type.equals("youtube") && !audio_type.equals("soundcloud")) throw new SusiActionException("the identifier_type object in unknown");
                    if (audio_type.equals("url")) {
                        String url = json.getString("identifier");
                        if (url.startsWith("file://") && url.length() > 8 && url.charAt(7) != '/') {
                            // this is a relative path; relative to application path
                            File f = new File(DAO.conf_dir.getParentFile(), url.substring(7));
                            json.put("identifier", "file://" + f.getAbsolutePath());
                        }
                    }
                    break;
                case video_record:
                    throw new SusiActionException("this action is not yet defined");
                case video_play:
                    if (!json.has("identifier")) throw new SusiActionException("the video_play action needs an identifier object");
                    if (!json.has("identifier_type")) throw new SusiActionException("the video_play action needs an identifier_type object");
                    String video_type = json.getString("identifier_type");
                    if (!video_type.equals("url") && !video_type.equals("youtube")) throw new SusiActionException("the identifier_type object in unknown");
                    break;
                case image_take:
                    throw new SusiActionException("this action is not yet defined");
                case image_show:
                    if (!json.has("url")) throw new SusiActionException("the image_show action needs an url object");
                    break;
                case emotion:
                    throw new SusiActionException("this emotion action is not yet defined");
                case button_push:
                    throw new SusiActionException("this button_push action is not yet defined");
                case io:
                    throw new SusiActionException("this io action is not yet defined");
                default:
                    throw new SusiActionException("the action type '" + renderType + "' is not handled. Extend the Action Case statement."); // if you see this exception then the case statment must be extended with the new action type
            }
        } catch (IllegalArgumentException e) {
            throw new SusiActionException("the action type '" + json.getString("type") + "' is not known");
        }
    }

    public SusiAction clone() {
        JSONObject j = new JSONObject(true);
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key)));
        try {
            return new SusiAction(j);
        } catch (SusiActionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * answer action: produces a written or spoken answer.
     * If several answers are given one of then can be picked by random by the client.
     * @param answers
     * @return the action
     */
    public static JSONObject answerAction(int line, SusiLanguage language, String... answers) {
        JSONArray phrases = new JSONArray();
        for (String answer: answers) phrases.put(answer.trim());
        JSONObject json = new JSONObject(true)
            .put("type", RenderType.answer.name())
            .put("select", SelectionType.random.name())
            .put("phrases", phrases);
        assert language != SusiLanguage.unknown;
        if (language != SusiLanguage.unknown) json.put("language", language.name());
        if (line > 0) json.put("line", line);
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

    public boolean isSabta() {
        return this.json.has("mood") && "sabta".equals(this.json.getString("mood"));
    }

    public boolean hasAttr(String attr) {
        return this.json.has(attr);
    }

    /**
     * if the action contains more String attributes where these strings are named, they can be retrieved here
     * @param attr the name of the string attribute
     * @return the action string
     */
    public String getStringAttr(String attr) {
        return this.json.has(attr) ? this.json.getString(attr) : "";
    }

    public SusiAction setStringAttr(String attr, String value) {
        this.json.put(attr, value);
        return this;
    }

    /**
     * If the action contains integer attributes, they can be retrieved here
     * @param attr the name of the integer attribute
     * @return the integer number
     */
    public int getIntAttr(String attr) {
        return this.json.has(attr) ? this.json.getInt(attr) : 0;
    }

    public long getLongAttr(String attr) {
        return this.json.has(attr) ? this.json.getLong(attr) : 0L;
    }

    public SusiAction setIntAttr(String attr, int value) {
        this.json.put(attr, value);
        return this;
    }

    public SusiAction setLongAttr(String attr, long value) {
        this.json.put(attr, value);
        return this;
    }

    public Date getDateAttr(String attr) throws ParseException {
        String d = this.getStringAttr(attr);
        return DateParser.parse(d, 0).getTime();
    }

    public SusiAction setDateAttr(String attr, Date date) {
        String d = DateParser.formatISO8601(date);
        return setStringAttr(attr, d);
    }

    final static Pattern visible_assignment = Pattern.compile("(?:(?:.*)[\\?\\!\\s,\\.;-]+)?([^\\^]+?)>([_a-zA-Z0-9]+)(?:[\\?\\!\\s,\\.;-](?:.*))?+");
    final static Pattern blind_assignment = Pattern.compile("(?:.*?)\\^(.*?)\\^>([_a-zA-Z0-9]+)(?:[\\?\\!\\s,\\.;-](?:.*))?+");
    final static Pattern reflection_parallel = Pattern.compile(".*?`([^`]*?)`.*?");
    final static Pattern reflection_nested = Pattern.compile(".*?`(.*)`.*?");

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
        return toJSONClone().toString(2);
    }
}
