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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.mind.SusiMind.ReactionException;
import ai.susi.tools.TimeoutMatcher;

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
public class SusiAction {
    
    public static class SusiActionException extends Exception {
        private static final long serialVersionUID = -754075705722756817L;
        public SusiActionException(String message) {
            super(message);
        }
    }
    
    public static enum RenderType {
        answer,        // show or say a text
        stop,          // stop any actions that are running right now
        table,         // show a table
        piechart,      // show a pie chart
        rss,           // show a link list with description (aka search result listing)
        self,          // reflection (internal function)
        websearch,     // do a web search on the client, show like rss rendering
        anchor,        // show/say a link
        map,           // show a map
        timer_set,     // set a timer on the client
        timer_reset,   // un-set a timer on the client
        audio_volume,  // audio volume settings
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
    public SusiAction(JSONObject json) throws SusiActionException {
        this.json = json;
        // check if the action is valid. If it is not valid, throw an exception
        // {"type":"answer","select":"random","phrases":["Here is the exact location of the event $2$"]}
        if (!json.has("type")) throw new SusiActionException("the action needs a type object");
        try {
        	RenderType renderType = RenderType.valueOf(json.getString("type"));
        	switch (renderType) {
	            case answer:
	                if (!json.has("select")) throw new SusiActionException("the action needs a select object");
	                if (!json.has("phrases")) throw new SusiActionException("the action needs a phrases object");
	            	break;
	            case stop:
                    //stop has no attributes
                    break;
                case table:
	                if (!json.has("columns")) throw new SusiActionException("the action needs a columns object");
	            	    if (!(json.get("columns") instanceof JSONObject)) throw new SusiActionException("the columns object must be an json object");
	            	    if (!json.has("count")) json.put("count", -1);
	            	break;
	            case piechart:
	            	    if (!json.has("total")) throw new SusiActionException("the action needs a total object");
	            	    if (json.get("total") instanceof String)  throw new SusiActionException("the total object must be a number");
	            	    if (!json.has("key")) throw new SusiActionException("the action needs a key object");
	            	    if (!json.has("value")) throw new SusiActionException("the action needs a value object");
	            	    if (!json.has("unit")) throw new SusiActionException("the action needs a unit object");
	            	break;
	            case rss:
	                if (!json.has("title")) throw new SusiActionException("the action needs a title object");
	            	    if (!json.has("description")) throw new SusiActionException("the action needs a description object");
	            	    if (!json.has("link")) throw new SusiActionException("the action needs a link object");
	            	    if (!json.has("count")) json.put("count", -1);
	            	break;
	            case self:
	                throw new SusiActionException("this action is not yet defined");
	            case websearch:
	                if (!json.has("query")) throw new SusiActionException("the action needs a query object");
	            	break;
	            case anchor:
                    throw new SusiActionException("this action is not yet defined");
	            case map:
	                if (!json.has("latitude")) throw new SusiActionException("the action needs a latitude object");
	            	    if (json.get("latitude") instanceof String)  throw new SusiActionException("the latitude object must be a number");
	            	    if (!json.has("longitude")) throw new SusiActionException("the action needs a longitude object");
	            	    if (json.get("longitude") instanceof String)  throw new SusiActionException("the longitude object must be a number");
	            	    if (!json.has("zoom")) throw new SusiActionException("the action needs a zoom object");
	            	    if (json.get("zoom") instanceof String)  throw new SusiActionException("the zoom object must be a number");
	            	break;
	            case timer_set:
	                // the time number is the unix time, UTC. Clients must translate this into their local time.
	            	    if (!json.has("time")) throw new SusiActionException("the action needs a time object");
	            	    if (json.get("time") instanceof String)  throw new SusiActionException("the time object must be a number");
	            	break;
	            case timer_reset:
                    //timer_reset has no attributes
                    break;
	            case audio_volume:
	                throw new SusiActionException("this action is not yet defined");
	            case audio_record:
                    throw new SusiActionException("this action is not yet defined");
	            case audio_play:
	                // in most cases the identifier must be an URL. The URL can point to a file location as well.
	                // if the identifier is a file, the URL is like file:///user/admin/audio/example.mp3
	                // the type may be i..: "url", "youtube". If youtube is given then the identifier is the number of the youtube video.
	                // we could give an exact url of the youtube stream, but thay may change over time while the youtube video stays the same.
	                // There is the option to give susi_server application-relative paths. These must be relaive file URLs, like
	                // file://conf/audio/all_systems_are_go_all_lights_are_green.mp3
	                // this is translated into an absolute path during this processing
                    if (!json.has("identifier")) throw new SusiActionException("the action needs an identifier object");
                    if (!json.has("identifier_type")) throw new SusiActionException("the action needs an identifier_type object");
                    String audio_type = json.getString("identifier_type");
                    if (!audio_type.equals("url") && !audio_type.equals("youtube")) throw new SusiActionException("the identifier_type object in unknown");
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
	            	if (!json.has("identifier")) throw new SusiActionException("the action needs an identifier object");
                    if (!json.has("identifier_type")) throw new SusiActionException("the action needs an identifier_type object");
                    String video_type = json.getString("identifier_type");
                    if (!video_type.equals("url") && !video_type.equals("youtube")) throw new SusiActionException("the identifier_type object in unknown");
                break;
	            case image_take:
                    throw new SusiActionException("this action is not yet defined");
	            case image_show:
                    throw new SusiActionException("this action is not yet defined");
	            case emotion:
                    throw new SusiActionException("this action is not yet defined");
	            case button_push:
                    throw new SusiActionException("this action is not yet defined");
	            case io:
                    throw new SusiActionException("this action is not yet defined");
	            default:
	            	    throw new SusiActionException("the action type '" + renderType + "' is not handled. Extend the Action Case statement."); // if you see this exception then the case statment must be extended with the new action type
        	}
        } catch (IllegalArgumentException e) {
            throw new SusiActionException("the action type '" + json.getString("type") + "' is not known");
        }
    }

    /**
     * answer action: produces a written or spoken answer.
     * If several answers are given one of then can be picked by random by the client.
     * @param answers
     * @return the action
     */
    public static JSONObject answerAction(SusiLanguage language, String... answers) {
        JSONArray phrases = new JSONArray();
        for (String answer: answers) phrases.put(answer.trim());
        JSONObject json = new JSONObject()
            .put("type", RenderType.answer.name())
            .put("select", SelectionType.random.name())
            .put("phrases", phrases);
        if (language != SusiLanguage.unknown) json.put("language", language.name());
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

    final static Pattern visible_assignment = Pattern.compile("(?:(?:.*)[\\?\\!\\s,\\.;-]+)?([^\\^]+?)>([_a-zA-Z0-9]+)(?:[\\?\\!\\s,\\.;-](?:.*))?+");
    final static Pattern blind_assignment = Pattern.compile("(?:.*?)\\^(.*?)\\^>([_a-zA-Z0-9]+)(?:[\\?\\!\\s,\\.;-](?:.*))?+");
    final static Pattern self_referrer = Pattern.compile(".*?`([^`]*?)`.*?");
    
    /**
     * Action descriptions are templates for data content. Strings may refer to arguments from
     * a thought deduction using variable templates. I.e. "$name$" inside an action string would
     * refer to an data entity in an thought argument which has the name "name". Applying the
     * Action to a thought will instantiate such variable templates and produces a new String
     * attribute named "expression"
     * @param thoughts an argument from previously applied inferences
     * @param client the client requesting the answer
     * @param language the language of the client
     * @param minds a hierarchy of minds which overlap each other. top mind is the first element in the list of minds
     * @return the action with the attribute "expression" instantiated by unification of the thought with the action
     * @throws ReactionException
     */
    public SusiAction execution(SusiArgument thoughts, String client, SusiLanguage language, SusiMind... minds) throws ReactionException {
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
                    SusiMind.Reaction reaction = null;
                    String selfanswer = "";
                    ReactionException ee = null;
                    mindlevels: for (SusiMind mind: minds) {
                        	try {
                        		reaction = mind.new Reaction(observation, language, client, new SusiThought(), minds);
                        		selfanswer = reaction.getExpression();
                        		if (selfanswer != null && selfanswer.length() > 0) break mindlevels;
                        	} catch (ReactionException e) {
                        		ee = e;
                        		continue mindlevels;
                        	}
                    }
                    if (reaction == null || selfanswer == null || selfanswer.length() == 0)
                    	throw ee == null ? new ReactionException("could not find an answer") : ee;
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
                    	SusiMind.Reaction reaction = null;
                    expression = "";
                    ReactionException ee = null;
                    mindlevels: for (SusiMind mind: minds) {
                        	try {
                        		reaction = mind.new Reaction(expression, language, client, new SusiThought(), minds);
                        		expression = reaction.getExpression();
                        		if (expression != null && expression.length() > 0) break mindlevels;
                        	} catch (ReactionException e) {
                        		ee = e;
                        		continue mindlevels;
                        	}
                    }
                    if (reaction == null || expression == null || expression.length() == 0)
                    	throw ee == null ? new ReactionException("could not find an answer") : ee;
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
        if ((this.getRenderType() == RenderType.video_play || this.getRenderType() == RenderType.audio_play) && this.json.has("identifier")) {
            this.json.put("identifier", thoughts.unify(getStringAttr("identifier"), false));
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
