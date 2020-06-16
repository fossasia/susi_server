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

    private final Object pattern; // will contain either String or Pattern objects

    public SusiPattern(String expression, boolean compileToPattern) throws PatternSyntaxException {
        //compileToPattern = true; // for debugging
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

        private Object matcher; // will contain either String or Matcher objects

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
                    ((String) SusiPattern.this.pattern).replaceAll("\\\\", "").equals((String) this.matcher);
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

        public String toString() {
            return this.matcher.toString();
        }

        // implementation of hashCode and equals below to make it possible
        // to store a SusiMatcher into hashtables

        @Override
        public int hashCode() {
            if (this.matcher instanceof Matcher) {
                return ((Matcher) this.matcher).toString().hashCode();
            } else {
                return ((String) this.matcher).hashCode();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SusiMatcher)) return false;
            SusiMatcher m = (SusiMatcher) o;
            if (this.matcher instanceof Matcher) {
                if (!(m.matcher instanceof Matcher)) return false;
                return ((Matcher) this.matcher).toString().equals(((Matcher) m.matcher).toString());
            } else {
                return ((String) this.matcher).equals((String) m.matcher);
            }
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

    // implementation of hashCode and equals below to make it possible
    // to store a SusiPattern into hashtables

    @Override
    public int hashCode() {
        return this.pattern instanceof Pattern ?
                ((Pattern) this.pattern).pattern().hashCode() :
                ((String) this.pattern).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SusiPattern)) return false;
        SusiPattern p = (SusiPattern) o;
        if (this.pattern instanceof Pattern) {
            if (!(p.pattern instanceof Pattern)) return false;
            return ((Pattern) this.pattern).pattern().equals(((Pattern) p.pattern).pattern());
        } else {
            if (!(p.pattern instanceof String)) return false;
            return ((String) this.pattern).equals(((String) p.pattern));
        }
    }

    public static void main(String[] args) {
        SusiPattern p = new SusiPattern("\\[token\\]", true);
        System.out.println("pattern: " + p.toString() + ", hashCode: " + p.hashCode());
        SusiMatcher m = p.matcher("[token]");
        System.out.println("m.hashCode: " + m.hashCode() + ", " + (m.matches() ? "match" : "no match"));

        p = new SusiPattern("\\[token\\]", false);
        System.out.println("pattern: " + p.toString() + ", hashCode: " + p.hashCode());
        m = p.matcher("[token]");
        System.out.println("m.hashCode: " + m.hashCode() + ", " + (m.matches() ? "match" : "no match"));
    }
}
