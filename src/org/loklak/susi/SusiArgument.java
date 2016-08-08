/**
 *  SusiThoughts
 *  Copyright 30.06.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Argument is a series of thoughts, also known as a 'proof' in automated reasoning.
 * Within the Susi AI infrastructure this may be considered as the representation of
 * the short-time memory of thinking inside Susi.
 */
public class SusiArgument implements Iterable<SusiThought> {

    private final ArrayList<SusiThought> recall;
    
    /**
     * Create an empty argument
     */
    public SusiArgument() {
        this.recall = new ArrayList<>();
    }
    
    /**
     * Get an impression of time which elapsed since the start of reasoning in this argument
     * @return the number of thoughts
     */
    public int times() {
        return this.recall.size();
    }
    
    /**
     * The mindstate is the current state of an argument
     * @return the latest thought in a series of proof steps
     */
    public SusiThought mindstate() {
        return remember(0);
    }
    
    /**
     * Remembering the thoughts is essential to recall which thoughts leads to the current mindstate
     * @param timesBack the number of thoughts backwards from the current mindstate
     * @return the thought in the past according to the elapsed time of the thoughts
     */
    public SusiThought remember(int timesBack) {
        int state = this.recall.size() - timesBack - 1;
        if (state < 0) return new SusiThought(); // empty mind!
        return this.recall.get(state);
    }
 
    /**
     * Thinking is a series of thoughts, every new thought appends another thought to the argument.
     * A special situation may (or may not) occur if one thinking step does not produce a result.
     * Depending on the inference rule set that may mean that the consideration of the rule containing
     * the inferences was wrong and should be abandoned. This happens if mindstate().equals(thought).
     * @param thought the next thought
     * @return self, the current mindstate
     */
    public SusiArgument think(SusiThought thought) {
        this.recall.add(thought);
        return this;
    }
    
    /**
     * Unification applies a piece of memory within the current argument to a statement
     * which creates an instantiated statement
     * @param statement
     * @return the instantiated statement with elements of the argument applied
     */
    public String unify(String statement) {
        for (SusiThought t: this) {
            String s = t.unify(statement);
            if (s == null) break; // no more patterns to be instantiated left, not an error!
            statement = s;
        }
        if (SusiThought.variable_pattern.matcher(statement).find()) return null; // failure!
        return statement;
    }

    /**
     * the iterator returns the thoughts in reverse order, latest thought first
     */
    @Override
    public Iterator<SusiThought> iterator() {
        return new Iterator<SusiThought>() {
            private int p = recall.size(); 
            @Override public boolean hasNext() {return p > 0;}
            @Override public SusiThought next() {return remember(--p);}
        };
    }
}
