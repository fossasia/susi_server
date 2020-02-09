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

        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        
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
            }
            //System.out.println("ROOT NODE " + nl.item(i).getNodeName());
        }
        return json;
    }
    
    public static JSONObject readAIMLCategory(Node category) {
        NodeList nl = category.getChildNodes();
        String[] phrases = null;
        String[] answers = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            //System.out.println("CATEGORYY NODE " + node.getNodeName());
            if (nodename.equals("pattern")) {
                phrases = readAIMLSentences(node);
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                answers = readAIMLSentences(node);
            }
        }
        if (phrases != null && answers != null) {
            return SusiIntent.answerIntent(phrases, null, answers, false, 0, null, null, null, null, SusiLanguage.unknown);
        }
        return null;
    }
    
    public static String[] readAIMLSentences(Node pot) {
        NodeList nl = pot.getChildNodes();
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            String nodename = node.getNodeName().toLowerCase();
            //System.out.println("SENTENCE NODE " + node.getNodeName());
            if (nodename.equals("pattern")) {
                sentences.add(node.getTextContent());
            } else if (nodename.equals("that")) {
                
            } else if (nodename.equals("template")) {
                
            }
        }
        return sentences.toArray(new String[sentences.size()]);
    }
    
    public static void main(String[] args) {
    	File archive = new File("/Users/admin/git/AIMLemotions");
    	String[] list = archive.list();
    	for (String f: list) {
    		if (! f.endsWith(".aiml")) continue;
    		try {
				JSONObject j = readAIMLSkill(new File(archive, f));
				System.out.println("AIML: " + f);
				System.out.println(j.toString(2));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }
}
