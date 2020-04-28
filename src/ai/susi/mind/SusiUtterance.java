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

    private final static Pattern metastarp = Pattern.compile(String.format(" \\%s | \\?\\%s ", '*', '*'));
    private final static Pattern metaplusp = Pattern.compile(String.format(" \\%s | \\?\\%s ", '+', '+'));

    /**
     * Susi Utterance patterns:
     * 
     * - CATCHONE: "+" is a greedy capturing group, capturing exactly 1 word
     * - CATCHSOME: "*" or "_" is a greedy capturing group, capturing 1 or more words - this is never empty
     * - IGNOREALL: "^" or "#" is a greedy capturing, capturing 0 or more words - thus might be empty
     * 
     * Capturing groups ("+", "*" or "_") fill up variables in order of appearance, are assigned to $1$, $2$ and so on.
     */

    private final static String CATCHONE_CAPTURE_GROUP_STRING = "(^\\S+)"; // or "#", only one word
    private final static Pattern CATCHONE_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHONE_CAPTURE_GROUP_STRING));

    private final static String CATCHSOME_CAPTURE_GROUP_STRING = "(.*)"; // or "_", greedy capturing everything is the best choice: that covers words phrases as well
    private final static Pattern CATCHSOME_CAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(CATCHSOME_CAPTURE_GROUP_STRING));
    
    private final static String IGNOREALL_NONCAPTURE_GROUP_STRING = "(?:.*)";
    private final static Pattern IGNOREALL_NONCAPTURE_GROUP_PATTERN = Pattern.compile(Pattern.quote(IGNOREALL_NONCAPTURE_GROUP_STRING));
    
    
    private final static Pattern dspace = Pattern.compile("  ");
    private final static Pattern wspace = Pattern.compile(",|;:"); // "#^_*" MUST NOT be a whitespace as they are special characters
    private final static Pattern sbackopen = Pattern.compile("\\[");
    private final static Pattern sbackclose = Pattern.compile("\\]");
    private final static Pattern cgopen = Pattern.compile("\\(\\?");

    private SusiPattern pattern;
    private Type type;
    private boolean hasCaptureGroup;
    private int meatsize;
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

        init(json.getString("expression"), this.type == Type.prior, json.has("line") ? json.getInt("line") : 0);
    }

    /**
     * create a simple phrase
     * @param expression
     * @param prior if true, this phrase has priority
     * @throws PatternSyntaxException
     */
    public SusiUtterance(String expression, boolean prior, int line) throws PatternSyntaxException {
        init(expression, prior, line);
    }

    private void init(String expression, boolean prior, int line) throws PatternSyntaxException {
        this.type = prior ? Type.prior : Type.minor;
        this.line = line;
        this.hasCaptureGroup = false;

        assert expression != null;
        if (expression == null) expression = "";

        // normalize expression: make a canonical form of utterances and queries
        expression = normalizeExpression(expression);

        // replace all usages of "_" by "*" and all usage of "#" by "^"
        expression = expression.replace('_', '*').replace('#', '^');

        // fix expression: patches for wrong utterances and syntax mis-uses.
        if (expression.indexOf(".*") >= 0 ||
            (expression.charAt(0) == '^' && expression.charAt(expression.length() - 1) == '$') ||
            (expression.charAt(0) == '(' && expression.charAt(expression.length() - 1) == ')')) {
            // this is a regular expression, let it be as it is.
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

        // parse pattern: create a proper regular expression from simple wildcard form
        String[] expressions = expression.split("\\|");
        if (expressions.length == 0) {
            expression = "";
        } else if (expressions.length == 1) {
            CaptureGroupCompiler onePattern = new CaptureGroupCompiler(expressions[0]);
            this.hasCaptureGroup = onePattern.hasCaptureGroup;
            expression = onePattern.getPattern();
        } else {
            StringBuilder sb = new StringBuilder();
            for (String e: expressions) {
                CaptureGroupCompiler onePattern = new CaptureGroupCompiler(e);
                this.hasCaptureGroup = this.hasCaptureGroup || onePattern.hasCaptureGroup;
                String pattern1 = onePattern.getPattern();
                if (pattern1.equals(CATCHSOME_CAPTURE_GROUP_STRING)) {
                    expression = CATCHSOME_CAPTURE_GROUP_STRING + " ";
                    break;
                }
                sb.append("(?:").append(pattern1).append(")|");
            }
            expression = sb.substring(0, sb.length() - 1);
        }

        // write class variables
        this.pattern = new SusiPattern(expression, this.hasCaptureGroup || expression.indexOf('*') >= 0);
        if (expression.equals("(.*)")) this.type = Type.minor;

        // measure the meat size
        this.meatsize = Math.min(99, extractMeat(expression).length());
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

    /**
     * Query String Normalization:
     * This must be applied to every utterance AND to every query. Applying the same normalization ensures that
     * queries match for a certain variation of different ways to express.
     * This includes i.e. uppercase/lowecase spelling.
     * @param s
     * @return a canonical form of utterances and queries
     */
    public static String normalizeExpression(String s) {
        s = s.trim().toLowerCase();
        Matcher m;
        while ((m = wspace.matcher(s)).find()) s = m.replaceAll(" ");
        while ((m = dspace.matcher(s)).find()) s = m.replaceAll(" ");
        if (s.length() == 0) return s; // prevent StringIndexOutOfBoundsException which can happen in the next line
        int p = -1;
        while ((p = "?!:.".indexOf(s.charAt(s.length() - 1))) >= 0 && (s.length() == 1 || s.charAt(s.length() - 2) != '\\')) {
            // cut out punctuation marks only at the end if it is not a part of a regular expression
            s = s.substring(0, s.length() - 1).trim();
        }
        // to be considered: https://en.wikipedia.org/wiki/Wikipedia:List_of_English_contractionst
        p = -1;
        return s;
    }

    public boolean isCatchallPhrase() {
        String expression = this.pattern.pattern();
        return CATCHSOME_CAPTURE_GROUP_STRING.equals(expression);
    }

    public static boolean isCatchallPhrase(JSONObject json) {
        String expression = json.getString("expression");
        return CATCHSOME_CAPTURE_GROUP_STRING.equals(expression);
    }

    /**
     * The CaptureGroupCompiler translates an expression in LoT using simple meta characters (i.e. '*', '^' or '+')
     * into a Java-style regular expression pattern including required capture groups.
     * A flag is also created to show that capture groups are created.
     */
    private static class CaptureGroupCompiler {

        private String pattern;
        private boolean hasCaptureGroup;

        public CaptureGroupCompiler(String expression) {
            this.hasCaptureGroup = false;
            expression = translateMeta2Regex(expression, '*', CATCHSOME_CAPTURE_GROUP_STRING);
            Matcher m = metastarp.matcher(expression);
            if (m.find()) this.hasCaptureGroup = true;
            expression = m.replaceAll(" " + CATCHSOME_CAPTURE_GROUP_STRING + " ");
            expression = translateMeta2Regex(expression, '+', CATCHONE_CAPTURE_GROUP_STRING);
            m = metaplusp.matcher(expression);
            if (m.find()) this.hasCaptureGroup = true;
            expression = m.replaceAll(" " + CATCHONE_CAPTURE_GROUP_STRING + " ");
            expression = sbackopen.matcher(expression).replaceAll("\\\\[");
            expression = sbackclose.matcher(expression).replaceAll("\\\\]");
            this.pattern = expression;
        }

        /**
         * find meta characters in an expression and replace with proper regular expression
         * @param expression the expression where characters are replaced
         * @param meta a character that is replaced
         * @param regex the regular expression which is the replacement for the character
         * @return the translated expression
         */
        private String translateMeta2Regex(String expression, char meta, String regex) {
            if (expression.length() == 0 || (expression.length() == 1 && expression.charAt(0) == meta)) {
                expression = regex;
                this.hasCaptureGroup = true;
            } else {
                if (expression.startsWith(meta + " ")) {
                    // expression starts with meta + <space>
                    expression = regex + " " + expression.substring(2);
                    this.hasCaptureGroup = true;
                }
                if (expression.length() > 0 && expression.charAt(0) == meta) {
                    expression = regex + " ?" + expression.substring(1);
                    this.hasCaptureGroup = true;
                }
                if (expression.endsWith(" " + meta)) {
                    // expression ends with <space> + meta
                    expression = expression.substring(0, expression.length() - 2) + " " + regex;
                    this.hasCaptureGroup = true;
                }
                if (expression.length() > 0 && expression.charAt(expression.length() - 1) == meta) {
                    expression = expression.substring(0, expression.length() - 1) + " ?" + regex;
                    this.hasCaptureGroup = true;
                }
            }
            return expression;
        }

        public String getPattern() {
            return this.pattern;
        }

        public boolean hasCaptureGroup() {
            return this.hasCaptureGroup;
        }
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
        sb.trimToSize();
        return sb.toString();
    }

    /**
     * get the pre-compiled regular expression pattern
     * @return a java pattern
     */
    public SusiPattern getPattern() {
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
        int subscore = this.type.getSubscore();
        return this.hasCaptureGroup ? subscore : subscore + 1;
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
        if (new TimeoutMatcher(CATCHSOME_CAPTURE_GROUP_PATTERN.matcher(p)).find()) {
            p = p.replaceAll(CATCHSOME_CAPTURE_GROUP_PATTERN.pattern(), "*");
        }
        json.put("type", this.type.name());
        json.put("expression", p);
        if (this.line > 0) json.put("line", line);
        return json;
    }

    public static void main(String[] args) {
        String[] tests = new String[] {
                " a * * b * c?",
                "* a * * b * c?",
                "*a * * b * c?",
                " a + + b * c?",
                "*", "hi", "hi ?*", "hi *",
                "# x y z"
        };

        for (String test: tests) {
        SusiUtterance utterance = new SusiUtterance(test, false, 0);
        System.out.println(utterance.getPattern().pattern() + ":" + utterance.getSubscore());
        }
    }

}