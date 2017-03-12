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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ConcurrentHashSet;
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
    private int attention; // a measurement for time
    private Map<String, SusiIdentity> memories;
    private Map<String, Map<String, JsonTray>> skillsets;
    private Set<String> unanswered;
    
    public SusiMemory(File storageLocation, int attention) {
        this.root = storageLocation;
        this.attention = attention;
        this.memories = new ConcurrentHashMap<>();
        this.skillsets = new ConcurrentHashMap<>();
        this.unanswered = null;
    }
    
    public Set<String> getUnanswered() {
        if (this.unanswered != null) return this.unanswered;
        this.unanswered = new ConcurrentHashSet<>();
        // debug
        if (this.root != null) for (String c: this.root.list()) {
            getAwareness(c).getCognitions().forEach(cognition -> {
                String query = cognition.getQuery().toLowerCase();
                String answer = cognition.getExpression();
                if (query.length() > 0 && failset.contains(answer)) this.unanswered.add(query);
                //System.out.println("** DEBUG user " + c + "; q = " + query + "; a = " + answer);
            });
        }
        return this.unanswered;
    }

    public boolean removeUnanswered(String s) {
        if (this.unanswered == null) getUnanswered();
        boolean removed = this.unanswered.remove(s.toLowerCase());
        return removed;
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
    public SusiAwareness getAwareness(String client) {
        SusiIdentity identity = this.memories.get(client);
        if (identity == null) {
            if (root == null) return null;
            identity = new SusiIdentity(new File(root, client), attention);
            this.memories.put(client, identity);
        }
        return identity.getAwareness();
    }
    public SusiMemory addCognition(String client, SusiCognition si) {
        SusiIdentity identity = this.memories.get(client);
        if (identity == null) {
            if (root == null) return null;
            identity = new SusiIdentity(new File(root, client), attention);
            this.memories.put(client, identity);
        }
        identity.add(si);
        return this;
    }
    
    /**
     * collect the complete awareness of all users in all the time
     * @return the list of full awareness, ordered by the time of the latest update of the memories (latest first)
     */
    public TreeMap<Long, SusiAwareness> getAllMemories() {
        TreeMap<Long, SusiAwareness> all = new TreeMap<>();
        if (root == null) return all;
        String[] clients = this.root.list();
        for (String client: clients) {
            File memorypath = new File(this.root, client);
            if (memorypath.exists()) {
                File memorydump = new File(memorypath, "log.txt");
                if (memorydump.exists()) try {
                    SusiAwareness awareness = SusiAwareness.readMemory(memorydump, Integer.MAX_VALUE);
                    if (awareness.getTime() > 0) {
                        Date d = awareness.getLatest().getQueryDate();
                        all.put(-d.getTime(), awareness);
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
        if (skillsets == null && root != null) {
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
        if (jt == null && root != null) {
            File rpath = new File(root, client);
            rpath.mkdirs();
            jt = new JsonTray(new File(rpath, name + ".json"), null, 1000);
            skillsets.put(name,  jt);
        }
        return jt;
    }
}
