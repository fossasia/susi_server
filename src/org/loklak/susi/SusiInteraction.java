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

import java.util.Base64;
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

    public SusiInteraction(JSONObject json) {
        this.json = json;
    }

    public SusiInteraction(final String query, int maxcount, String client, SusiMind mind) {
        String client_id = Base64.getEncoder().encodeToString(UTF8.getBytes(client));        
        this.json = new JSONObject(true);

        // store metadata
        this.json.put("client_id", client_id);
        this.json.put("query", query);
        long query_date = System.currentTimeMillis();
        this.json.put("query_date", AbstractObjectEntry.utcFormatter.print(query_date));

        // compute the mind reaction
        List<SusiArgument> dispute = mind.react(query, maxcount, client);
        long answer_date = System.currentTimeMillis();
        
        // store answer and actions into json
        this.json.put("answer_date", AbstractObjectEntry.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
        this.json.put("count", maxcount);
        this.json.put("answers", new JSONArray(dispute.stream().map(argument -> {
            JSONObject answer = argument.mindmeld();
            answer.put("actions", argument.getActions().stream()
                    .map(action -> action.apply(argument).toJSON())
                    .collect(Collectors.toList()));
            return answer;
        }).collect(Collectors.toList())));
    }
    
    /**
     * The interaction is the result of a though extraction. We can reconstruct
     * the disput as list of last mindstates using the interaction data.
     * @return a backtrackable thought reconstructed from the interaction data
     */    
    public SusiThought recallDispute() {
        SusiThought dispute = new SusiThought();
        if (this.json.has("answers")) {
            JSONArray answers = this.json.getJSONArray("answers");
            for (int i = answers.length() - 1; i >= 0; i--) {
                SusiThought clonedThought = new SusiThought(answers.getJSONObject(i));
                dispute.addObservation("query", this.json.getString("query"));  // we can unify "query" in queries
                if (clonedThought.has("actions") &&
                    clonedThought.getJSONArray("actions").getJSONObject(0).has("expression"))
                    dispute.addObservation("answer",
                            clonedThought.getJSONArray("actions").getJSONObject(0).getString("expression")); // we can unify with "answer" in queries
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
