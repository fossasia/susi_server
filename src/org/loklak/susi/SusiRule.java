/**
 *  SusiRule
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import org.json.JSONArray;
import org.json.JSONObject;

public class SusiRule {

    private JSONObject json;
    private List<SusiPhrase> phrases;
    private List<SusiProcess> process;
    private List<SusiAction> actions;

    public SusiRule(JSONObject json) throws PatternSyntaxException {
        this.json = json;
        
        // extract the phrases
        if (!this.json.has("phrases")) throw new PatternSyntaxException("phrases missing", "", 0);
        JSONArray p = (JSONArray) this.json.remove("phrases");
        this.phrases = new ArrayList<>(p.length());
        p.forEach(q -> this.phrases.add(new SusiPhrase((JSONObject) q)));

        // extract the phrases
        if (!this.json.has("process")) throw new PatternSyntaxException("process missing", "", 0);
        p = (JSONArray) this.json.remove("process");
        this.process = new ArrayList<>(p.length());
        p.forEach(q -> this.process.add(new SusiProcess((JSONObject) q)));
        
        // extract the actions
        if (!this.json.has("actions")) throw new PatternSyntaxException("actions missing", "", 0);
        p = (JSONArray) this.json.remove("actions");
        this.actions = new ArrayList<>(p.length());
        p.forEach(q -> this.actions.add(new SusiAction((JSONObject) q)));
    }
    
    public Set<String> getKeys() {
        Set<String> set = new HashSet<>();
        if (json.has("keys")) json.getJSONArray("keys").forEach(o -> set.add((String) o));
        return set;
    }
    
    public String getComment() {
        return json.has("comment") ? json.getString("comment") : "";
    }

    public int getScore() {
        return json.has("score") ? json.getInt("score") : 0;
    }
    
    public List<SusiPhrase> getPhrases() {
        return this.phrases;
    }
    
    public List<SusiProcess> getProcess() {
        return this.process;
    }

    public List<SusiAction> getActions() {
        return this.actions;
    }

    public Matcher matcher(String s) {
        for (SusiPhrase p: this.phrases) {
            Matcher m = p.pattern.matcher(s);
            if (m.find()) return m;
        }
        return null;
    }

}
