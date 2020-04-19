/**
 *  SusiPattern
 *  Copyright 12.01.2020 by Michael Peter Christen, @0rb1t3r
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ai.susi.tools.TimeoutMatcher;

/**
 * Just-in-time compiler for Pattern objects.
 * 
 * A SusiPattern object carries either a string with a Java Pattern or the Java Pattern itself.
 * This class is here to enable a greedy compilation from the pattern string to the java pattern object.
 * New SusiPattern are initialized with the pattern string. The first time that pattern is used,
 * it is compiled and stored as the compiled pattern object.
 */
public class SusiPattern {

    private final static Pattern SPACE_PATTERN = Pattern.compile(" ");
    private final static Pattern REVERSE_WILDCARD = Pattern.compile(Pattern.quote("(.*)"));

    private final Object pattern;

    public SusiPattern(String expression, boolean compileToPattern) throws PatternSyntaxException {
        if (compileToPattern) try {
            this.pattern = Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            // we throw the same exception here to make it possible to debug the event here with a breakpoint
            throw new PatternSyntaxException(e.getDescription(), e.getPattern(), e.getIndex());
        } else {
            this.pattern = expression;
        }
    }

    public String pattern() {
        return this.pattern instanceof Pattern ? ((Pattern) this.pattern).pattern() : (String) this.pattern;
    }

    public String[] token() {
        return SPACE_PATTERN.split(pattern());
    }

    public class SusiMatcher {

        private Object matcher;

        protected SusiMatcher(String s) {
            if (SusiPattern.this.pattern instanceof Pattern) {
                this.matcher = ((Pattern) SusiPattern.this.pattern).matcher(s);
            } else {
                this.matcher = s;
            }
        }

        public boolean matches() {
            return this.matcher instanceof Matcher ?
                    new TimeoutMatcher((Matcher) this.matcher).matches() :
                    ((String) SusiPattern.this.pattern).equals((String) this.matcher);
        }

        public String group(int g) {
            return this.matcher instanceof Matcher ?
                    ((Matcher) this.matcher).group(g) :
                    null;
        }

        public int groupCount() {
            return this.matcher instanceof Matcher ?
                    ((Matcher) this.matcher).groupCount() :
                    0;
        }
    }

    public SusiMatcher matcher(String s) {
        return new SusiMatcher(s);
    }

    public String toLoT() {
        String s = this.pattern();
        s = REVERSE_WILDCARD.matcher(s).replaceAll("*");
        return s;
    }

    public String toString() {
        return this.pattern();
    }
}
