/**
 *  Query
 *  Copyright 26.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.loklak.tools.DateParser;

/**
 * A Query is a recording of a search result based on the query.
 * THIS IS NOT RECORDED TO TRACK USER ACTIONS, THIS IS USED TO RE-SEARCH A QUERY INDEFINITELY!
 * Each query will be stored in elasticsearch and retrieved by the caretaker process in
 * order of the retrieval_next field. That date is calculated based on the number of search results
 * in the last time; the retrieval_next is estimated based on the time interval of all tweets in
 * the search results of the last query.
 * 
 * Privacy is important:
 * TO ALL COMMITTERS: please do not add any user-identification details to the data structures
 * to protect the privacy of the users; TO CODE EVALUATORS: please look for yourself that this
 * code does not contain any user-related information (like IP, user agent etc.).
 */
public class QueryEntry extends AbstractIndexEntry implements IndexEntry {
    
    private final static long DAY_MILLIS = 1000L * 60L * 60L * 24L;
    private final static int MINIMUM_PERIOD = 1000; // for single messages; used to calculate retrieval_next times

    public static double ttl_factor = 0.5d;
    
    protected String query;           // the query in the exact way as the user typed it in
    protected int query_length;       // the length in the query, number of characters
    protected SourceType source_type; // the (external) retrieval system where that query was submitted
    protected int timezoneOffset;     // the timezone offset of the user
    protected Date query_first;       // the date when this query was submitted by the user the first time
    protected Date query_last;        // the date when this query was submitted by the user the last time
    protected Date retrieval_last;    // the last time when this query was submitted to the external system
    protected Date retrieval_next;    // the estimated next time when the query should be submitted to get all messages
    protected Date expected_next;     // the estimated next time when one single message will appear
    protected int query_count;        // the number of queries by the user of that query done so far
    protected int retrieval_count;    // the number of retrievals of that query done so far to the external system
    protected long message_period;     // the estimated period length between two messages
    protected int messages_per_day;   // a message frequency based on the last query

    /**
     * This initializer can only be used for first-time creation of a query track.
     * @param query
     * @param timezoneOffset
     * @param timeline
     * @param source_type
     * @throws MalformedURLException
     */
    public QueryEntry(final String query, final int timezoneOffset, final Timeline timeline, final SourceType source_type, final boolean byUserQuery) {
        this.query = query;
        this.query_length = query.length();
        this.timezoneOffset = timezoneOffset;
        this.source_type = source_type;
        this.retrieval_count = 0; // will be set to 1 with first update
        update(timeline, byUserQuery);
        this.query_first = retrieval_last;
    }

    protected QueryEntry(Map<String, Object> map) throws IllegalArgumentException {
        init(map);
    }
    
    public void init(Map<String, Object> map) throws IllegalArgumentException {
        this.query = (String) map.get("query");
        this.query_length = (int) DAO.noNULL((Number) map.get("query_length"));
        String source_type_string = (String) map.get("source_type"); if (source_type_string == null) source_type_string = SourceType.USER.name();
        this.source_type = SourceType.valueOf(source_type_string);
        this.timezoneOffset = (int) DAO.noNULL((Number) map.get("timezoneOffset"));
        this.query_first = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) map.get("query_first")).toDate();
        this.query_last = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) map.get("query_last")).toDate();
        this.retrieval_last = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) map.get("retrieval_last")).toDate();
        this.retrieval_next = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) map.get("retrieval_next")).toDate();
        this.expected_next = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) map.get("expected_next")).toDate();
        this.query_count = (int) DAO.noNULL((Number) map.get("query_count"));
        this.retrieval_count = (int) DAO.noNULL((Number) map.get("retrieval_count"));
        this.message_period = DAO.noNULL((Number) map.get("message_period"));
        this.messages_per_day = (int) DAO.noNULL((Number) map.get("messages_per_day"));
    }
    
    /**
     * update the query entry
     * @param timeline the latest timeline retrieved from the target system
     * @param byUserQuery is true, if the query was submitted by the user; false if the query was submitted by an automatic system
     */
    public void update(final Timeline timeline, final boolean byUserQuery) {
        this.retrieval_last = new Date();
        this.retrieval_count++;
        if (byUserQuery) {
            this.query_count++;
            this.query_last = this.retrieval_last;
        }
        this.message_period = timeline.period();
        this.messages_per_day = (int) (DAY_MILLIS / this.message_period); // this is an interpolation based on the last tweet list, can be 0!
        this.expected_next = new Date(this.retrieval_last.getTime() + ((long) (ttl_factor *  this.message_period)));
        this.retrieval_next = new Date(this.retrieval_last.getTime() + ((long) (ttl_factor * timeline.size() * Math.max(MINIMUM_PERIOD, this.message_period))));
    }
    
    /**
     * A 'blind' update can be done if the user submits a query but there are rules which prevent that the target system is queried
     * as well. Then the query result is calculated using the already stored messages. To reflect this, only the query-related
     * attributes are changed.
     */
    public void update() {
        this.query_count++;
        this.query_last = new Date();
    }
    
    public String getQuery() {
        return this.query;
    }

    public int getQueryLength() {
        return this.query_length;
    }

    public SourceType getSourceType() {
        return this.source_type;
    }

    public Date getQueryFirst() {
        return this.query_first;
    }

    public Date getQueryLast() {
        return this.query_last;
    }

    public Date getRetrievalLast() {
        return this.retrieval_last;
    }

    public Date getRetrievalNext() {
        return this.retrieval_next;
    }

    public Date getExpectedNext() {
        return this.expected_next;
    }

    public int getTimezoneOffset() {
        return this.timezoneOffset;
    }

    public int getQueryCount() {
        return this.query_count;
    }

    public int getRetrievalCount() {
        return this.retrieval_count;
    }

    public int getMessagesPerDay() {
        return this.messages_per_day;
    }
    
    public void toJSON(XContentBuilder m) {
        try {
            m.startObject();
            m.field("query", this.query);
            m.field("query_length", this.query_length);
            m.field("source_type", this.source_type);
            m.field("timezoneOffset", this.timezoneOffset);
            m.field("query_first", this.query_first);
            m.field("query_last", this.query_last);
            m.field("retrieval_last", this.retrieval_last);
            m.field("retrieval_next", this.retrieval_next);
            m.field("expected_next", this.expected_next);
            m.field("query_count", this.query_count);
            m.field("retrieval_count", this.retrieval_count);
            m.field("message_period", this.message_period);
            m.field("messages_per_day", this.messages_per_day);
            m.endObject();
        } catch (IOException e) {
        }
    }

    private final static Pattern tokenizerPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // tokenizes Strings into terms respecting quoted parts

    public static class ElasticsearchQuery {
        
        QueryBuilder queryBuilder;
        Date since, until;
        
        public ElasticsearchQuery(String q, int timezoneOffset) {
            // tokenize the query
            List<String> qe = new ArrayList<String>();
            Matcher m = tokenizerPattern.matcher(q);
            while (m.find()) qe.add(m.group(1));
            
            // twitter search syntax:
            //   term1 term2 term3 - all three terms shall appear
            //   "term1 term2 term3" - exact match of all terms
            //   term1 OR term2 OR term3 - any of the three terms shall appear
            //   from:user - tweets posted from that user
            //   to:user - tweets posted to that user
            //   @user - tweets which mention that user
            //   near:"location" within:xmi - tweets that are near that location
            //   #hashtag - tweets containing the given hashtag
            //   since:2015-04-01 until:2015-04-03 - tweets within given time range
            String text = "";
            ArrayList<String> users = new ArrayList<>();
            ArrayList<String> hashtags = new ArrayList<>();
            HashMap<String, String> modifier = new HashMap<>();
            for (String t: qe) {
                if (t.length() == 0) continue;
                if (t.startsWith("@")) {
                    users.add(t.substring(1));
                } else if (t.startsWith("#")) {
                    hashtags.add(t.substring(1));
                } else if (t.indexOf(':') > 0) {
                    int p = t.indexOf(':');
                    modifier.put(t.substring(0, p).toLowerCase(), t.substring(p + 1));
                } else {
                    text += t + " ";
                }
            }
            if (modifier.containsKey("to")) users.add(modifier.get("to"));
            text = text.trim();
            
            // compose query
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            if (text.length() > 0) query.must(QueryBuilders.matchQuery("text", text));
            for (String user: users) query.must(QueryBuilders.termQuery("mentions", user));
            for (String hashtag: hashtags) query.must(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            if (modifier.containsKey("from")) query.must(QueryBuilders.termQuery("screen_name", modifier.get("from")));
            if (modifier.containsKey("near")) {
                BoolQueryBuilder nearquery = QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("place_name", modifier.get("near")))
                        .should(QueryBuilders.matchQuery("text", modifier.get("near")));
                query.must(nearquery);
            }
            if (modifier.containsKey("since")) try {
                Calendar since = DateParser.parse(modifier.get("since"), timezoneOffset);
                this.since = since.getTime();
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").from(this.since);
                if (modifier.containsKey("until")) {
                    Calendar until = DateParser.parse(modifier.get("until"), timezoneOffset);
                    if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                        // until must be the day which is included in results.
                        // To get the result within the same day, we must add one day.
                        until.add(Calendar.DATE, 1);
                    }
                    this.until = until.getTime();
                    rangeQuery.to(this.until);
                } else {
                    this.until = new Date(Long.MAX_VALUE);
                }
                query.must(rangeQuery);
            } catch (ParseException e) {} else {
                this.since = new Date(0);
                this.until = new Date(Long.MAX_VALUE);
            }
            this.queryBuilder = query;
        }
    }
}
