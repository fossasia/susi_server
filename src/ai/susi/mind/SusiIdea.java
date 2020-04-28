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

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.PatternSyntaxException;

import ai.susi.mind.SusiPattern.SusiMatcher;

/**
 * An idea is the application of a intent on a specific input. This matches with the idea of ideas where
 * an idea is the 'sudden' solution to a problem with the hint how to apply the idea's core concept
 * on the given input details. That is what this class does: it combines a intent with the pattern
 * that matched from the input with the intent.
 */
public class SusiIdea {

    private SusiIntent intent;
    private SusiLinguistics.Token token;
    private Collection<SusiMatcher> matchers;

    /**
     * create an idea based on a intent
     * @param intent the intent that matched
     * @throws PatternSyntaxException
     */
    public SusiIdea(SusiIntent intent) throws PatternSyntaxException {
        this.intent = intent;
        this.token = null;
        this.matchers = null;
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
    public SusiIdea setToken(SusiLinguistics.Token token) {
        this.token = token;
        return this;
    }

    public boolean hasToken() {
        return this.token != null;
    }

    /**
     * get the tokens for the idea
     * @return the keyword which matched with the intent keys
     */
    public SusiLinguistics.Token getToken() {
        return this.token;
    }

    public SusiIdea setMatchers(Collection<SusiMatcher> matchers) {
        this.matchers = matchers;
        return this;
    }

    public SusiIdea addMatcher(SusiMatcher matcher) {
        if (this.matchers == null) this.matchers = new ArrayList<>();
        this.matchers.add(matcher);
        return this;
    }

    public boolean hasMatcher() {
        return this.matchers != null && this.matchers.size() > 0;
    }

    public Collection<SusiMatcher> getMatchers() {
        return this.matchers;
    }

    @Override
    public int hashCode() {
        // we compare ideas only by the intent
        return this.intent.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        // we compare ideas only by the intent
        if (!(o instanceof SusiIdea)) return false;
        SusiIdea si = (SusiIdea) o;
        return this.intent.equals(si.intent);
    }

    public String toString() {
        return this.intent.toString();
    }
}
