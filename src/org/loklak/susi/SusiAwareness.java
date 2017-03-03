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


package org.loklak.susi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * Awareness is a sequence of cognitions.
 * The first entry always has the latest cognition
 */
public class SusiAwareness {

    private final ArrayList<SusiCognition> aware;
    
    public SusiAwareness() {
        this.aware = new ArrayList<>();
    }
    
    /**
     * perception of time
     * this is a silly method that defines the perception of time as the length of the cognition sequence
     * @return an abstract number regarding the perception of time
     */
    public int getTime() {
        return this.aware.size();
    }
    
    /**
     * learning a new cognition means that we store it to our memory.
     * New cognitions become first in the list of stored cognitions to be more visible then older cognitions
     * @param cognition
     * @return self
     */
    public SusiAwareness learn(SusiCognition cognition) {
        this.aware.add(0, cognition);
        return this;
    }
    
    /**
     * Forgetting the oldest cognition: this is an important operation to prevent that we
     * memorize too many cognitions all the time. Too many cognitions would mean to have
     * a too strong attention which may be exhausting (for memory and CPU)
     * @return the cognition which was forgot or NULL if no cognition was forgot
     */
    public SusiCognition forgetOldest() {
        if (this.aware.size() > 0) this.aware.remove(this.aware.size() - 1);
        return null;
    }
    
    /**
     * Limit the cognition up to an attention limit
     * @param attention the required attention limit (maximum number of cognitions)
     * @return list of cognitions which are forgotten, latest first
     */
    public List<SusiCognition> limitAwareness(int attention) {
        ArrayList<SusiCognition> removed = new ArrayList<>();
        while (attention != Integer.MAX_VALUE && this.getTime() > attention) {
            SusiCognition c = this.forgetOldest();
            if (c == null) break;
            removed.add(0, c);
        }
        return removed;
    }
    
    /**
     * Being aware of the latest cognition: get the latest
     * @return the latest cognition that this virtual being learned
     */
    public SusiCognition getLatest() {
        return this.aware.size() > 0 ? this.aware.get(0) : null;
    }
    
    /**
     * Get a list of all cognitions.
     * @return a list of cognitions, reverse order (latest first)
     */
    public List<SusiCognition> getCognitions() {
        return this.aware;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        this.aware.forEach(cognition -> sb.append(cognition.toString()).append('\n'));
        sb.append("]\n");
        return sb.toString();
    }
    
    
    /**
     * produce awareness by reading a memory dump up to a given attention time
     * @param memorydump file where the memory is stored
     * @param attentionTime the maximum number of cognitions within the required awareness
     * @return awareness for the give time
     * @throws IOException
     */
    public static SusiAwareness readMemory(final File memorydump, int attentionTime) throws IOException {
        List<String> lines = Files.readAllLines(memorydump.toPath());
        SusiAwareness awareness = new SusiAwareness();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.length() == 0) continue;
            SusiCognition si = new SusiCognition(new JSONObject(line));
            awareness.aware.add(si);
            if (attentionTime != Integer.MAX_VALUE && awareness.getTime() >= attentionTime) break;
        }
        return awareness;
    }
    
}
