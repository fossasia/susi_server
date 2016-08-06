/**
 *  SusiThought
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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A thought is a piece of data that can be remembered. The structure or the thought can be
 * modeled as a table which may be created using the retrieval of information from elsewhere
 * of the current argument.
 */
public class SusiThought extends JSONObject {

    final String metadata_name, data_name;

    /**
     * create an empty thought, to be filled with single data entities.
     */
    public SusiThought() {
        super(true);
        this.metadata_name = "metadata";
        this.data_name = "data";
    }
    
    /**
     * create a clone of a json object as a SusiThought object
     * @param json the 'other' thought, probably an exported and re-imported thought
     */
    public SusiThought(JSONObject json) {
        this();
        if (json.has(this.metadata_name)) this.put(this.metadata_name, json.getJSONObject(this.metadata_name));
        if (json.has(this.data_name)) this.setData(json.getJSONArray(this.data_name));
        if (json.has("actions")) this.put("actions", json.getJSONArray("actions"));
    }
    
    /**
     * Create an initial thought using the matcher on an expression.
     * Such an expression is like the input from a text source which contains keywords
     * that are essential for the thought. The matcher extracts such information.
     * Matching informations are named using the order of the appearance of the information pieces.
     * The first information is named '1', the second '2' and so on. The whole information which contained
     * the matching information is named '0'.
     * @param matcher
     */
    public SusiThought(Matcher matcher) {
        this();
        this.setOffset(0).setHits(1);
        JSONObject row = new JSONObject();
        row.put("0", matcher.group(0));
        for (int i = 0; i < matcher.groupCount(); i++) {
            row.put(Integer.toString(i + 1), matcher.group(i + 1));
        }
        this.setData(new JSONArray().put(row));
    }
    
    @Deprecated
    public SusiThought(String metadata_name, String data_name) {
        super(true);
        this.metadata_name = metadata_name;
        this.data_name = data_name;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SusiThought)) return false;
        SusiThought t = (SusiThought) o;
        return this.getData().equals(t.getData());
    }
    
    /**
     * In a series of information pieces the first information piece has number 0.
     * If the thought is a follow-up series of a previous set of information, an offset is needed.
     * That can be set here.
     * @param offset the offset to a previous set of information pieces.
     * @return the thought
     */
    public SusiThought setOffset(int offset) {
        getMetadata().put("offset", offset);
        return this;
    }
    
    public int getOffset() {
        return getMetadata().has("offset") ? getMetadata().getInt("offset") : 0;
    }
    
    /**
     * The number of information pieces in a set of informations may have a count.
     * @return hits number of information pieces
     */
    public int getCount() {
        return getData().length();
    }
    
    /**
     * While the number of information pieces in a whole has a count, the number of relevant
     * information pieces may have been extracted. The hits number gives the number of relevant
     * pieces. This can be set here.
     * @param hits number of information pieces
     * @return the thought
     */
    public SusiThought setHits(int hits) {
        getMetadata().put("hits", hits);
        return this;
    }
    
    public int getHits() {
        return getMetadata().has("hits") ? getMetadata().getInt("hits") : 0;
    }

    /**
     * The process which created this thought may have a name or description string.
     * To document what happened, the process namen can be given here
     * @param query the process which formed this thought
     * @return the thought
     */
    public SusiThought setProcess(String processName) {
        getMetadata().put("process", processName);
        return this;
    }

    /**
     * If this thought was the result of a retrieval using a specific expression, that expression is
     * called the query. The query can be attached to a thought
     * @param query the expression which caused that this thought was formed
     * @return the thought
     */
    public SusiThought setQuery(String query) {
        getMetadata().put("query", query);
        return this;
    }
    
    public String getQuery() {
        return getMetadata().has("query") ? getMetadata().getString("query") : "";
    }
    
    /**
     * If the expression to create this thought had an agent that expressed the result set of the
     * information contained in this thought, it is called the scraper. The scraper name can be attached here.
     * @param scraperInfo the scraper that created this thought
     * @return the thought
     */
    public SusiThought setScraperInfo(String scraperInfo) {
        getMetadata().put("scraperInfo", scraperInfo);
        return this;
    }
    
    /**
     * All details of the creation of this thought is collected in a metadata statement.
     * @return the set of meta information for this thought
     */
    private JSONObject getMetadata() {
        JSONObject md;
        if (this.has(metadata_name)) md = this.getJSONObject(metadata_name); else {
            md = new JSONObject();
            this.put(metadata_name, md);
        }
        if (!md.has("count")) md.put("count", getData().length());
        return md;
    }
    
    /**
     * Information contained in this thought has the form of a result set table, organized in rows and columns.
     * The columns must have all the same name in each row.
     * @param table the information for this thought.
     * @return the thought
     */
    public SusiThought setData(JSONArray table) {
        this.put(data_name, table);
        JSONObject md = getMetadata();
        md.put("count", getData().length());
        return this;
    }

    /**
     * Information contained in this thought can get returned as a table, a set of information pieces.
     * @return a table of information pieces as a set of rows which all have the same column names.
     */
    public JSONArray getData() {
        if (this.has(data_name)) return this.getJSONArray(data_name);
        JSONArray a = new JSONArray();
        this.put(data_name, a);
        return a;
    }
    
    /**
     * If during thinking we observe something that we want to memorize, we can memorize this here
     * @param featureName the object key
     * @param observation the object value
     * @return the thought
     */
    public SusiThought addObservation(String featureName, String observation) {
        JSONArray data = getData();
        JSONObject spark;
        if (data.length() == 0) {
            spark = new JSONObject();
            this.setData(new JSONArray().put(spark));
        } else {
            spark = data.getJSONObject(0);
        }
        spark.put(featureName, observation);
        return this;
    }
}
