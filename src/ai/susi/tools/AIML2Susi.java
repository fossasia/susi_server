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

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.susi.mind.SusiIntent;
import ai.susi.mind.SusiLanguage;

public class AIML2Susi {

    public static JSONObject readAIMLSkill(File file) throws Exception {
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
        JSONArray intents = new JSONArray();
        json.put("intents", intents);
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            if (nodename.equals("category")) {
                JSONObject intent = readAIMLCategory(node);
                if (intent != null && intent.length() > 0) intents.put(intent);
            } else if (nodename.equals("#comment")) {
            } else if (nodename.equals("#text")) {
            } else if (nodename.equals("topic")) {
            } else {
                System.out.println("unknown aiml nodename: " + nodename); // hack until this disappears
            }
            //System.out.println("ROOT NODE " + nl.item(i).getNodeName());
        }
        return json;
    }

    public static JSONObject readAIMLCategory(Node category) {
        NodeList nl = category.getChildNodes();
        List<String> phrases = null;
        List<String> answers = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            //System.out.println("CATEGORYY NODE " + node.getNodeName());
            if (nodename.equals("pattern")) {
                phrases = readAIMLPattern(node.getChildNodes());
            } else if (nodename.equals("template")) {
                answers = readAIMLTemplate(node.getChildNodes(), true);
            } else if (nodename.equals("that")) {
            } else if (nodename.equals("#text")) {
            } else if (nodename.equals("category")) {
            } else {
                System.out.println("unknown category nodename: " + nodename); // hack until this disappears
            }
        }
        if (phrases != null && answers != null) {
            return SusiIntent.answerIntent(phrases.toArray(new String[phrases.size()]), null, answers.toArray(new String[answers.size()]), false, 0, null, null, null, null, SusiLanguage.unknown);
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

    public static List<String> readAIMLTemplate(NodeList nl, boolean visible) {
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            String nodevalue = node.getNodeValue();

            // see https://www.pandorabots.com/docs/aiml-reference/ as reference
            // the following conditions are listed in reverse order of occurrences
            if (nodename.equals("#text")) {
                if (nodevalue == null) nodevalue = "";
                nodevalue = nodevalue.trim();
                if (nodevalue.length() > 0) sentences.add(nodevalue);
            } else if (nodename.equals("srai")) {
                List<String> srai = readAIMLTemplate(node.getChildNodes(), visible);
                if (srai.size() != 1) throw new RuntimeException("unexpected number of srai nodes: " + srai.size());
                sentences.add("`" + srai.get(0) + "`");
            } else if (nodename.equals("think")) {
                System.out.println("unknown template node function: " + nodename);
                List<String> invisible = readAIMLTemplate(node.getChildNodes(), false);
                sentences.addAll(invisible);
            } else if (nodename.equals("set")) {
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
        return sentences;
    }

    public static void main(String[] args) {
        File archive = new File("/Users/admin/git/AIMLemotions");
        String[] list = archive.list();
        for (String f: list) {
            if (! f.endsWith(".aiml")) continue;
            try {
                JSONObject j = readAIMLSkill(new File(archive, f));
                //System.out.println("AIML: " + f);
                //System.out.println(j.toString(2));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
