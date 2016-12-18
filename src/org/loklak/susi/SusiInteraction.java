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
import org.loklak.server.ClientIdentity;
import org.loklak.tools.UTF8;

/**
 * An interaction with susi is the combination of a query of a user with the response
 * of susi.
 */
public class SusiInteraction {

    JSONObject json;

    public SusiInteraction(final SusiMind mind, final String query, int timezoneOffset, double latitude, double longitude, int maxcount, ClientIdentity identity) {
        this.json = new JSONObject(true);
        
        // get a response from susis mind
        String client = identity.getClient();
        this.setQuery(query);
        this.json.put("count", maxcount);
        SusiThought observation = new SusiThought();
        observation.addObservation("timezoneOffset", Integer.toString(timezoneOffset));
        
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
            observation.addObservation("latitude", Double.toString(latitude));
            observation.addObservation("longitude", Double.toString(longitude));
        }
        this.json.put("client_id", Base64.getEncoder().encodeToString(UTF8.getBytes(client)));
        long query_date = System.currentTimeMillis();
        this.json.put("query_date", AbstractObjectEntry.utcFormatter.print(query_date));
        
        // compute the mind reaction
        List<SusiArgument> dispute = mind.react(query, maxcount, client, observation);
        long answer_date = System.currentTimeMillis();
        
        // store answer and actions into json
        this.json.put("answers", new JSONArray(dispute.stream().map(argument -> argument.finding(client, mind)).collect(Collectors.toList())));
        this.json.put("answer_date", AbstractObjectEntry.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
    }
    
    public SusiInteraction(JSONObject json) {
        this.json = json;
    }

    public SusiInteraction() {
        
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
    
    public Date getQueryDate() {
        String d = this.json.getString("query_date");
        return AbstractObjectEntry.utcFormatter.parseDateTime(d).toDate();
    }

    /**
     * The answer of an interaction contains a list of the mind-melted arguments as one thought
     * @return a list of answer thoughts
     */
    public List<SusiThought> getAnswers() {
        List<SusiThought> answers = new ArrayList<>();
        if (this.json.has("answers")) {
            JSONArray a = this.json.getJSONArray("answers");
            for (int i = 0; i < a.length(); i++) answers.add(new SusiThought(a.getJSONObject(i)));
        }
        return answers;
    }
    
    /**
     * the expression of an interaction is the actual string that comes out as response to
     * actions of the user
     * @return a response string
     */
    public String getExpression() {
        List<SusiThought> answers = getAnswers();
        if (answers == null || answers.size() == 0) return "";
        SusiThought t = answers.get(0);
        List<SusiAction> actions = t.getActions();
        if (actions == null || actions.size() == 0) return "";
        SusiAction a = actions.get(0);
        List<String> phrases = a.getPhrases();
        if (phrases == null || phrases.size() == 0) return "";
        return phrases.get(0);
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
