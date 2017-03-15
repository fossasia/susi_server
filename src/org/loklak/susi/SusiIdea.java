/**
 *  SusiIdea
 *  Copyright 13.07.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.regex.PatternSyntaxException;

/**
 * An idea is the application of a skill on a specific input. This matches with the idea of ideas where
 * an idea is the 'sudden' solution to a problem with the hint how to apply the idea's core concept
 * on the given input details. That is what this class does: it combines a skill with the pattern
 * that matched from the input with the skill.
 */
public class SusiIdea {

    private SusiSkill skill;
    private SusiReader.Token intent;
    
    /**
     * create an idea based on a skill
     * @param skill the skill that matched
     * @throws PatternSyntaxException
     */
    public SusiIdea(SusiSkill skill) throws PatternSyntaxException {
        this.skill = skill;
        this.intent = null;
    }

    public SusiSkill getSkill() {
        return this.skill;
    }
    
    /**
     * Add an intent to the idea. The intent is usually a token (i.e. a normalized single word)
     * that matched with the skill keys.
     * @param intentKey
     * @return the idea
     */
    public SusiIdea setIntent(SusiReader.Token intent) {
        this.intent = intent;
        return this;
    }
    
    /**
     * get the intentions for the idea
     * @return the keyword which matched with the skill keys
     */
    public SusiReader.Token getIntent() {
        return this.intent;
    }
    
    public String toString() {
        return this.skill.toString();
    }
}
