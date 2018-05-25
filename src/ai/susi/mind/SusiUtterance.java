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

package ai.susi.mind;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONObject;

import ai.susi.tools.TimeoutMatcher;

/**
 * Thinking starts with a series of inferences if it is triggered with a matching mechanism which tells the
 * Susi mind that it should identify a given input as something it can think about. Such a trigger is a
 * SusiPhrase, a pattern that is used to 'remember' how to handle inputs. 
 * To descibe a phrase in a more computing-related way: a phrase is a pre-compiled regular expression.
 */
public class SusiUtterance {

    public static enum Type {
        minor(0), regex(1), pattern(1), prior(3);
        private final int subscore;
        private Type(int s) {this.subscore = s;}
        private int getSubscore() {return this.subscore;}
    }

    private final static String CATCHONE_CAPTURE_GROUP_STRING = "(^\\S+)"; // only one word
    private final static String CATCHALL_CAPTURE_GROUP_STRING = "(.*)"; // greedy capturing everything is the best choice: that covers words phrases as well
    private final static Pattern CATCHONE_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHONE_CAPTURE_GROUP_STRING));
    private final static Pattern CATCHALL_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHALL_CAPTURE_GROUP_STRING));
    private final static Pattern dspace = Pattern.compile("  ");
    private final static Pattern wspace = Pattern.compile(",|;:");

    private final Pattern pattern;
    private final Type type;
    private final boolean hasCaptureGroups;
    private final int meatsize;
    
    /**
     * Create a phrase using a json data structure containing the phrase description.
     * The json must contain at least two properties:
     *   type: the name of the phrase type which means: what language is used for the pattern
     *   expression: the pattern string using either regular expressions or simple patterns
     * @param json the phrase description
     * @throws PatternSyntaxException
     */
    public SusiUtterance(JSONObject json) throws PatternSyntaxException {
        if (!json.has("expression")) throw new PatternSyntaxException("expression missing", "", 0);
        String expression = json.getString("expression").toLowerCase();
        
        Type t = Type.pattern;
        if (json.has("type")) try {
            t = Type.valueOf(json.getString("type"));
        } catch (IllegalArgumentException e) {
            Logger.getLogger("SusiPhrase").warning("type value is wrong: " + json.getString("type"));
            t = expression.indexOf(".*") >= 0 ? Type.regex : expression.indexOf('*') >= 0 ? Type.pattern : Type.minor;
        }
        
        expression = normalizeExpression(expression);
        if (expression.length() > 0 && (t == Type.minor || t == Type.prior)) {
            if (expression.indexOf('*') >= 0) t = Type.pattern;
            if (expression.indexOf(".*") >= 0 ||
                (expression.charAt(0) == '^' && expression.charAt(expression.length() - 1) == '$') ||
                (expression.charAt(0) == '(' && expression.charAt(expression.length() - 1) == ')')) t = Type.regex;
        }
        if (t == Type.pattern) expression = parsePattern(expression);
        this.pattern = Pattern.compile(expression);
        this.type = expression.equals("(.*)") ? Type.minor : t;
        this.hasCaptureGroups = expression.replaceAll("\\(\\?", "").indexOf('(') >= 0;
        
        // measure the meat size
        this.meatsize = Math.min(99, extractMeat(expression).length());
    }
    
    public static String normalizeExpression(String s) {
        s = s.trim().toLowerCase().replaceAll("\\#", "  ");
        Matcher m;
        while ((m = wspace.matcher(s)).find()) s = m.replaceAll(" ");
        while ((m = dspace.matcher(s)).find()) s = m.replaceAll(" ");
        if (s.length() == 0) return s; // prevent StringIndexOutOfBoundsException which can happen in the next line
        if (".?!".indexOf(s.charAt(s.length() - 1)) >= 0) s = s.substring(0, s.length() - 1).trim();
        // to be considered: https://en.wikipedia.org/wiki/Wikipedia:List_of_English_contractionst
        int p = -1;
        while ((p = s.toLowerCase().indexOf("it's ")) >= 0) s = s.substring(0, p + 2) + " is " + s.substring(p + 5);
        while ((p = s.toLowerCase().indexOf("what's ")) >= 0) s = s.substring(0, p + 4) + " is " + s.substring(p + 7);
        return s;
    }
    
    public static JSONObject simplePhrase(String query, boolean prior) {
        	// normalize query
        	query = query.trim();
        	
        	// correction of wrong wild card usage
        	if (query.length() > 0 && (
        	    query.charAt(0) != '^' || query.charAt(query.length() - 1) != '$' ||
        	    query.charAt(0) != '(' || query.charAt(query.length() - 1) != ')')) {
        	    int p;
        	    while ((p = query.indexOf('.')) > 0 && query.charAt(p - 1) != ' ') {
        	        query = query.substring(0, p) + ' ' + query.substring(p);
        	    }
        	    while ((p = query.indexOf('.')) >= 0 && p < query.length() - 1 && query.charAt(p + 1) != ' ') {
        	        query = query.substring(0, p + 1) + ' ' + query.substring(p + 1);
        	    }
        	}
        	
        	// create phrase
        JSONObject json = new JSONObject();
        json.put("type", prior ? Type.prior.name() : Type.minor.name());
        json.put("expression", query);
        return json;
    }

    public static String parsePattern(String expression) {
        String[] expressions = expression.split("\\|");
        if (expressions.length == 0) return "";
        if (expressions.length == 1) return parseOnePattern(expressions[0]);
        StringBuilder sb = new StringBuilder();
        for (String e: expressions) sb.append("(?:").append(parseOnePattern(e)).append(")|");
        return sb.substring(0, sb.length() - 1);
    }
    
    private static String parseOnePattern(String expression) {
        expression = parseOnePattern(expression, '*', CATCHALL_CAPTURE_GROUP_STRING);
        expression = parseOnePattern(expression, '+', CATCHONE_CAPTURE_GROUP_STRING);
        return expression;
    }
    

    private static String parseOnePattern(String expression, char meta, String regex) {
        if (expression.length() == 0 || expression.equals("" + meta)) expression = regex;
        if ("?!:.".indexOf(expression.charAt(expression.length() - 1)) >= 0) expression = expression.substring(0, expression.length() - 1);
        if (expression.startsWith(meta + " ")) expression = regex + " " + expression.substring(2);
        if (expression.length() > 0 && expression.charAt(0) == meta) expression = regex + " ?" + expression.substring(1);
        if (expression.endsWith(" " + meta)) expression = expression.substring(0, expression.length() - 2) + " " + regex;
        if (expression.length() > 0 && expression.charAt(expression.length() - 1) == meta) expression = expression.substring(0, expression.length() - 1) + " ?" + regex;
        expression = expression.replaceAll(" \\" + meta + " | \\?\\" + meta + " ", " " + regex + " ");
        return expression;
    }
    
    public static boolean isRegularExpression(String expression) {
        if (expression.indexOf('\\') >=0 || (expression.indexOf('(') >= 0 && expression.indexOf(')') >= 0) ) {
            // this is a hint that this could be a regular expression.
            // the meatsize of regular expressions must be 0, therefore we must check this in more detail
            try {
                Pattern.compile(expression);
                return true;
            } catch (PatternSyntaxException e) {}
            // if this is not successful, go on
        }
        return false;
    }
    
    public static String extractMeat(String expression) {
        if (isRegularExpression(expression)) return ""; // the meatsize of a regular expression is zero
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c) || c == ' ' || c == '_') sb.append(c);
        }
        return sb.toString();
    }
    
    /**
     * get the pre-compiled regular expression pattern
     * @return a java pattern
     */
    public Pattern getPattern() {
        return this.pattern;
    }
    
    /**
     * get the type. this will be used for score computation
     * @return the type
     */
    public Type getType() {
        return this.type;
    }
    
    public int getSubscore() {
        return ((this.type == Type.pattern || this.type == Type.regex) && !this.hasCaptureGroups) ? this.type.getSubscore() + 1 : this.type.getSubscore();
    }
    
    public int getMeatsize() {
        return this.meatsize;
    }
    
    public String toString() {
        return this.toJSON().toString();
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        String p = this.pattern.pattern();
        if (this.type == Type.pattern || this.type == Type.regex) {
            if (new TimeoutMatcher(CATCHALL_CAPTURE_GROUP_PATTERN.matcher(p)).find()) {
                p = p.replaceAll(CATCHALL_CAPTURE_GROUP_PATTERN.pattern(), "*");
            }
            json.put("type", this.type.name());
            json.put("expression", this.pattern.pattern());
        } else {
            json.put("type", this.type.name());
            json.put("expression", p);
        }
        return json;
    }
}