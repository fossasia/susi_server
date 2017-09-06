/**
 *  SusiSkill
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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.susi.DAO;
import ai.susi.mind.SusiInference.Type;

/**
 * A susi skill is a set of intents.
 * This class provides parser methods for such sets, given as text files
 */
public class SusiSkill {

    private String skillName;
    private String description;
    private String author;
    private String authorURL;
    private String image;
    private String termsOfUse;
    private Set<String> examples;
    private String developerPrivacyPolicy;
    private Boolean dynamicContent;

    /**
     * read an "EzD" ('Easy Dialog') file: this is just a text file. Read the docs/susi_skill_development_tutorial.md for an explanation
     * @param br
     * @return
     * @throws JSONException
     * @throws FileNotFoundException
     */

    public SusiSkill() {
        this.author = null;
        this.authorURL = null;
        this.description = null;
        this.examples = new LinkedHashSet<>();
        this.image = null;
        this.skillName = null;
        this.termsOfUse = null;
        this.developerPrivacyPolicy = null;
        this.dynamicContent = false;
    }
    public static JSONObject readEzDSkill(BufferedReader br) throws JSONException {
        // read the text file and turn it into a intent json; then learn that
        JSONObject json = new JSONObject();
        JSONArray intents = new JSONArray();
        json.put("intents", intents);
        String lastLine = "", line = "";
        String bang_answers = "", bang_type = "", bang_term = ""; StringBuilder bang_bag = new StringBuilder();
        String example = "", expect = "", description="", image="", skillName="", authorName= "", authorURL = "", developerPrivacyPolicy = "", termsOfUse="";
        boolean prior = false, dynamicContent = false;
        try {readloop: while ((line = br.readLine()) != null) {
            line = line.trim();
            
            if (bang_type.length() > 0) {
                // collect a bang
                if (line.toLowerCase().equals("eol")) {
                    // stop collection
                    if (bang_type.equals("javascript")) {
                        // create a javascript intent
                        JSONObject intent = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        intent.put("phrases", phrases);
                        for (String phrase: bang_answers.split("\\|")) phrases.put(SusiUtterance.simplePhrase(phrase.trim(), prior));
                        
                        // javascript process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.javascript.name());
                        process.put("expression", bang_bag.toString());
                        intent.put("process", new JSONArray().put(process));
                        
                        // answers; must contain $!$
                        intent.put("actions", new JSONArray().put(SusiAction.answerAction(bang_term.split("\\|"))));
                        if (example.length() > 0) intent.put("example", example);
                        intents.put(intent);
                    }
                    else if (bang_type.equals("console")) {
                        // create a console intent
                        JSONObject intent = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        intent.put("phrases", phrases);
                        for (String phrase: bang_answers.split("\\|")) phrases.put(SusiUtterance.simplePhrase(phrase.trim(), prior));
                        
                        // console process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.console.name());
                        JSONObject bo = new JSONObject(new JSONTokener(bang_bag.toString()));
                        if (bo.has("url") && bo.has("path")) {
                            JSONObject definition = new JSONObject().put("url", bo.get("url")).put("path", bo.get("path"));
                            process.put("definition", definition);
                            intent.put("process", new JSONArray().put(process));
                            
                            // actions; we may have several actions here
                            JSONArray actions = new JSONArray();
                            intent.put("actions", actions);
                            
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
                                    } else
                                    if(type.equals(SusiAction.RenderType.timer_set.toString()) &&
                                            boa.has("hour")){
                                        int hour = boa.getInt("hour");
                                            actions.put(SusiAction.timerSetAction(
                                                    hour, boa.has("minute") ? boa.getInt("minute") : 0,
                                                    boa.has("second") ? boa.getInt("second") : 0));
                                    }
                                });
                            }
                            if (example.length() > 0) intent.put("example", example);
                            if (expect.length() > 0) intent.put("expect", expect);
                            intents.put(intent);
                        }
                    }
                    bang_answers = "";
                    bang_type = "";
                    bang_term = "";
                    bang_bag.setLength(0);
                }
                bang_bag.append(line).append('\n');
                continue readloop;
            }
            
            // read metadata
            if (line.startsWith("::")) {
                int thenpos=-1;
//                line = line.toLowerCase();
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                if (line.startsWith("::description") && (thenpos = line.indexOf(' ')) > 0) {
                    description = line.substring(thenpos + 1).trim();
                    if(description.length() > 0)
                        json.put("description",description);
                   // System.out.println(description);
                }
                if (line.startsWith("::image") && (thenpos = line.indexOf(' ')) > 0) {
                    image = line.substring(thenpos + 1).trim();
                    if(image.length() > 0)
                        json.put("image",image);
                }
                if (line.startsWith("::name") && (thenpos = line.indexOf(' ')) > 0) {
                    skillName = line.substring(thenpos + 1).trim();
                    if(skillName.length() > 0)
                        json.put("skill_name",skillName);
                }
                if (line.startsWith("::author") && (!line.startsWith("::author_url")) && (thenpos = line.indexOf(' ')) > 0) {
                   authorName = line.substring(thenpos + 1).trim();
                    if(authorName.length() > 0)
                        json.put("author",authorName);
                }
                if (line.startsWith("::author_url") && (thenpos = line.indexOf(' ')) > 0) {
                    authorURL = line.substring(thenpos + 1).trim();
                    if(authorURL.length() > 0)
                        json.put("author_url",authorURL);
                }
                if (line.startsWith("::developer_privacy_policy") && (thenpos = line.indexOf(' ')) > 0) {
                    developerPrivacyPolicy = line.substring(thenpos + 1).trim();
                    if(developerPrivacyPolicy.length() > 0)
                        json.put("developer_privacy_policy",developerPrivacyPolicy);
                }
                if (line.startsWith("::terms_of_use") && (thenpos = line.indexOf(' ')) > 0) {
                    termsOfUse = line.substring(thenpos + 1).trim();
                    if(termsOfUse.length() > 0)
                        json.put("terms_of_use",termsOfUse);
                }
                if (line.startsWith("::dynamic_content") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) dynamicContent=true;
                    json.put("dynamic_content",dynamicContent);
                }

                lastLine = ""; example = ""; expect = "";
                continue readloop;
            }
            
            if (line.startsWith("#")) {
                // a comment line; ignore the line and consider it as whitespace
                lastLine = ""; example = ""; expect = "";
                continue readloop;
            }
            
            // read content
            if (line.length() > 0 && lastLine.length() > 0) {
                // mid of conversation (last answer is query for next intent)
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
                            JSONObject intent = SusiIntent.answerIntent(phrases, "IF " + condition, answers, prior, example, expect);
                            intents.put(intent);
                        }
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (ifsubstring.length() > 0) {
                            String[] ifanswers = ifsubstring.split("\\|");
                            JSONObject intentif = SusiIntent.answerIntent(phrases, "IF " + condition, ifanswers, prior, example, expect);
                            intents.put(intentif);
                        }
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            JSONObject intentelse = SusiIntent.answerIntent(phrases, "NOT " + condition, elseanswers, prior, example, expect);
                            intents.put(intentelse);
                        }
                    }
                } else if (line.startsWith("!") && (thenpos = line.indexOf(':')) > 0) {
                    String head = line.substring(1, thenpos).trim().toLowerCase();
                    String tail = line.substring(thenpos + 1).trim();
                    // test bang type
                    if (head.equals("example")) {
                        example = tail;
                    } else if (head.equals("expect")) {
                        expect = tail;
                    }
                    else if (head.equals("image")) {
                        image =tail;
                    } else {
                        // start multi-line bang
                        bang_answers = lastLine;
                        bang_type = head;
                        bang_term = tail;
                        bang_bag.setLength(0);
                    }
                    continue readloop;
                } else {
                    String[] answers = line.split("\\|");
                    JSONObject intent = SusiIntent.answerIntent(phrases, condition, answers, prior, example, expect);
                    //System.out.println(intent.toString());
                    intents.put(intent);
                }
            }
            lastLine = line;
        }} catch (IOException e) {
            DAO.log(e.getMessage());
        }
        return json;
    }
    

    public static JSONObject readJsonSkill(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return json;
    }
    
    
    public static JSONObject readAIMLSkill(File file) throws Exception {
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
        JSONArray intents = new JSONArray();
        json.put("intents", intents);
        for (int i = 0; i < nl.getLength(); i++) {
            String nodename = nl.item(i).getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                JSONObject intent = readAIMLCategory(nl.item(i));
                if (intent != null && intent.length() > 0) intents.put(intent);
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
            return SusiIntent.answerIntent(phrases, null, answers, false, null, null);
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
    public void setAuthor(String author) {
        this.author = author;
    }

    public void setAuthorURL(String authorURL) {
        this.authorURL = authorURL;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeveloperPrivacyPolicy(String developerPrivacyPolicy) {
        this.developerPrivacyPolicy = developerPrivacyPolicy;
    }

    public void setExamples(Set<String> examples) {
        this.examples = examples;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setDynamicContent(Boolean dynamicContent) {
        this.dynamicContent = dynamicContent;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorURL() {
        return authorURL;
    }

    public String getImage() {
        return image;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public Set<String> getExamples() {
        return examples;
    }

    public String getDeveloperPrivacyPolicy() {
        return developerPrivacyPolicy;
    }

    public Boolean getDynamicContent() {
        return dynamicContent;
    }
}
