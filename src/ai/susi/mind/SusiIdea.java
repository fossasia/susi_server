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

package ai.susi.mind;

import java.util.regex.PatternSyntaxException;

/**
 * An idea is the application of a intent on a specific input. This matches with the idea of ideas where
 * an idea is the 'sudden' solution to a problem with the hint how to apply the idea's core concept
 * on the given input details. That is what this class does: it combines a intent with the pattern
 * that matched from the input with the intent.
 */
public class SusiIdea {

    private SusiIntent intent;
    private SusiReader.Token token;
    
    /**
     * create an idea based on a intent
     * @param intent the intent that matched
     * @throws PatternSyntaxException
     */
    public SusiIdea(SusiIntent intent) throws PatternSyntaxException {
        this.intent = intent;
        this.token = null;
    }

    public SusiIntent getIntent() {
        return this.intent;
    }
    
    /**
     * Add an token to the idea. The token is usually a work (i.e. a normalized single word)
     * that matched with the intent keys.
     * @param token Key
     * @return the idea
     */
    public SusiIdea setToken(SusiReader.Token token) {
        this.token = token;
        return this;
    }
    
    /**
     * get the tokens for the idea
     * @return the keyword which matched with the intent keys
     */
    public SusiReader.Token getToken() {
        return this.token;
    }
    
    public String toString() {
        return this.intent.toString();
    }
}
