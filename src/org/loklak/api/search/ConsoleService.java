/**
 *  TableService
 *  Copyright 13.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.search;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.data.DAO;
import org.loklak.geo.GeoMark;
import org.loklak.http.ClientConnection;
import org.loklak.objects.AccountEntry;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.ResultList;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.susi.SusiSkills;
import org.loklak.susi.SusiThought;
import org.loklak.susi.SusiTransfer;

import org.loklak.tools.storage.JSONObjectWithDefault;

/* examples:
 * http://localhost:9000/api/console.json?q=SELECT%20text,%20screen_name,%20user.name%20AS%20user%20FROM%20messages%20WHERE%20query=%271%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 * http://localhost:9000/api/console.json?q=SELECT%20link,screen_name%20FROM%20messages%20WHERE%20id=%27742384468560912386%27;
 * http://localhost:9000/api/console.json?q=SELECT%20COUNT(*)%20AS%20count,%20screen_name%20AS%20twitterer%20FROM%20messages%20WHERE%20query=%27loklak%27%20GROUP%20BY%20screen_name;
 * http://localhost:9000/api/console.json?q=SELECT%20PERCENT(count)%20AS%20percent,%20screen_name%20FROM%20(SELECT%20COUNT(*)%20AS%20count,%20screen_name%20FROM%20messages%20WHERE%20query=%27loklak%27%20GROUP%20BY%20screen_name)%20WHERE%20screen_name%20IN%20(%27leonmakk%27,%27Daminisatya%27,%27sudheesh001%27,%27shiven_mian%27);
 * http://localhost:9000/api/console.json?q=SELECT%20query,%20query_count%20AS%20count%20FROM%20queries%20WHERE%20query=%27auto%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20users%20WHERE%20screen_name=%270rb1t3r%27;
 * http://localhost:9000/api/console.json?q=SELECT%20place[0]%20AS%20place,%20population,%20location[0]%20AS%20lon,%20location[1]%20AS%20lat%20FROM%20locations%20WHERE%20location=%27Berlin%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20locations%20WHERE%20location=%2753.1,13.1%27;
 * http://localhost:9000/api/console.json?q=SELECT%20description%20FROM%20wikidata%20WHERE%20query=%27football%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20meetup%20WHERE%20url=%27http://www.meetup.com/Women-Who-Code-Delhi%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20rss%20WHERE%20url=%27https://www.reddit.com/search.rss?q=loklak%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20eventbrite%20WHERE%20url=%27https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153?aff=es2%27;
 * http://localhost:9000/api/console.json?q=SELECT%20definition,example%20FROM%20urbandictionary%20WHERE%20query=%27football%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20wordpress%20WHERE%20url=%27https://jigyasagrover.wordpress.com/%27;
 * */

public class ConsoleService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/api/console.json";
    }
    
    public final static SusiSkills dbAccess = new SusiSkills();
    static {
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?\\(\\h??SELECT\\h+?(.*?)\\h??\\)\\h+?WHERE\\h+?(.*?)\\h?+IN\\h?+\\((.*?)\\)\\h??;"), (flow, matcher) -> {
            String subquery = matcher.group(2).trim();
            if (!subquery.endsWith(";")) subquery = subquery + ";";
            String filter_name = matcher.group(3);
            JSONArray a0 = dbAccess.inspire("SELECT " + subquery).getJSONArray("data");
            JSONArray a1 = new JSONArray();
            Set<String> filter_set = new SusiTransfer(matcher.group(4)).keys();
            a0.forEach(o -> {
                JSONObject j = (JSONObject) o;
                if (j.has(filter_name) && filter_set.contains(j.getString(filter_name))) a1.put(j);
            });
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return new SusiThought()
                    .setOffset(0).setHits(a0.length())
                    .setData(transfer.conclude(a1));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?id\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            JSONObject message = DAO.messages.readJSON(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return message == null ? null : new SusiThought()
                    .setOffset(0).setHits(1)
                    .setData((new JSONArray()).put(transfer.extract(message)));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h?+GROUP\\h?+BY\\h?+(.*?)\\h??;"), (flow, matcher) -> {
            String group = matcher.group(3);
            DAO.SearchLocalMessages messages = new DAO.SearchLocalMessages(matcher.group(2), Timeline.Order.CREATED_AT, 0, 0, 100, group);
            JSONArray array = new JSONArray();
            JSONObject aggregation = messages.getAggregations().getJSONObject(group);
            
            for (String key: aggregation.keySet()) array.put(new JSONObject(true).put(group, key).put("COUNT(*)", aggregation.get(key)));
            SusiThought json = messages.timeline.toSusi(true);
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(array));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            DAO.SearchLocalMessages messages = new DAO.SearchLocalMessages(matcher.group(2), Timeline.Order.CREATED_AT, 0, 100, 0);
            SusiThought json = messages.timeline.toSusi(true);
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?messages\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h+?ORDER BY (.*?)\\h??;"), (flow, matcher) -> {
            DAO.SearchLocalMessages messages = new DAO.SearchLocalMessages(matcher.group(2), Timeline.Order.valueOf(matcher.group(3)), 0, 100, 0);
            SusiThought json = messages.timeline.toSusi(true);
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?queries\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            ResultList<QueryEntry> queries = DAO.SearchLocalQueries(matcher.group(2), 100, "retrieval_next", "date", SortOrder.ASC, null, new Date(), "retrieval_next");
            SusiThought json = queries.toSusi();
            json.setQuery(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?users\\h+?WHERE\\h+?screen_name\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            UserEntry user_entry = DAO.searchLocalUserByScreenName(matcher.group(2));
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            if (user_entry == null) {
                json.setHits(0).setData(new JSONArray());
            } else {
                json.setHits(1).setData(new JSONArray().put(user_entry.toJSON()));
            }
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?accounts\\h+?WHERE\\h+?screen_name\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            AccountEntry account_entry = DAO.searchLocalAccount(matcher.group(2));
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            if (account_entry == null) {
                json.setHits(0).setData(new JSONArray());
            } else {
                json.setHits(1).setData(new JSONArray().put(account_entry.toJSON()));
            }
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?locations\\h+?WHERE\\h+?location\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            GeoMark loc = DAO.geoNames.analyse(matcher.group(2), null, 5, Long.toString(System.currentTimeMillis()));
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            if (loc == null) {
                json.setHits(0).setData(new JSONArray());
            } else {
                json.setHits(1).setData(new JSONArray().put(loc.toJSON(false)));
            }
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            return json.setData(transfer.conclude(json.getJSONArray("data")));
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?wikidata\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            JSONObject wikidata;
            try {
                ClientConnection cc = new ClientConnection("https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&language=en&search=" + URLEncoder.encode(matcher.group(2), "UTF-8"));
                wikidata = new JSONObject(new JSONTokener(cc.inputStream));
                cc.close();
            } catch (IOException | JSONException e) {wikidata = new JSONObject();}
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(wikidata.getJSONArray("search")));
            json.setHits(json.getCount());
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?urbandictionary\\h+?WHERE\\h+?query\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            JSONObject urbandictionary;
            try {
                ClientConnection cc = new ClientConnection("http://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(matcher.group(2), "UTF-8"));
                urbandictionary = new JSONObject(new JSONTokener(cc.inputStream));
                cc.close();
            } catch (IOException | JSONException e) {urbandictionary = new JSONObject();}
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(urbandictionary.getJSONArray("list")));
            json.setHits(json.getCount());
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?meetup\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = MeetupsCrawlerService.crawlMeetups(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?rss\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = RSSReaderService.readRSS(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?eventbrite\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = EventBriteCrawlerService.crawlEventBrite(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?wordpress\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = WordpressCrawlerService.crawlWordpress(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        
        return dbAccess.inspire(q);
    }
    
}
