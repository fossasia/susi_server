/**
 *  SusiPhrase
 *  Copyright 29.06.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONObject;

/**
 * Thinking starts with a series of inferences if it is triggered with a matching mechanism which tells the
 * Susi mind that it should identify a given input as something it can think about. Such a trigger is a
 * SusiPhrase, a pattern that is used to 'remember' how to handle inputs. 
 * To descibe a phrase in a more computing-related way: a phrase is a pre-compiled regular expression.
 */
public class SusiPhrase {

    public static enum Type {regex, pattern;}

    private final static String CATCHALL_CAPTURE_GROUP_STRING = "(.*)"; // greedy capturing everything is the best choice: that covers words phrases as well
    private final static Pattern CATCHALL_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHALL_CAPTURE_GROUP_STRING));
    private final static Pattern dspace = Pattern.compile("  ");

    private final Pattern pattern;
    
    /**
     * Create a phrase using a json data structure containing the phrase description.
     * The json must contain at least two properties:
     *   type: the name of the phrase type which means: what language is used for the pattern
     *   expression: the pattern string using either regular expressions or simple patterns
     * @param json the phrase description
     * @throws PatternSyntaxException
     */
    public SusiPhrase(JSONObject json) throws PatternSyntaxException {
        if (!json.has("type")) throw new PatternSyntaxException("type missing", "", 0);
        Type type = Type.pattern;
        try {
            type = Type.valueOf(json.getString("type"));
        } catch (IllegalArgumentException e) {throw new PatternSyntaxException("type value is wrong", json.getString("type"), 0);}
        if (!json.has("expression")) throw new PatternSyntaxException("expression missing", "", 0);
        String expression = json.getString("expression");
        expression = expression.toLowerCase().replaceAll("\\#", "  ");
        Matcher m;
        while ((m = dspace.matcher(expression)).find()) expression = m.replaceAll(" ");
        if (type == Type.pattern) expression = expression.replaceAll("\\*", CATCHALL_CAPTURE_GROUP_STRING);
        this.pattern = Pattern.compile(expression);
    }
    
    /**
     * get the pre-compiled regular expression pattern
     * @return a java pattern
     */
    public Pattern getPattern() {
        return this.pattern;
    }
    
    public String toString() {
        return this.toJSON().toString();
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        String p = this.pattern.pattern();
        if (CATCHALL_CAPTURE_GROUP_PATTERN.matcher(p).find()) {
            p = p.replaceAll(CATCHALL_CAPTURE_GROUP_PATTERN.pattern(), "*");
        }
        if (p.indexOf('.') >= 0 || p.indexOf('\\') > 0) {
            json.put("type", Type.regex.name());
            json.put("expression", this.pattern.pattern());
        } else {
            json.put("type", Type.pattern.name());
            json.put("expression", p);
        }
        return json;
    }
}