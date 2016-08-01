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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.objects.AbstractObjectEntry;

/**
 * An interaction with susi is the combination of a query of a user with the response
 * of susi.
 */
public class SusiInteraction {

    JSONObject json;

    public SusiInteraction(final String query, int maxcount, SusiMind mind) {
        this.json = new JSONObject(true);
        this.json.put("query", query);
        long query_date = System.currentTimeMillis();
        this.json.put("query_date", AbstractObjectEntry.utcFormatter.print(query_date));
        List<SusiArgument> dispute = mind.react(query, maxcount);
        long answer_date = System.currentTimeMillis();
        this.json.put("answer_date", AbstractObjectEntry.utcFormatter.print(answer_date));
        this.json.put("answer_time", answer_date - query_date);
        this.json.put("count", maxcount);
        JSONArray answers = new JSONArray();
        dispute.forEach(answer -> answers.put(answer.mindstate()));
        this.json.put("answers", answers);
    }
    
    public SusiInteraction(JSONObject json) {
        this.json = json;
    }
    
    public JSONObject getJSON() {
        return this.json;
    }
    
}
