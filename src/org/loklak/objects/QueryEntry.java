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

package org.loklak.objects;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.data.Classifier;
import org.loklak.data.DAO;
import org.loklak.geo.GeoLocation;
import org.loklak.geo.GeoMark;
import org.loklak.harvester.SourceType;
import org.loklak.tools.DateParser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
    
    protected String query;           // the query in the exact way as the user typed it in
    protected int query_length;       // the length in the query, number of characters
    public SourceType source_type; // the (external) retrieval system where that query was submitted
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
     * @param message_period
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

    public QueryEntry(JSONObject json) throws IllegalArgumentException, JSONException {
        init(json);
    }

    public void init(JSONObject json) throws IllegalArgumentException, JSONException {
        this.query = (String) json.get("query");
        this.query_length = (int) parseLong((Number) json.get("query_length"));
        String source_type_string = (String) json.get("source_type");
        if (source_type_string == null) source_type_string = SourceType.USER.name();
        this.source_type = SourceType.valueOf(source_type_string);
        this.timezoneOffset = (int) parseLong((Number) json.get("timezoneOffset"));
        Date now = new Date();
        this.query_first = parseDate(json.get("query_first"), now);
        this.query_last = parseDate(json.get("query_last"), now);
        this.retrieval_last = parseDate(json.get("retrieval_last"), now);
        this.retrieval_next = parseDate(json.get("retrieval_next"), now);
        this.expected_next = parseDate(json.get("expected_next"), now);
        this.query_count = (int) parseLong((Number) json.get("query_count"));
        this.retrieval_count = (int) parseLong((Number) json.get("retrieval_count"));
        this.message_period = parseLong((Number) json.get("message_period"));
        this.messages_per_day = (int) parseLong((Number) json.get("messages_per_day"));
        this.score_retrieval = (int) parseLong((Number) json.get("score_retrieval"));
        this.score_suggest = (int) parseLong((Number) json.get("score_suggest"));
    }
    
    /**
     * update the query entry
     * @param message_period 
     * @param byUserQuery is true, if the query was submitted by the user; false if the query was submitted by an automatic system
     */
    public void update(final long message_period, final boolean byUserQuery) {
        // message_period may have the value Long.MAX_VALUE if search requests have been empty and a message period cannot be computed
        this.retrieval_last = new Date();
        this.retrieval_count++;
        if (byUserQuery) {
            this.query_count++;
            this.query_last = this.retrieval_last;
        }
        long new_message_period = message_period; // can be Long.MAX_VALUE if less than 2 messages are in timeline!
        int new_messages_per_day = (int) (DAY_MILLIS / new_message_period); // this is an interpolation based on the last tweet list, can be 0!
        if (new_message_period == Long.MAX_VALUE || new_messages_per_day == 0) {
            this.message_period = DAY_MILLIS;
        } else {
            this.message_period = this.message_period == 0 ? new_message_period : (this.message_period + new_message_period) / 2;
        }
        this.messages_per_day = (int) (DAY_MILLIS / this.message_period);
        double ttl_factor = DAO.getConfig("retrieval.queries.ttlfactor", 0.75d);
        long pivot_period = DAO.getConfig("retrieval.queries.pivotfrequency", 10000);
        this.expected_next = new Date(this.retrieval_last.getTime() + ((long) (ttl_factor *  this.message_period)));
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
    public JSONObject toJSON() {
        JSONObject m = new JSONObject();
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
        hashtag("hashtags"),
        emotion("classifier_emotion"),
        profanity("classifier_profanity"),
        language("classifier_language");
        protected String field_name;
        protected Pattern pattern;
        private Constraint(String field_name) {
            this.field_name = field_name;
            this.pattern = Pattern.compile("\\s?\\-?/" + this.name() + "\\S*");
        }
    }
    
    public static class Tokens {
        
        public final String original;
        public String raw;
        public final HashSet<String> constraints_positive, constraints_negative;
        public Multimap<String, String> modifier;
        public PlaceContext place_context;
        public double[] bbox; // double[]{lon_west,lat_south,lon_east,lat_north}
        
        public Tokens(final String q) {
            this.original = q;
            List<String> tokens = new ArrayList<String>();
            Matcher m = tokenizerPattern.matcher(q);
            while (m.find()) tokens.add(m.group(1));

            this.constraints_positive = new HashSet<>();
            this.constraints_negative = new HashSet<>();
            this.modifier = HashMultimap.create();
            StringBuilder rawb = new StringBuilder(q.length() + 1);
            Set<String> hashtags = new HashSet<>();
            for (String t: tokens) {
                if (t.startsWith("/")) {
                    constraints_positive.add(t.substring(1));
                    continue;
                } else if (t.startsWith("-/")) {
                    constraints_negative.add(t.substring(2));
                    continue;
                } else if (t.indexOf(':') > 0) {
                    int p = t.indexOf(':');
                    modifier.put(t.substring(0, p).toLowerCase(), t.substring(p + 1));
                    rawb.append(t).append(' ');
                    continue;
                } else {
                    if (t.startsWith("#")) hashtags.add(t.substring(1));
                    rawb.append(t).append(' ');
                }
            }
            this.place_context = this.constraints_positive.remove("about") ? PlaceContext.ABOUT : PlaceContext.FROM;
            if (this.constraints_negative.remove("about")) this.place_context = PlaceContext.FROM;
            if (rawb.length() > 0 && rawb.charAt(rawb.length() - 1) == ' ') rawb.setLength(rawb.length() - 1);
            this.raw = rawb.toString();
            // fix common mistake using hashtags in combination with their words without hashtag
            for (String h: hashtags) {
                int p = this.raw.indexOf(h + " #" + h);
                if (p >= 0) this.raw = this.raw.substring(0,  p) + h + " OR #" + h + this.raw.substring(p + h.length() * 2 + 2);
                p = this.raw.indexOf("#" + h + " " + h);
                if (p >= 0) this.raw = this.raw.substring(0,  p) + "#" + h + " OR " + h + this.raw.substring(p + h.length() * 2 + 2);
            }
            
            // find bbox
            this.bbox = null;
            bboxsearch: for (String cs: this.constraints_positive) {
                if (cs.startsWith(Constraint.location.name() + "=")) {
                    String params = cs.substring(Constraint.location.name().length() + 1);
                    String[] coord = params.split(",");
                    if (coord.length == 4) {
                        this.bbox = new double[4];
                        for (int i = 0; i < 4; i++) this.bbox[i] = Double.parseDouble(coord[i]);
                        break bboxsearch;
                    }
                }
            }
            
            if (modifier.containsKey("near")) {
                // either check coordinates or name
                String near_name = modifier.get("near").iterator().next();
                GeoMark loc = DAO.geoNames.analyse(near_name, null, 10, Long.toString(System.currentTimeMillis()));
                if (loc != null) {
                    this.bbox = new double[]{loc.lon() - 1.0, loc.lat() + 1.0, loc.lon() + 1.0, loc.lat() - 1.0};
                }
            }
        }
        
        public String translate4scraper() {
            // check if a location constraint was given
            if (this.bbox == null || this.original.indexOf("near:") > 0) {
                return this.raw;
            }
            // find place within the bbox
            double lon_west  = this.bbox[0];
            double lat_south = this.bbox[1];
            double lon_east  = this.bbox[2];
            double lat_north = this.bbox[3];
            assert lon_west < lon_east;
            assert lat_north > lat_south;
            // find largest city around to compute a 'near:' operator for twitter
            double lon_km = 40000 / 360 * (lon_east - lon_west);
            double lat_km = 40000 / 360 * (lat_north- lat_south);
            double within_km = Math.max(2.0, Math.max(lon_km, lat_km) / 2);
            double lon_border = (lon_east - lon_west) / 3;
            double lat_border = (lat_north - lat_south) / 3;
            GeoLocation largestCity = DAO.geoNames.getLargestCity(lon_west + lon_border, lat_south + lat_border, lon_east - lon_border, lat_north - lat_border);
            if (largestCity == null) largestCity = DAO.geoNames.getLargestCity(lon_west, lat_south, lon_east, lat_north);
            if (largestCity == null) largestCity = DAO.geoNames.cityNear((lat_north + lat_south) / 2.0, (lon_east + lon_west) / 2.0);
            String q = this.raw + " near:\"" + largestCity.getNames().iterator().next() + "\" within:" + ((int) (within_km / 1.609344)) + "mi"; // stupid imperial units are stupid
            return q;
        }
    }
    
    public static Timeline applyConstraint(Timeline tl0, Tokens tokens, boolean applyLocationConstraint) {
         if (tokens.constraints_positive.size() == 0 && tokens.constraints_negative.size() == 0 && tokens.modifier.size() == 0) return tl0;
        Timeline tl1 = new Timeline(tl0.getOrder());
        messageloop: for (MessageEntry message: tl0) {

            // check modifier
            if (tokens.modifier.containsKey("from")) {
                for (String screen_name: tokens.modifier.get("from")) {
                    if (!message.getScreenName().equals(screen_name)) continue messageloop;
                }
            }
            if (tokens.modifier.containsKey("-from")) {
                for (String screen_name: tokens.modifier.get("-from")) {
                    if (message.getScreenName().equals(screen_name)) continue messageloop;
                }
            }
            if (applyLocationConstraint && tokens.bbox != null) {
                if (message.location_point == null || message.location_point.length < 2) continue messageloop; //longitude, latitude
                if (message.location_point[0] < tokens.bbox[0] || message.location_point[0] > tokens.bbox[2] ||  // double[]{lon_west,lat_south,lon_east,lat_north}
                    message.location_point[1] > tokens.bbox[1] || message.location_point[1] < tokens.bbox[3]) continue messageloop;
            }
            
            // check constraints
            if (tokens.constraints_positive.contains("image") && message.getImages().size() == 0) continue;
            if (tokens.constraints_negative.contains("image") && message.getImages().size() != 0) continue;
            if (tokens.constraints_positive.contains("place") && message.getPlaceName().length() == 0) continue;
            if (tokens.constraints_negative.contains("place") && message.getPlaceName().length() != 0) continue;
            if (tokens.constraints_positive.contains("location") && (message.getLocationPoint() == null || message.getPlaceContext() != tokens.place_context)) continue;
            if (tokens.constraints_negative.contains("location") && message.getLocationPoint() != null) continue;
            if (tokens.constraints_positive.contains("link") && message.getLinks().length == 0) continue;
            if (tokens.constraints_negative.contains("link") && message.getLinks().length != 0) continue;
            if (tokens.constraints_positive.contains("mention") && message.getMentions().length == 0) continue;
            if (tokens.constraints_negative.contains("mention") && message.getMentions().length != 0) continue;
            if (tokens.constraints_positive.contains("hashtag") && message.getHashtags().length == 0) continue;
            if (tokens.constraints_negative.contains("hashtag") && message.getHashtags().length != 0) continue;
            for (Classifier.Context context: Classifier.Context.values()) {
                if (tokens.constraints_positive.contains(context.name()) && message.getClassifier(context) == null) continue messageloop;
                if (tokens.constraints_negative.contains(context.name()) && message.getClassifier(context) != null) continue messageloop;
            }
            
            // special treatment of location and link constraint
            constraintCheck: for (String cs: tokens.constraints_positive) {
                if (cs.startsWith(Constraint.location.name() + "=")) {
                    if (message.getLocationPoint() == null) continue messageloop;
                    if (message.getPlaceContext() != tokens.place_context) continue messageloop;
                    String params = cs.substring(Constraint.location.name().length() + 1);
                    String[] coord = params.split(",");
                    if (coord.length == 4) {
                        double lon = message.getLocationPoint()[0];
                        double lon_west  = Double.parseDouble(coord[0]);
                        double lon_east  = Double.parseDouble(coord[2]);
                        assert lon_west < lon_east;
                        if (lon < lon_west || lon > lon_east) continue messageloop;
                        double lat = message.getLocationPoint()[1];
                        double lat_south = Double.parseDouble(coord[1]);
                        double lat_north = Double.parseDouble(coord[3]);
                        assert lat_north > lat_south;
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
            
            tl1.add(message, tl0.getUser(message.getScreenName()));
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
        
        public QueryBuilder queryBuilder;
        public Date since;
        public Date until;

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
            List<String> terms = splitIntoORGroups(q); // OR binds stronger than AND
            if (terms.size() == 0) return QueryBuilders.matchAllQuery();
            
            // special handling
            if (terms.size() == 1) return parse(terms.get(0), timezoneOffset);

            // generic handling
            BoolQueryBuilder aquery = QueryBuilders.boolQuery();
            for (String t: terms) {
                QueryBuilder partial = parse(t, timezoneOffset);
                aquery.must(partial);
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
            Multimap<String, String> modifier = HashMultimap.create();
            Set<String> constraints_positive = new HashSet<>();
            Set<String> constraints_negative = new HashSet<>();
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
            if (modifier.containsKey("to")) users_positive.addAll(modifier.get("to"));
            if (modifier.containsKey("-to")) users_negative.addAll(modifier.get("-to"));

            // special constraints
            boolean constraint_about = constraints_positive.remove("about");
            if (constraints_negative.remove("about")) constraint_about = false;
            
            // compose query for text
            List<QueryBuilder> ops = new ArrayList<>();
            List<QueryBuilder> nops = new ArrayList<>();
            List<QueryBuilder> filters = new ArrayList<>();
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
                ops.add(QueryBuilders.termsQuery("id_str", modifier.get("id")));
            }
            if (modifier.containsKey("-id")) {
                nops.add(QueryBuilders.termsQuery("id_str", modifier.get("-id")));
            }

            for (String user: users_positive) {
                ops.add(QueryBuilders.termQuery("mentions", user));
            }
            for (String user: users_negative) nops.add(QueryBuilders.termQuery("mentions", user));
            
            for (String hashtag: hashtags_positive) {
                ops.add(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            }
            for (String hashtag: hashtags_negative) nops.add(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase()));
            
            if (modifier.containsKey("from")) {
                for (String screen_name: modifier.get("from")) {
                    if (screen_name.indexOf(',') < 0) {
                        ops.add(QueryBuilders.termQuery("screen_name", screen_name));
                    } else {
                        String[] screen_names = screen_name.split(",");
                        BoolQueryBuilder disjunction = QueryBuilders.boolQuery();
                        for (String name: screen_names) disjunction.should(QueryBuilders.termQuery("screen_name", name));
                        disjunction.minimumNumberShouldMatch(1);
                        ops.add(disjunction);
                    }
                }
            }
            if (modifier.containsKey("-from")) {
                for (String screen_name: modifier.get("-from")) {
                    if (screen_name.indexOf(',') < 0) {
                        nops.add(QueryBuilders.termQuery("screen_name", screen_name));
                    } else {
                        String[] screen_names = screen_name.split(",");
                        for (String name: screen_names) nops.add(QueryBuilders.termQuery("screen_name", name));
                    }
                }
            }
            if (modifier.containsKey("near")) {
                // either check coordinates or name
                String near_name = modifier.get("near").iterator().next();
                GeoMark loc = DAO.geoNames.analyse(near_name, null, 10, Long.toString(System.currentTimeMillis()));
                if (loc == null) {
                    BoolQueryBuilder nearquery = QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("place_name", near_name))
                        .should(QueryBuilders.matchQuery("text", near_name));
                    nearquery.minimumNumberShouldMatch(1);
                    ops.add(QueryBuilders.boolQuery().must(nearquery).must(QueryBuilders.matchQuery("place_context", PlaceContext.FROM.name())));
                } else {                    
                    filters.add(QueryBuilders.geoDistanceQuery("location_point").distance(100.0, DistanceUnit.KILOMETERS).lat(loc.lat()).lon(loc.lon()));
                }
            }
            if (modifier.containsKey("since")) try {
                Calendar since = DateParser.parse(modifier.get("since").iterator().next(), timezoneOffset);
                this.since = since.getTime();
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").from(this.since);
                if (modifier.containsKey("until")) {
                    Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
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
                Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
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
                if (ORconnective) ((BoolQueryBuilder) bquery).minimumNumberShouldMatch(1);
                for (QueryBuilder nqb: nops) {
                    ((BoolQueryBuilder) bquery).mustNot(nqb);
                }
                
            }
            
            // apply constraints as filters
            for (String text: text_positive_filter) {
                filters.add(QueryBuilders.termsQuery("text", text));
            }
            for (String text: text_negative_filter) filters.add(QueryBuilders.notQuery(QueryBuilders.termsQuery("text", text)));
            for (Constraint c: Constraint.values()) {
                if (constraints_positive.contains(c.name())) {
                    filters.add(QueryBuilders.existsQuery(c.field_name));
                }
                if (constraints_negative.contains(c.name())) {
                    filters.add(QueryBuilders.notQuery(QueryBuilders.existsQuery(c.field_name)));
                }
            }
            if (constraints_positive.contains("location")) {
                filters.add(QueryBuilders.termsQuery("place_context", (constraint_about ? PlaceContext.ABOUT : PlaceContext.FROM).name()));
            }

            // special treatment of location constraints of the form /location=lon-west,lat-south,lon-east,lat-north i.e. /location=8.58,50.178,8.59,50.181
            //                      source_type constraint of the form /source_type=FOSSASIA_API -> search exact term (source_type must exists in SourceType enum)
            for (String cs: constraints_positive) {
                if (cs.startsWith(Constraint.location.name() + "=")) {
                    String params = cs.substring(Constraint.location.name().length() + 1);
                    String[] coord = params.split(",");
                    if (coord.length == 1) {
                        filters.add(QueryBuilders.termsQuery("location_source", coord[0]));
                    } else if (coord.length == 2) {
                        double lon = Double.parseDouble(coord[0]);
                        double lat = Double.parseDouble(coord[1]);
                        // ugly way to search exact geo_point : using geoboundingboxfilter, with two identical bounding points
                        // geoshape filter can search for exact point shape but it can't be performed on geo_point field
                        filters.add(QueryBuilders.geoBoundingBoxQuery("location_point")
                                .topLeft(lat, lon)
                                .bottomRight(lat, lon));
                    }
                    else if (coord.length == 4 || coord.length == 5) {
                        double lon_west  = Double.parseDouble(coord[0]);
                        double lat_south = Double.parseDouble(coord[1]);
                        double lon_east  = Double.parseDouble(coord[2]);
                        double lat_north = Double.parseDouble(coord[3]);
                        PlaceContext context = constraint_about ? PlaceContext.ABOUT : PlaceContext.FROM;
                        filters.add(QueryBuilders.existsQuery(Constraint.location.field_name));
                        filters.add(QueryBuilders.termsQuery("place_context", context.name()));
                        filters.add(QueryBuilders.geoBoundingBoxQuery("location_point")
                                .topLeft(lat_north, lon_west)
                                .bottomRight(lat_south, lon_east));
                        if (coord.length == 5) filters.add(QueryBuilders.termsQuery("location_source", coord[4]));
                    }
                } else if (cs.startsWith(Constraint.link.name() + "=")) {
                    String regexp = cs.substring(Constraint.link.name().length() + 1);
                    filters.add(QueryBuilders.existsQuery(Constraint.link.field_name));
                    filters.add(QueryBuilders.regexpQuery(Constraint.link.field_name, regexp));
                } else if (cs.startsWith(Constraint.source_type.name() + "=")) {
                    String regexp = cs.substring(Constraint.source_type.name().length() + 1);
                    if (SourceType.hasValue(regexp)) {
                        filters.add(QueryBuilders.termQuery("_type", regexp));
                    }
                }
            }

            for (String cs : constraints_negative) {
                if (cs.startsWith(Constraint.source_type.name() + "=")) {
                    String regexp = cs.substring(Constraint.source_type.name().length() + 1);
                    if (SourceType.hasValue(regexp)) {
                        filters.add(QueryBuilders.notQuery(QueryBuilders.termQuery("_type", regexp)));
                    }

                }
            }

            QueryBuilder cquery = filters.size() == 0 ? bquery : QueryBuilders.filteredQuery(bquery, QueryBuilders.andQuery(filters.toArray(new QueryBuilder[filters.size()])));
            return cquery;
        }
    }
    
    public static enum PlaceContext {
        
        FROM,  // the message was made at that place
        ABOUT; // the message is about that place
        
    }

}
