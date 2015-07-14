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

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.loklak.geo.PlaceContext;
import org.loklak.harvester.SourceType;
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
    private final static int RETRIEVAL_CONSTANT = 20; // the number of messages that we get with each retrieval at maximum
    
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
    protected long message_period;    // the estimated period length between two messages
    protected int messages_per_day;   // a message frequency based on the last query
    protected long score_retrieval;   // score for the retrieval order
    protected long score_suggest;     // score for the suggest order

    /**
     * This initializer can only be used for first-time creation of a query track.
     * @param query
     * @param timezoneOffset
     * @param timeline
     * @param source_type
     * @throws MalformedURLException
     */
    public QueryEntry(final String query, final int timezoneOffset, final long message_period, final SourceType source_type, final boolean byUserQuery) {
        this.query = query;
        this.query_length = query.length();
        this.timezoneOffset = timezoneOffset;
        this.source_type = source_type;
        this.retrieval_count = 0; // will be set to 1 with first update
        this.message_period = 0; // means: unknown
        this.messages_per_day = 0; // means: unknown
        this.score_retrieval = 0;
        this.score_suggest = 0;
        update(message_period, byUserQuery);
        this.query_first = retrieval_last;
    }

    protected QueryEntry(Map<String, Object> map) throws IllegalArgumentException {
        init(map);
    }
    
    public void init(Map<String, Object> map) throws IllegalArgumentException {
        this.query = (String) map.get("query");
        this.query_length = (int) parseLong((Number) map.get("query_length"));
        String source_type_string = (String) map.get("source_type"); if (source_type_string == null) source_type_string = SourceType.USER.name();
        this.source_type = SourceType.valueOf(source_type_string);
        this.timezoneOffset = (int) parseLong((Number) map.get("timezoneOffset"));
        Date now = new Date();
        this.query_first = parseDate(map.get("query_first"), now);
        this.query_last = parseDate(map.get("query_last"), now);
        this.retrieval_last = parseDate(map.get("retrieval_last"), now);
        this.retrieval_next = parseDate(map.get("retrieval_next"), now);
        this.expected_next = parseDate(map.get("expected_next"), now);
        this.query_count = (int) parseLong((Number) map.get("query_count"));
        this.retrieval_count = (int) parseLong((Number) map.get("retrieval_count"));
        this.message_period = parseLong((Number) map.get("message_period"));
        this.messages_per_day = (int) parseLong((Number) map.get("messages_per_day"));
        this.score_retrieval = (int) parseLong((Number) map.get("score_retrieval"));
        this.score_suggest = (int) parseLong((Number) map.get("score_suggest"));
    }
    
    /**
     * update the query entry
     * @param timeline the latest timeline retrieved from the target system
     * @param byUserQuery is true, if the query was submitted by the user; false if the query was submitted by an automatic system
     */
    public void update(final long message_period, final boolean byUserQuery) {
        this.retrieval_last = new Date();
        this.retrieval_count++;
        if (byUserQuery) {
            this.query_count++;
            this.query_last = this.retrieval_last;
        }
        long new_message_period = message_period; // can be Long.MAX_VALUE if less than 2 messages are in timeline!
        int new_messages_per_day = (int) (DAY_MILLIS / new_message_period); // this is an interpolation based on the last tweet list, can be 0!
        if (new_message_period == Long.MAX_VALUE || new_messages_per_day == 0) {
            this.message_period = this.message_period == 0 ? DAY_MILLIS : Math.min(DAY_MILLIS, this.message_period * 2);
        } else {
            this.message_period = this.message_period == 0 ? new_message_period : (this.message_period + new_message_period) / 2;
        }
        this.messages_per_day = (int) (DAY_MILLIS / this.message_period);
        this.expected_next = new Date(this.retrieval_last.getTime() + ((long) (ttl_factor *  this.message_period)));
        long pivot_period = DAO.getConfig("retrieval.pivotfrequency", 10000);
        long strategic_period =   // if the period is far below the minimum, we apply a penalty
                 (this.message_period < pivot_period ?
                     pivot_period + 1000 * (long) Math.pow((pivot_period - this.message_period) / 1000, 3) :
                this.message_period);
        long waitingtime = Math.min(DAY_MILLIS, (long) (ttl_factor * RETRIEVAL_CONSTANT * strategic_period));
        this.retrieval_next = new Date(this.retrieval_last.getTime() + waitingtime);
    }
    // to check the retrieval order created by the update method, call
    // http://localhost:9000/api/suggest.json?orderby=retrieval_next&order=asc
    
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

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("query", this.query);
        m.put("query_length", this.query_length);
        m.put("source_type", this.source_type.name());
        m.put("timezoneOffset", this.timezoneOffset);
        if (this.query_first != null) m.put("query_first", utcFormatter.print(this.query_first.getTime()));
        if (this.query_last != null) m.put("query_last", utcFormatter.print(this.query_last.getTime()));
        if (this.retrieval_last != null) m.put("retrieval_last", utcFormatter.print(this.retrieval_last.getTime()));
        if (this.retrieval_next != null) m.put("retrieval_next", utcFormatter.print(this.retrieval_next.getTime()));
        if (this.expected_next != null) m.put("expected_next", utcFormatter.print(this.expected_next.getTime()));
        m.put("query_count", this.query_count);
        m.put("retrieval_count", this.retrieval_count);
        m.put("message_period", this.message_period);
        m.put("messages_per_day", this.messages_per_day);
        m.put("score_retrieval", this.score_retrieval);
        m.put("score_suggest", this.score_suggest);
        return m;
    }

    private final static Pattern tokenizerPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // tokenizes Strings into terms respecting quoted parts
    
    private static enum Constraint {
        image("images"),
        audio("audio"),
        video("videos"),
        place("place_name"),
        location("location_point"),
        link("links"),
        mention("mentions"),
        source_type("source_type"),
        hashtag("hashtags");
        protected String field_name;
        protected Pattern pattern;
        private Constraint(String field_name) {
            this.field_name = field_name;
            this.pattern = Pattern.compile("\\s?\\-?/" + this.name() + "\\S*");
        }
    }
    
    
    public static String removeConstraints(String q) {
        for (Constraint c: Constraint.values()) {
            q = c.pattern.matcher(q).replaceAll("");
        }
        return q;
    }
    
    public static Timeline applyConstraint(Timeline tl0, String q) { 
        // tokenize the query
        List<String> qe = new ArrayList<String>();
        Matcher m = tokenizerPattern.matcher(q);
        while (m.find()) qe.add(m.group(1));

        HashSet<String> constraints_positive = new HashSet<>();
        HashSet<String> constraints_negative = new HashSet<>();
        for (String t: qe) {
            if (t.startsWith("/")) constraints_positive.add(t.substring(1));
            if (t.startsWith("-/")) constraints_negative.add(t.substring(2));
        }
        PlaceContext place_context = constraints_positive.remove("about") ? PlaceContext.ABOUT : PlaceContext.FROM;
        if (constraints_negative.remove("about")) place_context = PlaceContext.FROM;
        
        Timeline tl1 = new Timeline(tl0.getOrder());
        messageloop: for (MessageEntry message: tl0) {
            if (constraints_positive.contains("image") && message.getImages().size() == 0) continue;
            if (constraints_negative.contains("image") && message.getImages().size() != 0) continue;
            if (constraints_positive.contains("place") && message.getPlaceName().length() == 0) continue;
            if (constraints_negative.contains("place") && message.getPlaceName().length() != 0) continue;
            if (constraints_positive.contains("location") && (message.getLocationPoint() == null || message.getPlaceContext() != place_context)) continue;
            if (constraints_negative.contains("location") && message.getLocationPoint() != null) continue;
            if (constraints_positive.contains("link") && message.getLinks().length == 0) continue;
            if (constraints_negative.contains("link") && message.getLinks().length != 0) continue;
            if (constraints_positive.contains("mention") && message.getMentions().length == 0) continue;
            if (constraints_negative.contains("mention") && message.getMentions().length != 0) continue;
            if (constraints_positive.contains("hashtag") && message.getHashtags().length == 0) continue;
            if (constraints_negative.contains("hashtag") && message.getHashtags().length != 0) continue;

            // special treatment of location and link constraint
            constraintCheck: for (String cs: constraints_positive) {
                if (cs.startsWith(Constraint.location.name() + "=")) {
                    if (message.getLocationPoint() == null) continue messageloop;
                    if (message.getPlaceContext() != place_context) continue messageloop;
                    String params = cs.substring(Constraint.location.name().length() + 1);
                    String[] coord = params.split(",");
                    if (coord.length == 4) {
                        double lon = message.getLocationPoint()[0];
                        double lon_west  = Double.parseDouble(coord[0]);
                        double lon_east  = Double.parseDouble(coord[2]);
                        if (lon < lon_west || lon > lon_east) continue messageloop;
                        double lat = message.getLocationPoint()[1];
                        double lat_south = Double.parseDouble(coord[1]);
                        double lat_north = Double.parseDouble(coord[3]);
                        if (lat < lat_south || lat > lat_north) continue messageloop;
                    }
                }
                if (cs.startsWith(Constraint.link.name() + "=")) {
                    if (message.getLinks().length == 0) continue messageloop;
                    Pattern regex = Pattern.compile(cs.substring(Constraint.link.name().length() + 1));
                    for (String link: message.getLinks()) {
                        if (regex.matcher(link).matches()) continue constraintCheck;
                    }
                    // no match
                    continue messageloop;
                }
            }
            
            tl1.addTweet(message);
        }
        return tl1;
    }
    
    private final static Pattern term4ORPattern = Pattern.compile("(?:^| )(\\S*(?: OR \\S*)+)(?: |$)"); // Pattern.compile("(^\\s*(?: OR ^\\s*+)+)");
    
    private static List<String> splitIntoORGroups(String q) {
        // detect usage of OR junctor usage. Right now we cannot have mixed AND and OR usage. Thats a hack right now
        q = q.replaceAll(" AND ", " "); // AND is default
        
        // tokenize the query
        ArrayList<String> list = new ArrayList<>();
        Matcher m = term4ORPattern.matcher(q);
        while (m.find()) {
            String d = m.group(1);
            q = q.replace(d, "").replace("  ", " ");
            list.add(d);
            m = term4ORPattern.matcher(q);
        }
        q = q.trim();
        if (q.length() > 0) list.add(0, q);
        return list;
    }
    
    public static void main(String[] args) {
        splitIntoORGroups("Alpha OR Beta AND Gamma /constraint sand OR kies OR wasser skilanglauf");
    }
    
    public static class ElasticsearchQuery {
        
        QueryBuilder queryBuilder;
        Date since, until;

        public ElasticsearchQuery(String q, int timezoneOffset) {
            // default values for since and util
            this.since = new Date(0);
            this.until = new Date(Long.MAX_VALUE);
            // parse the query
            this.queryBuilder = preparse(q, timezoneOffset);
        }

        private QueryBuilder preparse(String q, int timezoneOffset) {
            // detect usage of OR connector usage.
            q = q.replaceAll(" AND ", " "); // AND is default
            List<String> terms = splitIntoORGroups(q);
            if (terms.size() == 0) return QueryBuilders.matchAllQuery();
            if (terms.size() == 1) return parse(terms.get(0), timezoneOffset);

            BoolQueryBuilder aquery = QueryBuilders.boolQuery();
            for (String t: terms) {
                aquery.must(parse(t, timezoneOffset));
            }
            return aquery;
        }
        
        private QueryBuilder parse(String q, int timezoneOffset) {
            // detect usage of OR ORconnective usage. Because of the preparse step we will have only OR or only AND here.
            q = q.replaceAll(" AND ", " "); // AND is default
            boolean ORconnective = q.indexOf(" OR ") >= 0;
            q = q.replaceAll(" OR ", " "); // if we know that all terms are OR, we remove that and apply it later
            
            // tokenize the query
            Set<String> qe = new LinkedHashSet<String>();
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
            // additional constraints:
            //   /image /audio /video /place - restrict to tweets which have attached images, audio, video or place
            ArrayList<String> text_positive_match = new ArrayList<>();
            ArrayList<String> text_negative_match = new ArrayList<>();
            ArrayList<String> text_positive_filter = new ArrayList<>();
            ArrayList<String> text_negative_filter = new ArrayList<>();
            ArrayList<String> users_positive = new ArrayList<>();
            ArrayList<String> users_negative = new ArrayList<>();
            ArrayList<String> hashtags_positive = new ArrayList<>();
            ArrayList<String> hashtags_negative = new ArrayList<>();
            HashMap<String, String> modifier = new HashMap<>();
            HashSet<String> constraints_positive = new HashSet<>();
            HashSet<String> constraints_negative = new HashSet<>();
            for (String t: qe) {
                if (t.length() == 0) continue;
                if (t.startsWith("@")) {
                    users_positive.add(t.substring(1));
                    continue;
                } else if (t.startsWith("-@")) {
                    users_negative.add(t.substring(2));
                    continue;
                } else if (t.startsWith("#")) {
                    hashtags_positive.add(t.substring(1));
                    continue;
                } else if (t.startsWith("-#")) {
                    hashtags_negative.add(t.substring(2));
                    continue;
                } else if (t.startsWith("/")) {
                    constraints_positive.add(t.substring(1));
                    continue;
                } else if (t.startsWith("-/")) {
                    constraints_negative.add(t.substring(2));
                    continue;
                } else if (t.indexOf(':') > 0) {
                    int p = t.indexOf(':');
                    modifier.put(t.substring(0, p).toLowerCase(), t.substring(p + 1));
                    continue;
                } else {
                    // patch characters that will confuse elasticsearch or have a different meaning
                    boolean negative = t.startsWith("-");
                    if (negative) t = t.substring(1);
                    if (t.length() == 0) continue;
                    if ((t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') || (t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'')) {
                        t = t.substring(1, t.length() - 1);
                        if (negative) text_negative_filter.add(t); else text_positive_filter.add(t);
                    } else if (t.indexOf('-') > 0) {
                        // this must be handled like a quoted string without the minus
                        t = t.replaceAll("-", " ");
                        if (negative) text_negative_filter.add(t); else text_positive_filter.add(t);
                    } else {
                        if (negative) text_negative_match.add(t); else text_positive_match.add(t);
                    }
                    continue;
                }
            }
            if (modifier.containsKey("to")) users_positive.add(modifier.get("to"));
            if (modifier.containsKey("-to")) users_negative.add(modifier.get("-to"));

            // special constraints
            boolean constraint_about = constraints_positive.remove("about");
            if (constraints_negative.remove("about")) constraint_about = false;
            
            // compose query for text
            List<QueryBuilder> ops = new ArrayList<>();
            List<QueryBuilder> nops = new ArrayList<>();
            for (String text: text_positive_match)  {
                ops.add(QueryBuilders.matchQuery("text", text));
            }
            for (String text: text_negative_match) {
                // negation of terms in disjunctions would cause to retrieve almost all documents
                // this cannot be the requirement of the user. It may be valid in conjunctions, but not in disjunctions
                nops.add(QueryBuilders.matchQuery("text", text));
            }
            
            // apply modifiers
            if (modifier.containsKey("id")) {
                ops.add(QueryBuilders.termQuery("id_str", modifier.get("id")));
            }
            if (modifier.containsKey("-id")) nops.add(QueryBuilders.termQuery("id_str", modifier.get("-id")));

            for (String user: users_positive) {
                ops.add(QueryBuilders.termQuery("mentions", user));
            }
            for (String user: users_negative) nops.add(QueryBuilders.termQuery("mentions", user));
            
            for (String hashtag: hashtags_positive) {
                ops.add(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            }
            for (String hashtag: hashtags_negative) nops.add(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            
            if (modifier.containsKey("from")) {
                String screen_name = modifier.get("from");
                if (screen_name.indexOf(',') < 0) {
                    ops.add(QueryBuilders.termQuery("screen_name", screen_name));
                } else {
                    String[] screen_names = screen_name.split(",");
                    BoolQueryBuilder disjunction = QueryBuilders.boolQuery();
                    for (String name: screen_names) disjunction.should(QueryBuilders.termQuery("screen_name", name));
                    ops.add(disjunction);
                }
            }
            if (modifier.containsKey("-from")) {
                String screen_name = modifier.get("-from");
                if (screen_name.indexOf(',') < 0) {
                    nops.add(QueryBuilders.termQuery("screen_name", screen_name));
                } else {
                    String[] screen_names = screen_name.split(",");
                    for (String name: screen_names) nops.add(QueryBuilders.termQuery("screen_name", name));
                }
            }
            if (modifier.containsKey("near")) {
                BoolQueryBuilder nearquery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("place_context", PlaceContext.FROM.name()))
                        .should(QueryBuilders.matchQuery("place_name", modifier.get("near")))
                        .should(QueryBuilders.matchQuery("text", modifier.get("near")));
                if (ORconnective) ops.add(nearquery);
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
                ops.add(rangeQuery);
            } catch (ParseException e) {} else if (modifier.containsKey("until")) try {
                Calendar until = DateParser.parse(modifier.get("until"), timezoneOffset);
                if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                    // until must be the day which is included in results.
                    // To get the result within the same day, we must add one day.
                    until.add(Calendar.DATE, 1);
                }
                this.until = until.getTime();
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").to(this.until);
                ops.add(rangeQuery);
            } catch (ParseException e) {}

            // apply the ops and nops
            QueryBuilder bquery = QueryBuilders.boolQuery();
            if (ops.size() == 1 && nops.size() == 0)
                bquery = ops.iterator().next();
            else if (ops.size() == 0 && nops.size() == 1)
                bquery = QueryBuilders.boolQuery().mustNot(ops.iterator().next());
            else {
                for (QueryBuilder qb: ops) {
                    if (ORconnective) ((BoolQueryBuilder) bquery).should(qb); else ((BoolQueryBuilder) bquery).must(qb);
                }
                for (QueryBuilder nqb: nops) {
                    ((BoolQueryBuilder) bquery).mustNot(nqb);
                }
                
            }
            
            // apply constraints as filters
            List<FilterBuilder> filters = new ArrayList<>();
            for (String text: text_positive_filter) {
                filters.add(FilterBuilders.termsFilter("text", text));
            }
            for (String text: text_negative_filter) filters.add(FilterBuilders.notFilter(FilterBuilders.termsFilter("text", text)));
            for (Constraint c: Constraint.values()) {
                if (constraints_positive.contains(c.name())) {
                    filters.add(FilterBuilders.existsFilter(c.field_name));
                }
                if (constraints_negative.contains(c.name())) {
                    filters.add(FilterBuilders.notFilter(FilterBuilders.existsFilter(c.field_name)));
                }
            }
            if (constraints_positive.contains("location")) {
                filters.add(FilterBuilders.termsFilter("place_context", (constraint_about ? PlaceContext.ABOUT : PlaceContext.FROM).name()));
            }
            
            // special treatment of location constraints of the form /location=lon-west,lat-south,lon-east,lat-north i.e. /location=8.58,50.178,8.59,50.181
            for (String cs: constraints_positive) {
                if (cs.startsWith(Constraint.location.name() + "=")) {
                    String params = cs.substring(Constraint.location.name().length() + 1);
                    String[] coord = params.split(",");
                    if (coord.length == 1) {
                        filters.add(FilterBuilders.termsFilter("location_source", coord[0]));
                    }
                    else if (coord.length == 4 || coord.length == 5) {
                        double lon_west  = Double.parseDouble(coord[0]);
                        double lat_south = Double.parseDouble(coord[1]);
                        double lon_east  = Double.parseDouble(coord[2]);
                        double lat_north = Double.parseDouble(coord[3]);
                        PlaceContext context = constraint_about ? PlaceContext.ABOUT : PlaceContext.FROM;
                        filters.add(FilterBuilders.existsFilter(Constraint.location.field_name));
                        filters.add(FilterBuilders.termsFilter("place_context", context.name()));
                        filters.add(FilterBuilders.geoBoundingBoxFilter("location_point")
                                .topLeft(lat_north, lon_west)
                                .bottomRight(lat_south, lon_east));
                        if (coord.length == 5) filters.add(FilterBuilders.termsFilter("location_source", coord[4]));
                    }
                }
                if (cs.startsWith(Constraint.link.name() + "=")) {
                    String regexp = cs.substring(Constraint.link.name().length() + 1);
                    filters.add(FilterBuilders.existsFilter(Constraint.link.field_name));
                    filters.add(FilterBuilders.regexpFilter(Constraint.link.field_name, regexp));
                }
            }

            QueryBuilder cquery = filters.size() == 0 ? bquery : QueryBuilders.filteredQuery(bquery, FilterBuilders.andFilter(filters.toArray(new FilterBuilder[filters.size()])));
            return cquery;
        }
    }
}
