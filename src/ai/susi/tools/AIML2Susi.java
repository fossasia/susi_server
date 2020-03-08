/**
 *  AIML2Susi
 *  Copyright 27.11.2017 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.susi.mind.SusiIntent;
import ai.susi.mind.SusiLanguage;
import ai.susi.mind.SusiSkill;
import ai.susi.mind.SusiUtterance;
import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiAction.SusiActionException;

public class AIML2Susi {

    public static List<SusiIntent> readAIMLSkill(File file, SusiLanguage language) throws Exception {
        SusiSkill.ID id = new SusiSkill.ID(language, file.getName());
        // read the file as string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        StringBuilder buf=new StringBuilder();
        while ((str = br.readLine()) != null) buf.append(str);
        br.close();

        // parse the string as xml into a node object
        InputStream is = new ByteArrayInputStream(buf.toString().getBytes(StandardCharsets.UTF_8));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        //System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

        Node root = doc.getDocumentElement();
        NodeList nl = root.getChildNodes();
        JSONObject json = new JSONObject();
        List<SusiIntent> intents = new ArrayList<>();
        json.put("intents", intents);
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                SusiIntent intent = readAIMLCategory(node, language, id);
                if (intent != null) intents.add(intent);
            } else if (nodename.equals("#comment")) {
            } else if (nodename.equals("#text")) {
            } else if (nodename.equals("topic")) {
            } else {
                System.out.println("unknown aiml nodename: " + nodename); // hack until this disappears
            }
            //System.out.println("ROOT NODE " + nl.item(i).getNodeName());
        }
        return intents;
    }

    private static int countStars(String utterance) {
        int c = 0;
        for (int i = 0; i < utterance.length(); i++) if (utterance.charAt(i) == '*') c++;
        return c;
    }
    
    public static SusiIntent readAIMLCategory(Node category, SusiLanguage language, SusiSkill.ID id) throws SusiActionException {
        NodeList nl = category.getChildNodes();
        List<String> phrases = null;
        String[] answers = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            //System.out.println("CATEGORYY NODE " + node.getNodeName());
            if (nodename.equals("pattern")) {
                phrases = readAIMLPattern(node.getChildNodes());
            } else if (nodename.equals("that")) {
            } else if (nodename.equals("template")) {
                answers = readAIMLTemplate(node.getChildNodes(), true, phrases.size() == 0 ? 0 : countStars(phrases.get(0)));
            } else if (nodename.equals("that")) {
            } else if (nodename.equals("#text")) {
            } else if (nodename.equals("category")) {
            } else {
                System.out.println("unknown category nodename: " + nodename); // hack until this disappears
            }
        }
        if (phrases != null && answers != null) {
            List<SusiUtterance> utterances = new ArrayList<>();
            phrases.forEach(phrase -> utterances.add(new SusiUtterance(phrase, false, 0)));
            SusiIntent intent = new SusiIntent(utterances, false, 0, id);
            JSONObject answerActionObject = SusiAction.answerAction(0, language, answers);
            SusiAction answerAction = new SusiAction(answerActionObject);
            intent.addAction(answerAction);
            return intent;
        }
        return null;
    }

    public static List<String> readAIMLPattern(NodeList nl) {
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            String nodevalue = node.getNodeValue();
            if (nodename.equals("#text")) {
                sentences.add(nodevalue);
            } else if (nodename.equals("bot")) {
            } else {
                System.out.println("unknown pattern nodename: " + nodename); // hack until this disappears
            }
        }
        return sentences;
    }

    public static String[] readAIMLTemplate(NodeList nl, boolean visible, int stars) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            String nodevalue = node.getNodeValue();

            // see https://www.pandorabots.com/docs/aiml-reference/ as reference
            // the following conditions are listed in reverse order of occurrences
            if (nodename.equals("#text")) {
                if (nodevalue == null) nodevalue = "";
                nodevalue = nodevalue.trim();
                if (nodevalue.length() > 0) sb.append(nodevalue);
            } else if (nodename.equals("srai")) {
                String[] srai = readAIMLTemplate(node.getChildNodes(), visible, stars);
                if (srai == null || srai.length == 0) continue;
                if (srai.length != 1) throw new RuntimeException("unexpected number of srai nodes: " + srai.length);
                sb.append(" `" + srai[0] + "` ");
            } else if (nodename.equals("think")) {
                String[] invisible = readAIMLTemplate(node.getChildNodes(), false, stars);
                if (invisible == null || invisible.length == 0) continue;
                if (invisible.length != 1) throw new RuntimeException("unexpected number of invisible nodes: " + invisible.length);
                String set = invisible[0];
                int p = set.indexOf('>');
                if (p >= 0) set = "^" + set.substring(0, p).trim() +"^" + set.substring(p);
                sb.append(" " + set + " ");
            } else if (nodename.equals("set")) {
                String[] set = readAIMLTemplate(node.getChildNodes(), visible, stars);
                if (set == null || set.length == 0) continue;
                if (set.length != 1) throw new RuntimeException("unexpected number of set nodes: " + set.length);
                if (set[0].indexOf('>') >= 0) {sb.append(" " + set[0] + " "); continue;} // we do not do a set in set
                NamedNodeMap attributes = node.getAttributes();
                String[] items = readAIMLTemplate(attributes.getNamedItem("name").getChildNodes(), visible, stars);
                if (items == null || items.length == 0) continue;
                if (items.length != 1) throw new RuntimeException("unexpected number of items nodes: " + items.length);
                sb.append(" " + set[0] + ">" + items[0] + " ");
            } else if (nodename.equals("random")) {
            } else if (nodename.equals("person")) {
            } else if (nodename.equals("bot")) {
            } else if (nodename.equals("get")) {
            } else if (nodename.equals("br")) {
            } else if (nodename.equals("#comment")) {
            } else if (nodename.equals("formal")) {
            } else if (nodename.equals("sr")) {
            } else if (nodename.equals("that")) {
            } else if (nodename.equals("em")) {
            } else if (nodename.equals("star")) {
                String s = sb.toString();
                int c = 1;
                while (s.indexOf("$" + c + "$") >= 0 && c < stars) c++;
                sb.append(" $" + c + "$ ");
            } else if (nodename.equals("a")) {
            } else if (nodename.equals("date")) {
            } else if (nodename.equals("condition")) {
            } else if (nodename.equals("input")) {
            } else if (nodename.equals("thatstar") || nodename.equals("thastar")) {
            } else if (nodename.equals("p")) {
            } else if (nodename.equals("id")) {
            } else if (nodename.equals("size")) {
            } else if (nodename.equals("img")) {
            } else if (nodename.equals("uppercase")) {
            } else if (nodename.equals("ul")) {
            } else if (nodename.equals("get_likes")) {
            } else if (nodename.equals("version")) {
            } else {
                System.out.println("unknown template nodename: " + nodename); // hack until this disappears
            }
        }
        return new String[]{sb.toString().trim()};
    }

    private static void loadFiles(File path) {
        if (path.isDirectory()) {
            File[] list = path.listFiles();
            for (File f : list) {
                loadFiles(f);
            }
        } else {
            if (path.getName().endsWith(".aiml"))
                try {
                    List<SusiIntent> intents = readAIMLSkill(path, SusiLanguage.en);
                    System.out.println("AIML: " + path);
                    intents.forEach(intent -> System.out.print(intent.toLoT()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    public static void main(String[] args) {
        File archive = new File("/Users/admin/git/AIML_Archive/aiml/");
        loadFiles(archive);
    }
}
