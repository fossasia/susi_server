/**
 *  SusiExpert
 *  Copyright 06.06.2017 by Michael Peter Christen, @0rb1t3r
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.susi.mind.SusiInference.Type;

/**
 * A susi expert is a set of skills.
 * This class provides parser methods for such sets, given as text files
 */
public class SusiExpert {


    /**
     * read an "EzD" ('Easy Dialog') file: this is just a text file. Read the docs/susi_skill_development_tutorial.md for an explanation
     * @param br
     * @return
     * @throws JSONException
     * @throws FileNotFoundException
     */
    public static JSONObject readEzDExpert(BufferedReader br) throws JSONException {
        // read the text file and turn it into a skill json; then learn that
        JSONObject json = new JSONObject();
        JSONArray skills = new JSONArray();
        json.put("skills", skills);
        String lastLine = "", line = "";
        String bang_phrases = "", bang_type = "", bang_term = ""; StringBuilder bang_bag = new StringBuilder();
        boolean prior = false;
        try {readloop: while ((line = br.readLine()) != null) {
            line = line.trim();
            
            if (bang_type.length() > 0) {
                // collect a bang
                if (line.toLowerCase().equals("eol")) {
                    // stop collection
                    if (bang_type.equals("javascript")) {
                        // create a javascript skill
                        JSONObject skill = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        skill.put("phrases", phrases);
                        for (String phrase: bang_phrases.split("\\|")) phrases.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
                        
                        // javascript process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.javascript.name());
                        process.put("expression", bang_bag.toString());
                        skill.put("process", new JSONArray().put(process));
                        
                        // answers; must contain $!$
                        skill.put("actions", new JSONArray().put(SusiAction.answerAction(bang_term.split("\\|"))));
                        skills.put(skill);
                    }
                    if (bang_type.equals("console")) {
                        // create a console skill
                        JSONObject skill = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        skill.put("phrases", phrases);
                        for (String phrase: bang_phrases.split("\\|")) phrases.put(SusiPhrase.simplePhrase(phrase.trim(), prior));
                        
                        // console process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.console.name());
                        JSONObject bo = new JSONObject(new JSONTokener(bang_bag.toString()));
                        if (bo.has("url") && bo.has("path")) {
                            JSONObject definition = new JSONObject().put("url", bo.get("url")).put("path", bo.get("path"));
                            process.put("definition", definition);
                            skill.put("process", new JSONArray().put(process));
                            
                            // actions; we may have several actions here
                            JSONArray actions = new JSONArray();
                            skill.put("actions", actions);
                            
                            // answers; must contain names from the console result array
                            if (bang_term.length() > 0) {
                                actions.put(SusiAction.answerAction(bang_term.split("\\|")));
                            }

                            // optional additional renderings
                            if (bo.has("actions")) {
                                JSONArray bo_actions = bo.getJSONArray("actions");
                                bo_actions.forEach(action -> {
                                    JSONObject boa = (JSONObject) action;
                                    String type = boa.has("type") ? boa.getString("type") : "";
                                    if (type.equals(SusiAction.RenderType.table.toString()) && boa.has("columns")) {
                                        actions.put(SusiAction.tableAction(boa.getJSONObject("columns"),
                                                    boa.has("length") ? boa.getInt("length") : -1));
                                    } else
                                    if (type.equals(SusiAction.RenderType.piechart.toString()) &&
                                            boa.has("total") && boa.has("key") &&
                                            boa.has("value") && boa.has("unit")) {
                                        actions.put(SusiAction.piechartAction(
                                                boa.getInt("total"), boa.getString("key"),
                                                boa.getString("value"), boa.getString("unit")));
                                    } else
                                    if (type.equals(SusiAction.RenderType.rss.toString()) &&
                                            boa.has("title") && boa.has("description") && boa.has("link")) {
                                        actions.put(SusiAction.rssAction(
                                            boa.getString("title"), boa.getString("description"), boa.getString("link"),
                                            boa.has("length") ? boa.getInt("length") : -1));
                                    } else
                                    if (type.equals(SusiAction.RenderType.websearch.toString()) && boa.has("query")) {
                                        actions.put(SusiAction.websearchAction(boa.getString("query")));
                                    } else
                                    if (type.equals(SusiAction.RenderType.map.toString()) &&
                                            boa.has("latitude") && boa.has("longitude") && boa.has("zoom")) {
                                        actions.put(SusiAction.mapAction(
                                            boa.getDouble("latitude"), boa.getDouble("longitude"), boa.getInt("zoom")));
                                    }
                                });
                            }
                            skills.put(skill);
                        }
                    }
                    bang_phrases = "";
                    bang_type = "";
                    bang_term = "";
                    bang_bag.setLength(0);
                }
                bang_bag.append(line).append('\n');
                continue readloop;
            }
            
            // read metadata
            if (line.startsWith("::")) {
                line = line.toLowerCase();
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                lastLine = "";
                continue readloop;
            }
            
            if (line.startsWith("#")) {
                lastLine = "";
                continue readloop;
            }
            
            // read content
            if (line.length() > 0 && lastLine.length() > 0) {
                // mid of conversation (last answer is query for next skill)
                String[] phrases = lastLine.split("\\|");
                String condition = null;
                int thenpos = -1;
                if (line.startsWith("?") && (thenpos = line.indexOf(':')) > 0) {
                    int elsepos = line.substring(thenpos + 1).indexOf(':') + thenpos + 1;
                    condition = line.substring(1, thenpos).trim();
                    if (elsepos <= thenpos) {
                        // only if, no else
                        String ifsubstring = line.substring(thenpos + 1).trim();
                        if (ifsubstring.length() > 0) {
                            String[] answers = ifsubstring.split("\\|");
                            JSONObject skill = SusiSkill.answerSkill(phrases, "IF " + condition, answers, prior);
                            skills.put(skill);
                        }
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (ifsubstring.length() > 0) {
                            String[] ifanswers = ifsubstring.split("\\|");
                            JSONObject skillif = SusiSkill.answerSkill(phrases, "IF " + condition, ifanswers, prior);
                            skills.put(skillif);
                        }
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            JSONObject skillelse = SusiSkill.answerSkill(phrases, "NOT " + condition, elseanswers, prior);
                            skills.put(skillelse);
                        }
                    }
                } else if (line.startsWith("!") && (thenpos = line.indexOf(':')) > 0) {
                    bang_phrases = lastLine;
                    bang_type = line.substring(1, thenpos).trim().toLowerCase();
                    bang_term = line.substring(thenpos + 1).trim();
                    bang_bag.setLength(0);
                    continue readloop;
                } else {
                    String[] answers = line.split("\\|");
                    JSONObject skill = SusiSkill.answerSkill(phrases, condition, answers, prior);
                    //System.out.println(skill.toString());
                    skills.put(skill);
                }
            }
            lastLine = line;
        }} catch (IOException e) {}
        return json;
    }
    

    public static JSONObject readJsonExpert(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return json;
    }
    
    
    public static JSONObject readAIMLExpert(File file) throws Exception {
        // read the file as string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        StringBuilder buf=new StringBuilder();
        while ((str = br.readLine()) != null) buf.append(str);
        br.close();
        
        // parse the string as xml into a node object
        InputStream is = new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();
        Node root = doc.getDocumentElement();
        Node node = root;
        NodeList nl = node.getChildNodes();
        JSONObject json = new JSONObject();
        JSONArray skills = new JSONArray();
        json.put("skills", skills);
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                JSONObject skill = readAIMLCategory(nl.item(i));
                if (skill != null && skill.length() > 0) skills.put(skill);
            }
            System.out.println("ROOT NODE " + nl.item(i).getNodeName());
        }
        return json;
    }
    
    public static JSONObject readAIMLCategory(Node category) {
        NodeList nl = category.getChildNodes();
        String[] phrases = null;
        String[] answers = null;
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            System.out.println("CATEGORYY NODE " + nl.item(i).getNodeName());
            if (nodename.equals("pattern")) {
                phrases = readAIMLSentences(nl.item(i));
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                answers = readAIMLSentences(nl.item(i));
            }
        }
        if (phrases != null && answers != null) {
            return SusiSkill.answerSkill(phrases, null, answers, false);
        }
        return null;
    }
    
    public static String[] readAIMLSentences(Node pot) {
        NodeList nl = pot.getChildNodes();
        JSONObject json = new JSONObject();
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            System.out.println("SENTENCE NODE " + nl.item(i).getNodeName());
            if (nodename.equals("pattern")) {
                
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                
            }
        }
        return null;
    }
}
