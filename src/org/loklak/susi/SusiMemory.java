/**
 *  SusiMemory
 *  Copyright 24.07.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.susi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.tools.UTF8;
import org.loklak.tools.storage.JsonTray;

/**
 * Susis log is a kind of reflection about the conversation in the past
 */
public class SusiMemory {

    private final static String[] failterms = new String[]{
            "You can ask me anything, but not that :)",
            "Oh sorry, I don't understand what you say. Please ask something else!",
            "Das weiss ich leider nicht.",
            "I don't know."
    };
    private final static String[] donotremoveunanswered = new String[] {
            "was ?(.*)",
            ".+ (?:.+ )+(.+)",
            "(.*)",
            "(.*) ?sorry ?(.*)",
            "(.*) ?you ?(.*)",
            "what ?(.*)",
            "you ?(.*)"
    };
    private final static Set<String> failset = new HashSet<>();
    private final static Set<String> dnruset = new HashSet<>();
    static {
        for (String t: failterms) failset.add(t);
        for (String t: donotremoveunanswered) dnruset.add(t);
    }
    
    private File root;
    private int rc;
    private Map<String, UserRecord> memories;
    private Map<String, Map<String, JsonTray>> skillsets;
    private Set<String> unanswered;
    
    public SusiMemory(File storageLocation, int rememberLatestCount) {
        this.root = storageLocation;
        this.rc = rememberLatestCount;
        this.memories = new ConcurrentHashMap<>();
        this.skillsets = new ConcurrentHashMap<>();
        this.unanswered = null;
    }
    
    public Set<String> getUnanswered() {
        if (this.unanswered != null) return this.unanswered;
        this.unanswered = new ConcurrentHashSet<>();
        // debug
        if (this.root != null) for (String c: this.root.list()) {
            getCognitions(c).forEach(i -> {
                String query = i.getQuery().toLowerCase();
                String answer = i.getExpression();
                if (query.length() > 0 && failset.contains(answer)) this.unanswered.add(query);
                //System.out.println("** DEBUG user " + c + "; q = " + query + "; a = " + answer);
            });
        }
        return this.unanswered;
    }

    public void removeUnanswered(String s) {
        if (this.unanswered == null) getUnanswered();
        boolean removed = this.unanswered.remove(s.toLowerCase());
        //if (removed) System.out.println("** removed unanswered " + s);
    }
    
    public void removeUnanswered(Pattern p) {
        if (this.unanswered == null) getUnanswered();
        if (dnruset.contains(p.pattern())) return;
        boolean removed = this.unanswered.remove(p.pattern());
        if (!removed) {
            Iterator<String> i = this.unanswered.iterator();
            while (i.hasNext()) {
                String s = i.next();
                if (p.matcher(s).matches()) {
                    i.remove();
                    removed = true;
                }
            }
        }
        if (removed) System.out.println("** removed unanswered " + p.pattern());
    }
    
    /**
     * get a list of cognitions using the client key
     * @param client
     * @return a list of interactions, latest cognition is first in list
     */
    public ArrayList<SusiCognition> getCognitions(String client) {
        UserRecord ur = this.memories.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            this.memories.put(client, ur);
        }
        return ur.cognition;
    }
    public SusiMemory addCognition(String client, SusiCognition si) {
        UserRecord ur = this.memories.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            this.memories.put(client, ur);
        }
        ur.add(si);
        return this;
    }
    
    public class UserRecord {
        private ArrayList<SusiCognition> cognition = null; // first entry always has the latest cognition
        private File memorydump;
        public UserRecord(String client) {
            this.cognition = new ArrayList<>();
            File memorypath = new File(root, client);
            memorypath.mkdirs();
            this.memorydump = new File(memorypath, "log.txt");
            if (this.memorydump.exists()) {
                try {
                    this.cognition = readMemory(this.memorydump, rc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public UserRecord add(SusiCognition cognition) {
            if (this.cognition == null) return this;
            this.cognition.add(0, cognition);
            if (this.cognition.size() > rc) this.cognition.remove(this.cognition.size() - 1);
            try {
                Files.write(this.memorydump.toPath(), UTF8.getBytes(cognition.getJSON().toString(0) + "\n"), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return this;
        }
    }
    
    public static ArrayList<SusiCognition> readMemory(final File memorydump, int count) throws IOException {
        List<String> lines = Files.readAllLines(memorydump.toPath());
        ArrayList<SusiCognition> conversation = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.length() == 0) continue;
            SusiCognition si = new SusiCognition(new JSONObject(line));
            conversation.add(si);
            if (conversation.size() >= count) break;
        }
        return conversation;
    }
    
    public TreeMap<Long, List<SusiCognition>> getAllMemories() {
        TreeMap<Long, List<SusiCognition>> all = new TreeMap<>();
        String[] clients = this.root.list();
        for (String client: clients) {
            File memorypath = new File(this.root, client);
            if (memorypath.exists()) {
                File memorydump = new File(memorypath, "log.txt");
                if (memorydump.exists()) try {
                    ArrayList<SusiCognition> conversation = readMemory(memorydump, Integer.MAX_VALUE);
                    if (conversation.size() > 0) {
                        Date d = conversation.get(0).getQueryDate();
                        all.put(-d.getTime(), conversation);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return all;
    }

    public Set<String> getSkillsetNames(String client) {
        Map<String, JsonTray> skillsets = this.skillsets.get(client);
        if (skillsets == null) {
            Set<String> skills = new HashSet<String>();
            File rpath = new File(root, client);
            rpath.mkdirs();
            for (String s: rpath.list()) if (s.endsWith(".json")) skills.add(s.substring(0, s.length() - 5));
            return skills;
        }
        return skillsets.keySet();
    }
    
    public JsonTray getSkillset(String client, String name) throws IOException {
        Map<String, JsonTray> skillsets = this.skillsets.get(client);
        if (skillsets == null) {
            skillsets = new HashMap<>();
            this.skillsets.put(client, skillsets);
        }
        JsonTray jt = skillsets.get(name);
        if (jt == null) {
            File rpath = new File(root, client);
            rpath.mkdirs();
            jt = new JsonTray(new File(rpath, name + ".json"), null, 1000);
            skillsets.put(name,  jt);
        }
        return jt;
    }
}
