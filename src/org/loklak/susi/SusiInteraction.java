/**
 *  SusiInteraction
 *  Copyright 24.07.2016 by Michael Peter Christen, @0rb1t3r
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
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.objects.AbstractObjectEntry;
import org.loklak.tools.UTF8;

/**
 * An interaction with susi is the combination of a query of a user with the response
 * of susi.
 */
public class SusiInteraction {

    private JSONObject json;

    public SusiInteraction() {
        this.json = new JSONObject(true);
    }

    public SusiInteraction(JSONObject json) {
        this.json = json;
    }
    
    public SusiInteraction setQuery(final String query) {
        this.json.put("query", query);
        return this;
    }
    
    public String getQuery() {
        if (!this.json.has("query")) return "";
        String q = this.json.getString("query");
        return q == null ? "" : q;
    }
    
    public SusiInteraction react(int maxcount, String client, SusiMind mind, SusiThought observation) {
        this.json.put("client_id", Base64.getEncoder().encodeToString(UTF8.getBytes(client)));
        long query_date = System.currentTimeMillis();
        this.json.put("query_date", AbstractObjectEntry.utcFormatter.print(query_date));
        
        // compute the mind reaction
        String query = this.json.getString("query");
        
        List<SusiArgument> dispute = mind.react(query, maxcount, client, observation);
        long answer_date = System.currentTimeMillis();
        
        // store answer and actions into json
        this.json.put("answer_date", AbstractObjectEntry.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
        this.json.put("count", maxcount);
        this.json.put("answers", new JSONArray(dispute.stream().map(argument -> {
            SusiThought answer = argument.mindmeld(true);
            answer.put("actions", 
                    argument.getActions().stream()
                        .map(action -> action.apply(argument, mind, client).toJSONClone())
                        .collect(Collectors.toList()));
            return answer;
        }).collect(Collectors.toList())));
        
        //System.out.println(this.json);
        return this;
    }
    
    public Date getQueryDate() {
        String d = this.json.getString("query_date");
        return AbstractObjectEntry.utcFormatter.parseDateTime(d).toDate();
    }

    public String getAnswer() {
        if (!this.json.has("answers")) return "";
        JSONArray a = this.json.getJSONArray("answers");
        if (a.length() == 0) return "";
        JSONObject b = a.getJSONObject(0);
        if (!b.has("actions")) return "";
        JSONArray c = b.getJSONArray("actions");
        if (c.length() == 0) return "";
        JSONObject d = c.getJSONObject(0);
        if (!d.has("expression")) return "";
        return d.getString("expression");
    }
    
    /**
     * The interaction is the result of a though extraction. We can reconstruct
     * the dispute as list of last mindstates using the interaction data.
     * @return a backtrackable thought reconstructed from the interaction data
     */    
    public SusiThought recallDispute() {
        SusiThought dispute = new SusiThought();
        if (this.json.has("answers")) {
            JSONArray answers = this.json.getJSONArray("answers"); // in most cases there is only one answer
            for (int i = answers.length() - 1; i >= 0; i--) {
                SusiThought clonedThought = new SusiThought(answers.getJSONObject(i));
                dispute.addObservation("query", this.json.getString("query"));  // we can unify "query" in queries
                SusiAction expressionAction = null;
                for (SusiAction a: clonedThought.getActions()) {
                    ArrayList<String> phrases = a.getPhrases();
                    if (phrases.size() > 0) {expressionAction = a; break;}
                }
                if (expressionAction != null) dispute.addObservation("answer", expressionAction.getPhrases().get(0)); // we can unify with "answer" in queries

                // add all data from the old dispute
                JSONArray clonedData = clonedThought.getData();
                if (clonedData.length() > 0) {
                    JSONObject row = clonedData.getJSONObject(0);
                    row.keySet().forEach(key -> {if (key.startsWith("_")) dispute.addObservation(key, row.getString(key));});
                    //data.put(clonedData.get(0));
                }
            }
        }
        return dispute;
    }
    
    public JSONObject getJSON() {
        return this.json;
    }
    
    public String toString() {
        return this.json.toString();
    }
}
