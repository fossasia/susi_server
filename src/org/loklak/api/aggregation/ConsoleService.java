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

package org.loklak.api.aggregation;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.http.ClientConnection;
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

import javax.servlet.http.HttpServletResponse;

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
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20timeanddate;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20githubProfile%20WHERE%20profile=%27torvalds%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20locationwisetime%20WHERE%20query=%27london%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20instagramprofile%20WHERE%20profile=%27justinpjtrudeau%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20wikigeodata%20WHERE%20place=%27Singapore%27;
 * http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20quoraprofile%20WHERE%20profile=%27justinpjtrudeau%27;

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
        return "/aggregation/console.json";
    }
    
    public final static SusiSkills dbAccess = new SusiSkills();
    
    public static void addGenericConsole(String serviceName, String serviceURL, String responseArrayObjectName) {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?" + serviceName + " +?WHERE +?query ??= ??'(.*?)' ??;"), (flow, matcher) -> {
            JSONObject serviceResponse;
            try {
                ClientConnection cc = new ClientConnection(serviceURL + URLEncoder.encode(matcher.group(2), "UTF-8"));
                serviceResponse = new JSONObject(new JSONTokener(cc.inputStream));
                cc.close();
            } catch (IOException | JSONException e) {serviceResponse = new JSONObject();}
            SusiThought json = new SusiThought();
            json.setQuery(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(serviceResponse.getJSONArray(responseArrayObjectName)));
            json.setHits(json.getCount());
            return json;
        });
    }
    
    static {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?\\( ??SELECT +?(.*?) ??\\) +?WHERE +?(.*?) ?+IN ?+\\((.*?)\\) ??;"), (flow, matcher) -> {
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
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?rss +?WHERE +?url ??= ??'(.*?)' ??;"), (flow, matcher) -> {
            SusiThought json = RSSReaderService.readRSS(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
    }
    
    
    
    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        
        return dbAccess.inspire(q);
    }
    
}
