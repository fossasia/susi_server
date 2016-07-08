/**
 *  ResultList
 *  Copyright 12.11.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.objects;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.loklak.susi.SusiThought;

public class ResultList<E> extends ArrayList<E> {

    private static final long serialVersionUID = -982453065951290203L;

    private long hits = -1;
    
    public ResultList() {
        super();
    }
 
    /**
     * set the number of total hits this list has
     * @param totalHits
     */
    public void setHits(long totalHits) {
        this.hits = totalHits;
    }
    
    /**
     * get the number of total hits
     * @return
     */
    public long getHits() {
        return this.hits == -1 ? this.size() : this.hits;
    }
    
    public void clear() {
        super.clear();
        this.hits = -1;
    }
    
    public SusiThought toSusi() throws JSONException {
        SusiThought json = new SusiThought().setHits(this.size());
        JSONArray statuses = new JSONArray();
        for (E t: this) {
            if (t instanceof QueryEntry) {
                QueryEntry qe = (QueryEntry) t;
                statuses.put(qe.toJSON());
            }
        }
        json.setData(statuses);
        return json;
    }
}
