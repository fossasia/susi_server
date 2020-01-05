/**
 *  SusiIdentity
 *  Copyright 12.03.2016 by Michael Peter Christen, @0rb1t3r
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
import java.util.ArrayList;
import java.util.List;

import ai.susi.DAO;

/**
 * Identity is the mental model of a being represented with a SusiMind.
 * In other words, it is the storage location for all cognitions.
 * 
 * Identities are not only defined using a storage location but also using an attention.
 * An attention is the dimension of awareness time, it represents the number of cognitions that
 * the being is able to be aware of at the same time.
 * 
 * Awareness using an attention dimension means that certain entities of the memory
 * are lost after a given time. Time is defined here by the number of cognitions that
 * the identity stores.
 */
public class SusiIdentity {

    private File short_term_memory_file, long_term_memory_file;
    private SusiAwareness short_term_memory;
    private SusiSkillFile long_term_memory;
    private int attention;

    /**
     * Create a new identity.
     * The identity is initialized if it did not exist yet
     * or a memory dump is read up to the given attention.
     * If the attention is Integer.MAX_VALUE, the identity becomes GOD which means it has unlimited awareness.
     * Please be aware that being GOD means that computations on unlimited awareness may be CPU and memory intensive :)
     * Actually thats the main reason of having a limited awareness: to be efficient with the computation resources.
     * @param memorypath a path to a storage location of the identity
     * @param attention the dimension of the awareness
     */
    public SusiIdentity(File memorypath, int attention) {
        this.attention = attention;

        // initialize short term memory
        this.short_term_memory = new SusiAwareness();
        memorypath.mkdirs();
        this.short_term_memory_file = new File(memorypath, "log.txt");
        if (this.short_term_memory_file.exists()) {
            try {
                this.short_term_memory = new SusiAwareness(this.short_term_memory_file, attention);
                this.short_term_memory.limitAwareness(this.attention);
            } catch (IOException e) {
                DAO.severe("error reading short memory file " + this.short_term_memory_file.getAbsolutePath() + " - deleting file", e);
                this.short_term_memory_file.delete();
                this.short_term_memory = new SusiAwareness();
            }
        }

        // initialize long term memory
        this.long_term_memory_file = new File(memorypath, "rules.txt");
        if (!this.long_term_memory_file.exists()) try {
            this.long_term_memory_file.createNewFile();
        } catch (IOException e) {
            this.long_term_memory = null;
        }
        try {
            this.long_term_memory = SusiSkillFile.load(this.long_term_memory_file);
        } catch (IOException e) {
            this.long_term_memory = null;
        }
    }

    /**
     * Add a cognition to the identity. This will cause that we forget cognitions after
     * the awareness threshold has passed.
     * @param cognition
     * @return self
     */
    public SusiIdentity add(SusiCognition cognition) throws IOException {
        this.short_term_memory.learn(cognition);
        this.short_term_memory.limitAwareness(this.attention);
        cognition.appendToFile(this.short_term_memory_file);
        return this;
    }

    /**
     * To be able to increase or decrease the current attention the attention level can be set here.
     * Setting the attention to Integer.MAX_VALUE means, to be GOD
     * @param attention the next attention level
     * @return self
     */
    public SusiIdentity setAttention(int attention) {
        this.attention = attention;
        return this;
    }

    /**
     * Get the current attention dimension which is the number of cognitions in the maintained awareness
     * @return the attention dimension
     */
    public int getAttention() {
        return this.attention;
    }

    /**
     * Get the current awareness as list of cognitions.
     * The list is reverse ordered, latest cognitions are first in the list.
     * @return a list of cognitions, latest first
     */
    public List<SusiCognition> getCognitions() {
        ArrayList<SusiCognition> cognitions = new ArrayList<>();
        this.short_term_memory.forEach(cognition -> cognitions.add(cognition));
        return cognitions;
    }

}
