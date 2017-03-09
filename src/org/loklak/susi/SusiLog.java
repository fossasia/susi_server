/**
 *  SusiLog
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
public class SusiLog {

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
    private Map<String, UserRecord> logs;
    private Map<String, Map<String, JsonTray>> rulesets;
    private Set<String> unanswered;
    
    public SusiLog(File storageLocation, int rememberLatestCount) {
        this.root = storageLocation;
        this.rc = rememberLatestCount;
        this.logs = new ConcurrentHashMap<>();
        this.rulesets = new ConcurrentHashMap<>();
        this.unanswered = null;
    }
    
    public Set<String> getUnanswered() {
        if (this.unanswered != null) return this.unanswered;
        this.unanswered = new ConcurrentHashSet<>();
        // debug
        if (this.root != null) for (String c: this.root.list()) {
            getInteractions(c).forEach(i -> {
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
     * get a list of interaction using the client key
     * @param client
     * @return a list of interactions, latest interaction is first in list
     */
    public ArrayList<SusiInteraction> getInteractions(String client) {
        UserRecord ur = this.logs.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            this.logs.put(client, ur);
        }
        return ur.conversation;
    }
    public SusiLog addInteraction(String client, SusiInteraction si) {
        UserRecord ur = this.logs.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            this.logs.put(client, ur);
        }
        ur.add(si);
        return this;
    }
    
    public class UserRecord {
        private ArrayList<SusiInteraction> conversation = null; // first entry always has the latest interaction
        private File logdump;
        public UserRecord(String client) {
            this.conversation = new ArrayList<>();
            File logpath = new File(root, client);
            logpath.mkdirs();
            this.logdump = new File(logpath, "log.txt");
            if (this.logdump.exists()) {
                try {
                    this.conversation = readLog(this.logdump, rc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public UserRecord add(SusiInteraction interaction) {
            if (this.conversation == null) return this;
            this.conversation.add(0, interaction);
            if (this.conversation.size() > rc) this.conversation.remove(this.conversation.size() - 1);
            try {
                Files.write(this.logdump.toPath(), UTF8.getBytes(interaction.getJSON().toString(0) + "\n"), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return this;
        }
    }
    
    public static ArrayList<SusiInteraction> readLog(final File logdump, int count) throws IOException {
        List<String> lines = Files.readAllLines(logdump.toPath());
        ArrayList<SusiInteraction> conversation = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.length() == 0) continue;
            SusiInteraction si = new SusiInteraction(new JSONObject(line));
            conversation.add(si);
            if (conversation.size() >= count) break;
        }
        return conversation;
    }
    
    public TreeMap<Long, List<SusiInteraction>> getAllLogs() {
        TreeMap<Long, List<SusiInteraction>> all = new TreeMap<>();
        String[] clients = this.root.list();
        for (String client: clients) {
            File logpath = new File(this.root, client);
            if (logpath.exists()) {
                File logdump = new File(logpath, "log.txt");
                if (logdump.exists()) try {
                    ArrayList<SusiInteraction> conversation = readLog(logdump, Integer.MAX_VALUE);
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

    public Set<String> getRulesetNames(String client) {
        Map<String, JsonTray> rulesets = this.rulesets.get(client);
        if (rulesets == null) {
            Set<String> rules = new HashSet<String>();
            File rpath = new File(root, client);
            rpath.mkdirs();
            for (String s: rpath.list()) if (s.endsWith(".json")) rules.add(s.substring(0, s.length() - 5));
            return rules;
        }
        return rulesets.keySet();
    }
    
    public JsonTray getRuleset(String client, String name) throws IOException {
        Map<String, JsonTray> rulesets = this.rulesets.get(client);
        if (rulesets == null) {
            rulesets = new HashMap<>();
            this.rulesets.put(client, rulesets);
        }
        JsonTray jt = rulesets.get(name);
        if (jt == null) {
            File rpath = new File(root, client);
            rpath.mkdirs();
            jt = new JsonTray(new File(rpath, name + ".json"), null, 1000);
            rulesets.put(name,  jt);
        }
        return jt;
    }
}
