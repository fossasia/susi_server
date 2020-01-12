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

public class SusiPattern {

    private final static Pattern SPACE_PATTERN = Pattern.compile(" ");

    private final Pattern pattern;

    public SusiPattern(String expression) throws PatternSyntaxException {
        try {
            this.pattern = Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            // we throw the same exception here to make it possible to debug the event here with a breakpoint
            throw new PatternSyntaxException(e.getDescription(), e.getPattern(), e.getIndex());
        }
    }

    public String pattern() {
        return this.pattern.pattern();
    }

    public String[] token() {
        return SPACE_PATTERN.split(this.pattern.pattern());
    }

    public class SusiMatcher {

        private Matcher matcher;

        protected SusiMatcher(String s) {
            this.matcher = SusiPattern.this.pattern.matcher(s);
        }

        public boolean matches() {
            return new TimeoutMatcher(this.matcher).matches();
        }

        public String group(int g) {
            return this.matcher.group(g);
        }

        public int groupCount() {
            return this.matcher.groupCount();
        }
    }

    public SusiMatcher matcher(String s) {
        return new SusiMatcher(s);
    }

    public String toString() {
        return this.pattern();
    }
}
