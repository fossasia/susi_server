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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.json.JSONObject;

import ai.susi.tools.TimeoutMatcher;

/**
 * Thinking starts with a matching mechanism which tells the
 * Susi mind that it should identify a given input as something it can think about.
 * Such a trigger is a SusiUtterance which is a pre-compiled regular expression.
 */
public class SusiUtterance {

    public static enum Type {
        minor(0), prior(1);
        private final int subscore;
        private Type(int s) {this.subscore = s;}
        private int getSubscore() {return this.subscore;}
    }

    private final static String CATCHONE_CAPTURE_GROUP_STRING = "(^\\S+)"; // only one word
    private final static String CATCHALL_CAPTURE_GROUP_STRING = "(.*)"; // greedy capturing everything is the best choice: that covers words phrases as well
    //private final static Pattern CATCHONE_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHONE_CAPTURE_GROUP_STRING));
    private final static Pattern CATCHALL_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHALL_CAPTURE_GROUP_STRING));
    private final static Pattern dspace = Pattern.compile("  ");
    private final static Pattern wspace = Pattern.compile(",|;:");

    private final Pattern pattern;
    private Type type;
    private final boolean hasCaptureGroups;
    private final int meatsize;
    private int line;

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

        this.type = Type.minor;
        if (json.has("type")) try {
            this.type = Type.valueOf(json.getString("type"));
        } catch (IllegalArgumentException e) {
            //Logger.getLogger("SusiPhrase").warning("type value is wrong: " + json.getString("type"));
        }

        this.line = json.has("line") ? json.getInt("line") : 0;

        String expression = normalizeExpression(json.getString("expression"));
        expression = fixExpression(expression);
        expression = parsePattern(expression);

        // write class variables
        try {
            this.pattern = Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            // we throw the same exception here to make it possible to debug the event here with a breakpoint
            throw new PatternSyntaxException(e.getDescription(), e.getPattern(), e.getIndex());
        }
        if (expression.equals("(.*)")) this.type = Type.minor;
        this.hasCaptureGroups = expression.replaceAll("\\(\\?", "").indexOf('(') >= 0;

        // measure the meat size
        this.meatsize = Math.min(99, extractMeat(expression).length());
    }

    /**
     * create a simple phrase
     * @param expression
     * @param prior if true, this phrase has priority
     * @throws PatternSyntaxException
     */
    public SusiUtterance(String expression, boolean prior, int line) throws PatternSyntaxException {
        this.type = prior ? Type.prior : Type.minor;
        this.line = line;

        // normalize expression
        expression = normalizeExpression(expression);
        expression = fixExpression(expression);
        expression = parsePattern(expression);

        // write class variables
        this.pattern = Pattern.compile(expression);
        if (expression.equals("(.*)")) this.type = Type.minor;
        this.hasCaptureGroups = expression.replaceAll("\\(\\?", "").indexOf('(') >= 0;

        // measure the meat size
        this.meatsize = Math.min(99, extractMeat(expression).length());
    }

    private String fixExpression(String expression) {
        if (expression == null || expression.length() == 0) return "";
        if (expression.indexOf(".*") >= 0 ||
            (expression.charAt(0) == '^' && expression.charAt(expression.length() - 1) == '$') ||
            (expression.charAt(0) == '(' && expression.charAt(expression.length() - 1) == ')')) {
        } else {
            // this is not a regular expression, therefore we can remove superfluous dots
            int p;
            while ((p = expression.indexOf('.')) > 0 && expression.charAt(p - 1) != ' ') {
                expression = expression.substring(0, p) + ' ' + expression.substring(p);
            }
            while ((p = expression.indexOf('.')) >= 0 && p < expression.length() - 1 && expression.charAt(p + 1) != ' ') {
                expression = expression.substring(0, p + 1) + ' ' + expression.substring(p + 1);
            }
        }
        return expression;
    }

    /**
     * @deprecated use class constructor instead
     */
    public static JSONObject simplePhrase(String query, boolean prior) {
        // normalize query
        query = query.trim();

        // create phrase
        JSONObject json = new JSONObject();
        json.put("type", prior ? Type.prior.name() : Type.minor.name());
        json.put("expression", query);
        return json;
    }

    public int getLine() {
        return this.line;
    }

    public static String normalizeExpression(String s) {
        s = s.trim().toLowerCase().replaceAll("\\#", "  ");
        Matcher m;
        while ((m = wspace.matcher(s)).find()) s = m.replaceAll(" ");
        while ((m = dspace.matcher(s)).find()) s = m.replaceAll(" ");
        if (s.length() == 0) return s; // prevent StringIndexOutOfBoundsException which can happen in the next line
        int p = ".?!".indexOf(s.charAt(s.length() - 1));
        if (p >= 0 && (s.length() == 1 || s.charAt(s.length() - 2) != '\\')) s = s.substring(0, s.length() - 1).trim();
        // to be considered: https://en.wikipedia.org/wiki/Wikipedia:List_of_English_contractionst
        p = -1;
        while ((p = s.indexOf("it's ")) >= 0) s = s.substring(0, p + 2) + " is " + s.substring(p + 5);
        while ((p = s.indexOf("what's ")) >= 0) s = s.substring(0, p + 4) + " is " + s.substring(p + 7);
        return s;
    }

    public boolean isCatchallPhrase() {
        String expression = this.pattern.pattern();
        return CATCHALL_CAPTURE_GROUP_STRING.equals(expression);
    }

    public static boolean isCatchallPhrase(JSONObject json) {
        String expression = json.getString("expression");
        return CATCHALL_CAPTURE_GROUP_STRING.equals(expression);
    }

    public static String parsePattern(String expression) {
        String[] expressions = expression.split("\\|");
        if (expressions.length == 0) return "";
        if (expressions.length == 1) return parseOnePattern(expressions[0]);
        StringBuilder sb = new StringBuilder();
        for (String e: expressions) {
            String pattern = parseOnePattern(e);
            if (pattern.equals(CATCHALL_CAPTURE_GROUP_STRING)) return CATCHALL_CAPTURE_GROUP_STRING;
            sb.append("(?:").append(pattern).append(")|");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static String parseOnePattern(String expression) {
        expression = parseOnePattern(expression, '*', CATCHALL_CAPTURE_GROUP_STRING);
        expression = parseOnePattern(expression, '+', CATCHONE_CAPTURE_GROUP_STRING);
        expression = expression.replaceAll("\\[", "\\\\[");
        expression = expression.replaceAll("\\]", "\\\\]");
        return expression;
    }

    private static String parseOnePattern(String expression, char meta, String regex) {
        if (expression.length() == 0 || expression.equals("" + meta)) expression = regex;
        if ("?!:.".indexOf(expression.charAt(expression.length() - 1)) >= 0 && (expression.length() == 1 || expression.charAt(expression.length() - 2) !='\\')) expression = expression.substring(0, expression.length() - 1);
        if (expression.startsWith(meta + " ")) expression = regex + " " + expression.substring(2);
        if (expression.length() > 0 && expression.charAt(0) == meta) expression = regex + " ?" + expression.substring(1);
        if (expression.endsWith(" " + meta)) expression = expression.substring(0, expression.length() - 2) + " " + regex;
        if (expression.length() > 0 && expression.charAt(expression.length() - 1) == meta) expression = expression.substring(0, expression.length() - 1) + " ?" + regex;
        expression = expression.replaceAll(String.format(" \\%s | \\?\\%s ", meta, meta), " " + regex + " ");
        return expression;
    }

    public static boolean isRegularExpression(String expression) {
        if (expression.indexOf(".*") >= 0 || expression.indexOf("\\h*") >= 0 || (expression.indexOf('(') >= 0 && expression.indexOf(')') >= 0) ) {
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
            if (Character.isLetter(c) || Character.isDigit(c) || c == '['  || c == ']'  || c == ' ' || c == '_') sb.append(c);
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
        return !this.hasCaptureGroups ? this.type.getSubscore() + 1 : this.type.getSubscore();
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
        if (new TimeoutMatcher(CATCHALL_CAPTURE_GROUP_PATTERN.matcher(p)).find()) {
            p = p.replaceAll(CATCHALL_CAPTURE_GROUP_PATTERN.pattern(), "*");
        }
        json.put("type", this.type.name());
        json.put("expression", p);
        if (this.line > 0) json.put("line", line);
        return json;
    }
}