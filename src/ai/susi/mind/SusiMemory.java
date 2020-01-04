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

package ai.susi.mind;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import ai.susi.json.JsonTray;
import ai.susi.tools.MapTools;

import org.apache.commons.io.FileUtils;

/**
 * Susis log is a kind of reflection about the conversation in the past
 */
public class SusiMemory {

    private final static String[] failterms = new String[]{
            "I don't know how to answer this. Here is a web search result:",
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
    
    private File chatlog, skilllog;
    private int attention; // a measurement for time
    private Map<String, SusiIdentity> memories;
    private Map<String, Map<String, JsonTray>> intentsets;
    private Map<String, AtomicInteger> unanswered;
    
    public SusiMemory(File susi_chatlog_dir, File susi_skilllog_dir, int attention) {
        if (susi_chatlog_dir != null) susi_chatlog_dir.mkdirs();
        if (susi_skilllog_dir != null) susi_skilllog_dir.mkdirs();
        this.chatlog = susi_chatlog_dir;
        this.skilllog = susi_skilllog_dir;
        if (this.skilllog != null) try {FileUtils.cleanDirectory(this.skilllog);} catch (IOException e) {} // do this only as long as we are in a migration phase
        this.attention = attention;
        this.memories = new ConcurrentHashMap<>();
        this.intentsets = new ConcurrentHashMap<>();
        this.unanswered = new ConcurrentHashMap<>();
    }
    
    public void initializeMemory() {
        // initialize the unanswered list.
        if (this.chatlog != null) for (String identity: this.chatlog.list()) {
            // we iterate over all users and check all their conversations
            getCognitions(identity, false).forEach(cognition -> {
                // copy the cognition into the log for the skill
                List<SusiThought> thoughts = cognition.getAnswers();
                if (!thoughts.isEmpty()) {
                    SusiThought thought = thoughts.get(0);
                    String logpath = thought.getLogPath();
                    if (logpath != null) {
                        File skillogfile = new File(this.skilllog, logpath);
                        cognition.setIdentity(identity);
                        SusiAwareness.memorize(skillogfile, cognition);
                    }

                    // check unanswered
                    String query = cognition.getQuery().toLowerCase();
                    String answer = cognition.getExpression(true);
                    if (query.length() > 0 && failset.contains(answer)) {
                        AtomicInteger counter = this.unanswered.get(query);
                        if (counter == null) {
                            counter = new AtomicInteger(0);
                            this.unanswered.put(query,  counter);
                        }
                        counter.incrementAndGet();
                    }
                }
                //System.out.println("** DEBUG user " + c + "; q = " + query + "; a = " + answer);
            });
        }
    }

    public File getSkillLogPath(String skillName) {
        return new File(this.skilllog, SusiThought.getLogPath(skillName));
    }
    
    public Map<String, Integer> getUnanswered() {
        return MapTools.deatomize(this.unanswered);
    }
    
    /**
     * transform unanswered into a statistic for the number of occurrences of words.
     * The words are computed by tokenization of all unanswered phrases.
     * The result is a list of tokens, attached with a list of sentences which contains the token.
     * The list is ordered in reverse order of the number of sentences where the token appears.
     * @param unanswered
     * @return
     */
    public List<TokenMapList> unanswered2tokenizedstats() {
        // sort map by counter
        Map<String, AtomicInteger> tokenCounter = new TreeMap<>(); // a map from tokens to counts
        this.unanswered.forEach((term, counter) -> {
            SusiLinguistics.tokenizeSentence(null, term).forEach(token -> {
                if (token.original.length() > 1) MapTools.incCounter(tokenCounter, token.original, counter.get());
            });
        });
        
        // we now know how many times terms appear in the unanswered set.
        // lets cluster them and create a list of phrases for each word
        final Map<TokenMapList, Integer> aggregator = new HashMap<>(); // a map from counts to a subset of the unanswered list where the token appears in the query
        tokenCounter.forEach((token, tokencount) -> {
            final AtomicInteger c = new AtomicInteger(0);
            // find all phrases where the token appears
            final Map<String, AtomicInteger> unansweredPartList = new HashMap<>();
            //final String tl = token + " ";
            //final String tr = " " + token;
            //final String tm = " " + token + " ";
            this.unanswered.forEach((query, querycount) -> {
                //String q = query.toLowerCase();
                if (query.toLowerCase().indexOf(token) >= 0) {
                //if (q.indexOf(tm) >= 0 || q.startsWith(tl) || q.endsWith(tr) || q.indexOf(tr) == q.length() - tr.length() - 2) {
                    MapTools.incCounter(unansweredPartList, query, querycount.get());
                    c.getAndAdd(querycount.get());
                }
            });
            // add the unansweredPartList into a map for each number of tokencounts
            TokenMapList tokenmaplist = new TokenMapList(token, MapTools.deatomize(unansweredPartList), c.get());
            aggregator.put(tokenmaplist, c.get());
        });
        
        // finally read out the sorter and write a result in reverse order
        // flatten the accumulator
        LinkedList<TokenMapList> resultlist = new LinkedList<>();
        LinkedHashMap<TokenMapList, Integer> sorter = MapTools.sortByValue(aggregator);
        sorter.forEach((tokenmaplist, counter) -> {
            resultlist.add(tokenmaplist);
        });

        return resultlist;
    }
    
    
    /**
     * TokenMapList is an object which combines a token with a list of all
     * extractions of the unanswered map.
     * It is a list of sentences where the token appears in all the lists.
     */
    public static class TokenMapList {
        // the token which appears in all the sentences
        private final String token;
        
        private int counter;
        
        // a list of parts of the unanswered sentences. 
        private final Map<String, Integer> map;
        public TokenMapList(String token, Map<String, Integer> map, int counter) {
            this.token = token;
            this.map = map;
            this.counter = counter;
        }
        public void setCounter(int c) {
            this.counter = c;
        }
        public int getCounter() {
            return this.counter;
        }
        public String getToken() {
            return this.token;
        }
        public LinkedHashMap<String, Integer> getMap() {
            return MapTools.sortByValue(this.map);
        }
        public int hashCode() {
            return token.hashCode();
        }
        public boolean equals(Object o) {
            return o instanceof TokenMapList && ((TokenMapList) o).token.equals(this.token);
        }
    }

    public boolean removeUnanswered(String s) {
        AtomicInteger removed = this.unanswered.remove(s.toLowerCase());
        return removed != null;
        //if (removed) System.out.println("** removed unanswered " + s);
    }

    public void removeUnanswered(Pattern p) {
        if (p.toString().indexOf("reddit") >= 0) {
            System.out.println("");
        }
        if (dnruset.contains(p.pattern())) return;
        boolean removed = this.unanswered.remove(p.pattern()) != null;
        if (!removed) {
            Iterator<String> i = this.unanswered.keySet().iterator();
            while (i.hasNext()) {
                String s = i.next();
                if (p.matcher(s).matches()) {
                    System.out.println("** removed unanswered " + s);
                    i.remove();
                    removed = true;
                }
            }
        }
        if (removed) System.out.println("** removed unanswered pattern " + p.pattern());
    }

    public SusiIdentity getMemory(String client, boolean storeToCache) {
    	SusiIdentity identity = this.memories.get(client);
        if (identity == null) {
            identity = new SusiIdentity(new File(chatlog, client), attention);
            if (storeToCache) this.memories.put(client, identity);
        }
        return identity;
    }

    public SusiAwareness getAwareness(String client) {
        File short_term_memory_file = new File(new File(chatlog, client), "log.txt");
        if (short_term_memory_file.exists()) {
            try {
                SusiAwareness awareness = new SusiAwareness(short_term_memory_file, Integer.MAX_VALUE);
                return awareness;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public SusiCognition firstCognition(String client) {
        File short_term_memory_file = new File(new File(chatlog, client), "log.txt");
        if (short_term_memory_file.exists()) {
            SusiCognition firstCognition = SusiAwareness.firstCognition(short_term_memory_file);
            return firstCognition;
        }
        return null;
    }

    /**
     * get a list of cognitions using the client key
     * @param client
     * @return a list of interactions, latest cognition is first in list
     */
    public List<SusiCognition> getCognitions(String client, boolean storeToCache) {
        SusiIdentity identity = getMemory(client, storeToCache);
        return identity.getCognitions();
    }
    
    public SusiMemory addCognition(String client, SusiCognition cognition, boolean storeToCache) throws IOException {
        // add to user memories
        SusiIdentity identity = getMemory(client, storeToCache);
        identity.add(cognition);
        
        // add to skill memories
        List<SusiThought> thoughts = cognition.getAnswers();
        if (thoughts.size() > 0) {
	        SusiThought thought = cognition.getAnswers().get(0);
	        String logpath = thought.getLogPath();
	        if (logpath != null) {
	            File skillogfile = new File(this.skilllog, logpath);
	            SusiAwareness.memorize(skillogfile, cognition);
	        }
        }
        return this;
    }
    
    /**
     * collect the complete awareness of all users in all the time
     * @return the list of full awareness, ordered by the time of the latest update of the memories (latest first)
     */
    public TreeMap<Long, SusiAwareness> getAllMemories() {
        TreeMap<Long, SusiAwareness> all = new TreeMap<>();
        if (chatlog == null) return all;
        String[] clients = this.chatlog.list();
        for (String client: clients) {
            File memorypath = new File(this.chatlog, client);
            if (memorypath.exists()) {
                File memorydump = new File(memorypath, "log.txt");
                if (memorydump.exists()) try {
                    SusiAwareness awareness = new SusiAwareness(memorydump, Integer.MAX_VALUE);
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

    public Set<String> getIntentsetNames(String client) {
        Map<String, JsonTray> intentsets = this.intentsets.get(client);
        if (intentsets == null && chatlog != null) {
            Set<String> intents = new HashSet<String>();
            File rpath = new File(chatlog, client);
            rpath.mkdirs();
            for (String s: rpath.list()) if (s.endsWith(".json")) intents.add(s.substring(0, s.length() - 5));
            return intents;
        }
        return intentsets.keySet();
    }
    
    public JsonTray getIntentset(String client, String name) throws IOException {
        Map<String, JsonTray> intentsets = this.intentsets.get(client);
        if (intentsets == null) {
            intentsets = new HashMap<>();
            this.intentsets.put(client, intentsets);
        }
        JsonTray jt = intentsets.get(name);
        if (jt == null && chatlog != null) {
            File rpath = new File(chatlog, client);
            rpath.mkdirs();
            jt = new JsonTray(new File(rpath, name + ".json"), null, 1000);
            intentsets.put(name,  jt);
        }
        return jt;
    }
}
