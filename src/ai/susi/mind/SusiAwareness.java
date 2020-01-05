/**
 *  SusiAwareness
 *  Copyright 11.03.2017 by Michael Peter Christen, @0rb1t3r
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Awareness is a specific sequence of cognitions.
 * Susi is conscious about a part of that sequence which is caused by an awareness limit.
 * The limit is currently artificial (like: hardcoded) but a heuristic which creates such
 * a limit based on a signal that creates a kind of self-reference would actually create
 * a perception of time which is here defined as the lengh of the resulting cognition sequence.
 * 
 * This class therefore also contains a definition of time which means: the perception of
 * time for Susi minds.
 * 
 * The awareness memory is implemented as Deque list where the first entry
 * always has the latest cognition and the last cognition is the termination element.

 * TODO: the termination element should represent some kind of self-reference cognition.
 * Then awareness would express all cognitions until the time where a Susi mind remembers itself.
 * That could be a simple model of consciousness.
 */
public class SusiAwareness implements Iterable<SusiCognition> {

    /**
     * Awareness memory: this is reverse list of cognitions; the latest cognition is first in the list.
     */
    private final Deque<SusiCognition> awarex;

    public SusiAwareness() {
        this.awarex = new ConcurrentLinkedDeque<>();
    }

    // helper method to patch missing information in accounting object
    public static SusiCognition firstCognition(final File memorydump) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(memorydump), StandardCharsets.UTF_8));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {reader.close(); return null;}
                line = line.trim();
                if (line.length() == 0) continue;
                SusiCognition si = new SusiCognition(new JSONObject(line));
                reader.close();
                return si;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * produce awareness by reading a memory dump up to a given attention time
     * @param memorydump file where the memory is stored
     * @param attentionTime the maximum number of cognitions within the required awareness
     * @return awareness for the give time
     * @throws IOException
     */
    public SusiAwareness(final File memorydump, int attentionTime) throws IOException {
        this();
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName("initializing awareness with " + memorydump.getAbsolutePath());
        List<String> lines = Files.readAllLines(memorydump.toPath()); // this can throw a java.nio.charset.MalformedInputException which is an instance of IOException
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.length() == 0) continue;
            try {
                SusiCognition si = new SusiCognition(new JSONObject(line));
                this.awarex.addLast(si); // thats right, we insert at the end of the deque because we are reading in reverse order
            } catch (JSONException e) {
                throw new IOException(e.getMessage());
            }
            if (attentionTime != Integer.MAX_VALUE && this.getTime() >= attentionTime) break;
        }
        Thread.currentThread().setName(name);
    }
    
    /**
     * To memorize a cognition, we simply append it to a cognition dump file.
     * This may be used i.e. to store every conversation detail for each skill
     * @param skillogfile
     * @param cognition
     */
    public static void memorize(File skillogfile, SusiCognition cognition) {
        try {
            File parent = skillogfile.getParentFile();
            if (!parent.exists()) parent.mkdirs();
            cognition.appendToFile(skillogfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Perception of time:
     * as an experiment we implement the perception of time as the length of the cognition sequence.
     * Please see the class description for details.
     * @return an abstract number describing the perception of time as thinking sequence since the last thought about ourself.
     */
    public int getTime() {
        return this.awarex.size();
    }
    
    /**
     * Learning a new cognition means that we store it to our memory.
     * New cognitions become first in the list of stored cognitions to be more visible then older cognitions
     * @param cognition
     * @return self
     */
    public SusiAwareness learn(SusiCognition cognition) {
        this.awarex.addFirst(cognition);
        return this;
    }
    
    /**
     * Forgetting the oldest cognition: this is an important operation to prevent that we
     * memorize too many cognitions all the time. Too many cognitions would mean to have
     * a too strong attention which may be exhausting (for memory and CPU)
     * @return the cognition which was forgot or NULL if no cognition was forgot
     */
    public SusiCognition forgetOldest() {
        if (this.awarex.isEmpty()) return null;
        return this.awarex.pollLast();
    }
    
    /**
     * Limit the cognition up to an attention limit
     * @param attention the required attention limit (maximum number of cognitions)
     * @return list of cognitions which are forgotten, latest first
     */
    public List<SusiCognition> limitAwareness(int attention) {
        ArrayList<SusiCognition> removed = new ArrayList<>();
        while (this.getTime() > attention) {
            SusiCognition c = this.forgetOldest();
            if (c == null) break; // this should never happen in non-concurrent environments. In concurrent, it might.
            removed.add(0, c);
        }
        return removed;
    }
    
    /**
     * Being aware of the latest cognition: get the latest
     * @return the latest cognition that this virtual being learned
     */
    public SusiCognition getLatest() {
        if (this.awarex.isEmpty()) return null;
        return this.awarex.pollFirst();
    }

    /**
     * The Awareness iterator iterates all cognitions as are available in the current consciousness.
     * @return the available cognitions, latest first.
     */
    @Override
    public Iterator<SusiCognition> iterator() {
        return this.awarex.iterator();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        this.awarex.forEach(cognition -> sb.append(cognition.toString()).append('\n'));
        sb.append("]\n");
        return sb.toString();
    }
    
}
