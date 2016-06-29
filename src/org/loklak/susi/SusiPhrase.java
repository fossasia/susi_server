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

public class SusiPhrase {

    public static enum Type {regex, pattern;}
    
    private static Pattern dspace = Pattern.compile("  ");
    
    private Type type; 
    Pattern pattern;
    
    public SusiPhrase(JSONObject json) throws PatternSyntaxException {
        if (!json.has("type")) throw new PatternSyntaxException("type missing", "", 0);
        try {
            this.type = Type.valueOf(json.getString("type"));
        } catch (IllegalArgumentException e) {throw new PatternSyntaxException("type value is wrong", json.getString("type"), 0);}
        if (!json.has("expression")) throw new PatternSyntaxException("expression missing", "", 0);
        String expression = json.getString("expression");
        expression = expression.toLowerCase().replaceAll("\\#", "  ");
        Matcher m;
        while ((m = dspace.matcher(expression)).find()) expression = m.replaceAll(" ");
        if (this.type == Type.pattern) expression = expression.replaceAll("\\*", "(.*)");
        this.pattern = Pattern.compile(expression);
    }
    
    public Type getType() {
        return this.type;
    }
    
    public Pattern getPattern() {
        return this.pattern;
    }
}