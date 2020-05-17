/**
 *  EtherpadClient
 *  Copyright 17.05.2020 by Michael Peter Christen, @0rb1t3r
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;

public class EtherpadClient {

    String etherpadApikey, etherpadUrlstub;

    public EtherpadClient() throws IOException {
        // get API key from local etherpad; first find the etherpad installation
        File local_etherpad_apikey_file = new File(new File(new File(System.getProperty("user.home"), "SUSI.AI"), "etherpad-lite"), "APIKEY.txt");
        if (!local_etherpad_apikey_file.exists()) local_etherpad_apikey_file = new File(new File(DAO.data_dir, "etherpad-lite"), "APIKEY.txt");
        if (!local_etherpad_apikey_file.exists()) {
            // get API key from configured etherpad
            this.etherpadApikey = DAO.getConfig("etherpad.apikey", "");
            this.etherpadUrlstub = DAO.getConfig("etherpad.urlstub", "");
        } else {
            // read the key file
            FileInputStream fis = new FileInputStream(local_etherpad_apikey_file);
            byte[] data = new byte[(int) local_etherpad_apikey_file.length()];
            fis.read(data);
            fis.close();
            this.etherpadApikey = new String(data, "UTF-8");
            this.etherpadUrlstub = "http://localhost:9001";
        }

    }

    /**
     * read a text from a pad
     * @param padID the name of the pad
     * @return the plain text
     * @throws IOException
     */
    public String getText(String padID) throws IOException {
        String padurl = this.etherpadUrlstub + "/api/1/getText?apikey=" + this.etherpadApikey + "&padID=" + padID;
        Map<String, String> request_header = new HashMap<>();
        request_header.put("Accept","application/json");
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadGet(padurl, request_header)));
        JSONObject json = new JSONObject(serviceResponse);
        JSONObject data = json.optJSONObject("data");
        if (data == null) throw new IOException("bad data from pad: null");
        String text = data.getString("text");
        return text;
    }

    /**
     * write a default text to a pad in case the pad is empty or does not exist
     * @param padID the name of the pad
     * @param contentFile the file containing the default text
     * @return the text inside the pad after calling the method - either existing content or new content
     * @throws IOException
     */
    public String setTextIfEmpty(String padID, File contentFile) throws IOException {
        String text = null;
        try {
            text = getText(padID);
        } catch (Exception e) {}
        if (padIsEmpty(text)) text = createPad(padID);
        if (padIsEmptyOrDefault(text)) {
            // fill the pad with a default skill, a set of examples
            // read the examples file
            String content = new String(readFile(contentFile), StandardCharsets.UTF_8);

            // write the pad
            setText(padID, content);

            // read the content again
            text = getText(padID);
        }
        return text;
    }

    /**
     * Create a new pad
     * @param padID the name of the pad
     * @return the content of the new pad (usually a welcome text)
     * @throws IOException
     */
    public String createPad(String padID) throws IOException {
        Map<String, String> request_header = new HashMap<>();
        request_header.put("Accept","application/json");
        String createurl = this.etherpadUrlstub + "/api/1/createPad?apikey=" + this.etherpadApikey + "&padID=" + padID;
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadGet(createurl, request_header)));
        JSONObject json = new JSONObject(serviceResponse);
        JSONObject data = json.optJSONObject("data");
        assert data != null; // because we created the pad!
        String text = data == null ? "" : data.getString("text").trim();
        return text;
    }

    /**
     * Set a pad content
     * @param padID the name of the pad
     * @param content the text of the content
     * @throws IOException
     */
    public void setText(String padID, String text) throws IOException {
        // write the pad
        String writeurl = this.etherpadUrlstub + "/api/1/setText";
        Map<String, byte[]> p = new HashMap<>();
        p.put("apikey", this.etherpadApikey.getBytes(StandardCharsets.UTF_8));
        p.put("padID", padID.getBytes(StandardCharsets.UTF_8));
        p.put("text", text.getBytes(StandardCharsets.UTF_8));
        HttpClient.loadPost(writeurl, p);
    }

    private String createAuthorIfNotExistsFor(String name) throws IOException {
        String authorMapper = Integer.toString(Math.abs(name.hashCode()));
        String writeurl = this.etherpadUrlstub + "/api/1.2.13/createAuthorIfNotExistsFor";
        Map<String, byte[]> p = new HashMap<>();
        p.put("apikey", this.etherpadApikey.getBytes(StandardCharsets.UTF_8));
        p.put("authorMapper", authorMapper.getBytes(StandardCharsets.UTF_8));
        p.put("name", name.getBytes(StandardCharsets.UTF_8));
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadPost(writeurl, p)));
        JSONObject json = new JSONObject(serviceResponse);
        JSONObject data = json.optJSONObject("data");
        assert data != null; // because we created the pad!
        String authorID = data == null ? "" : data.getString("authorID").trim();
        return authorID;
    }

    public void appendChatMessage(String padID, String text, String name) throws IOException {
        String authorID = createAuthorIfNotExistsFor(name);
        String writeurl = this.etherpadUrlstub + "/api/1.2.13/appendChatMessage";
        Map<String, byte[]> p = new HashMap<>();
        p.put("apikey", this.etherpadApikey.getBytes(StandardCharsets.UTF_8));
        p.put("padID", padID.getBytes(StandardCharsets.UTF_8));
        p.put("text", text.getBytes(StandardCharsets.UTF_8));
        p.put("authorID", authorID.getBytes(StandardCharsets.UTF_8));
        HttpClient.loadPost(writeurl, p);
    }

    /**
     * check if a pad is empty
     * @param content
     * @return
     */
    public static boolean padIsEmpty(String content) {
        return content == null || content.trim().length() == 0;
    }

    /**
     * check if a pad is empty or has a default content (extended view of "is empty")
     * @param content
     * @return
     */
    public static boolean padIsEmptyOrDefault(String content) {
        return padIsEmpty(content) || content.startsWith("Welcome to Etherpad!");
    }

    /**
     * Check if a pad contains a skill
     * @param content
     * @return
     */
    public static boolean padContainsSkill(String content) {
        return !padIsEmptyOrDefault(content) && !content.startsWith("disabled");
    }

    /**
     * Read the content of a file as byte[]
     * @param f the file
     * @return byte[] of the content
     * @throws IOException
     */
    public static byte[] readFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] content = new byte[(int) f.length()];
        fis.read(content);
        fis.close();
        return content;
    }

}
