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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;

public class EtherpadClient {

    private File etherpadHome, dirtyDB;
    private String etherpadApikey, etherpadUrlstub;
    private boolean isPrivate;

    public EtherpadClient() throws IOException {
        // get API key from local etherpad; first find the etherpad installation

        this.etherpadHome = new File(DAO.data_dir, "etherpad-lite");
        this.dirtyDB = new File(new File(this.etherpadHome, "var"), "dirty.db");
        dirtyDBHealthCheck();
        File local_etherpad_apikey_file = new File(this.etherpadHome, "APIKEY.txt");
        if (!local_etherpad_apikey_file.exists()) {
            // get API key from configured etherpad
            this.etherpadApikey = DAO.getConfig("etherpad.apikey", "");
            this.etherpadUrlstub = DAO.getConfig("etherpad.urlstub", "");
            this.isPrivate = this.etherpadUrlstub.startsWith("http://localhost");
        } else {
            // read the key file
            FileInputStream fis = new FileInputStream(local_etherpad_apikey_file);
            byte[] data = new byte[(int) local_etherpad_apikey_file.length()];
            fis.read(data);
            fis.close();
            this.etherpadApikey = new String(data, "UTF-8");
            this.etherpadUrlstub = "http://localhost:9001";
            this.isPrivate = true;
        }
    }

    public void dirtyDBHealthCheck() {
        if (!this.dirtyDB.exists()) return;
        try {
            List<String> s = Files.readAllLines(this.dirtyDB.toPath());
            String l;
            if (s.size() == 0 || (l = s.get(s.size() - 1)).length() == 0 || l.charAt(0) == '{') return;
            // start healing
            while (s.size() > 0 && (l = s.get(s.size() - 1)).length() > 0 && l.charAt(0) != '{') s.remove(s.size() - 1);
            Files.write(this.dirtyDB.toPath(), s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if this etherpad is private. A etherpad is private if it is running at localhost
     * @return true if the etherpad is running at localhost
     */
    public boolean isPrivate() {
        return this.isPrivate;
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
        try {
            JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadGet(padurl, request_header)));
            JSONObject json = new JSONObject(serviceResponse);
            JSONObject data = json.optJSONObject("data");
            if (data == null) throw new IOException("bad data from pad: null");
            String text = data.getString("text");
            backup(padID, text);
            return text;
        } catch (IOException e) {
            // try a restore
            try {
                String text = restore(padID);
                // because this is new to the pad, store it there
                if (text.length() > 0) try {setText(padID, text);} catch (IOException eee) {}
                return text;
            } catch (IOException ee) {
                // now throw the original error
                throw e;
            }
        }
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
        if (padDoesNotExist(text)) text = createPad(padID);
        if (padDoesNotExistOrIsEmptyOrDefault(text)) {
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

    public File backupFile(String padID) {
        File p = this.dirtyDB.getParentFile();
        File b = new File(p, "pad." + padID + ".txt");
        return b;
    }

    private void backup(String padID, String content) {
        if (content == null || content.length() == 0 || content.startsWith("Welcome to Etherpad")) return; // we do not back-up empty content
        File b = backupFile(padID);
        try {Files.write(b.toPath(), content.getBytes(StandardCharsets.UTF_8));} catch (IOException e) {}
    }

    private String restore(String padID) throws IOException {
        File b = backupFile(padID);
        if (!b.exists()) throw new IOException("pad " + padID + " has no backup");
        String text = new String(Files.readAllBytes(b.toPath()), StandardCharsets.UTF_8);
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
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadPost(writeurl, p)));
        JSONObject json = new JSONObject(serviceResponse);
        String message = json.optString("message");
        assert "ok".equals(message);
    }


    // Access to the chat system


    private String createAuthorIfNotExistsFor(String name) throws IOException {
        String authorMapper = Integer.toString(Math.abs(name.hashCode()));
        String writeurl = this.etherpadUrlstub + "/api/1.2.7/createAuthorIfNotExistsFor";
        Map<String, byte[]> p = new HashMap<>();
        p.put("apikey", this.etherpadApikey.getBytes(StandardCharsets.UTF_8));
        p.put("authorMapper", authorMapper.getBytes(StandardCharsets.UTF_8));
        p.put("name", name.getBytes(StandardCharsets.UTF_8));
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadPost(writeurl, p)));
        JSONObject json = new JSONObject(serviceResponse);
        String message = json.optString("message");
        assert "ok".equals(message);
        JSONObject data = json.optJSONObject("data");
        assert data != null; // because we created the pad!
        String authorID = data == null ? "" : data.getString("authorID").trim();
        return authorID;
    }

    /**
     * append a chat message
     * @param padID the name of the pad
     * @param text message text
     * @param name name of the user that does the chat message
     * @throws IOException
     */
    public void appendChatMessage(String padID, String text, String name) throws IOException {
        String authorID = createAuthorIfNotExistsFor(name);
        String writeurl = this.etherpadUrlstub + "/api/1.2.12/appendChatMessage";
        Map<String, byte[]> p = new HashMap<>();
        p.put("apikey", this.etherpadApikey.getBytes(StandardCharsets.UTF_8));
        p.put("padID", padID.getBytes(StandardCharsets.UTF_8));
        p.put("text", text.getBytes(StandardCharsets.UTF_8));
        p.put("authorID", authorID.getBytes(StandardCharsets.UTF_8));
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadPost(writeurl, p)));
        JSONObject json = new JSONObject(serviceResponse);
        String message = json.optString("message");
        assert "ok".equals(message);
    }

    /**
     * returns the chatHead (last number of the last chat-message) of the pad
     * @param padID the name of the pad, i.e. "susi"
     * @return -1 if pad does not exist or has no chat entry, 0 or greater for one or more entries.
     * @throws IOException
     */
    public int getChatHead(String padID) throws IOException {
        Map<String, String> request_header = new HashMap<>();
        request_header.put("Accept","application/json");
        String createurl = this.etherpadUrlstub + "/api/1.2.7/getChatHead?apikey=" + this.etherpadApikey + "&padID=" + padID;
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadGet(createurl, request_header)));
        JSONObject json = new JSONObject(serviceResponse);
        // in case that
        // - the pad has no chat entry:     json = {"code":0,"message":"ok","data":{"chatHead":-1}}
        // - the pad exists, has one entry: json = {"code":0,"message":"ok","data":{"chatHead":0}}
        JSONObject data = json.optJSONObject("data");
        int chatHead = (data == null || !data.has("chatHead")) ? -1 : data.getInt("chatHead");
        return chatHead;
    }

    /**
     * Get a history of chat entries
     * @param padID the name of the pad
     * @param start the first entry in the chat
     * @param end the last entry in the chat history
     * @return a list of chat entries
     * @throws IOException
     */
    public List<Message> getChatHistory(String padID, int start, int end) throws IOException {
        List<Message> m = new ArrayList<>();
        if (end < start) return m; // may happen if head = -1 (no messages exist)
        Map<String, String> request_header = new HashMap<>();
        request_header.put("Accept","application/json");
        String createurl = this.etherpadUrlstub + "/api/1.2.7/getChatHistory?apikey=" + this.etherpadApikey + "&padID=" + padID + "&start=" + start + "&end=" + end; // probably bug in etherpad parsing the url: 0 required in front of start, the number is otherwise truncated
        JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(HttpClient.loadGet(createurl, request_header)));
        JSONObject json = new JSONObject(serviceResponse);
        /* i.e. json = 
         * {"code":0,"message":"ok","data":{"messages":[
         *   {"text":"foo","userId":"a.foo","time":1359199533759,"userName":"test"},
         *   {"text":"bar","userId":"a.foo","time":1359199534622,"userName":"test"}
         * ]}}
         */
        JSONObject data = json.optJSONObject("data");
        if (data == null || !data.has("messages")) throw new IOException("no messages in chat history");
        JSONArray messages = data.getJSONArray("messages");
        for (int i = 0; i < messages.length(); i++) m.add(new Message(messages.getJSONObject(i)));
        return m;
    }

    public List<Message> getChatHistory(String padID, int count) throws IOException {
        int head = getChatHead(padID); // the number of the last chat entry; i.e. 0 in case there is only one entry.
        int start = Math.max(0, head - count + 1); // head is inclusive: head - start = count - 1
        return getChatHistory(padID, start, head);
    }

    public static class Message {
        public final String text, userId, userName;
        public long time;
        public Message(JSONObject message) {
            this.text = message.optString("text");
            this.userName = message.optString("userName"); // null is possible if user has no name
            this.userId = message.optString("userId"); // even if the user name is null, the userId is set
            this.time = message.optLong("time");
        }
        public JSONObject toJSON() {
            JSONObject j = new JSONObject(true);
            j.put("text", text);
            j.put("userId", userId);
            j.put("userName", userName);
            j.put("time", time);
            return j;
        }
        public String toString() {
            return this.toJSON().toString(2);
        }
    }

    /**
     * check if pad does not exist
     * @param content
     * @return
     */
    public static boolean padDoesNotExist(String content) {
        return content == null;
    }

    /**
     * check if a pad is empty
     * @param content
     * @return
     */
    public static boolean padDoesNotExistOrIsEmpty(String content) {
        return padDoesNotExist(content) || content.trim().length() == 0;
    }

    /**
     * check if a pad is empty or has a default content (extended view of "is empty")
     * @param content
     * @return
     */
    public static boolean padDoesNotExistOrIsEmptyOrDefault(String content) {
        return padDoesNotExistOrIsEmpty(content) || content.startsWith("Welcome to Etherpad!");
    }

    /**
     * Check if a pad contains a skill
     * @param content
     * @return
     */
    public static boolean padContainsSkill(String content) {
        return !padDoesNotExistOrIsEmptyOrDefault(content) && !content.startsWith("disabled");
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
