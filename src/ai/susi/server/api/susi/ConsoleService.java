/**
 *  ConsoleService
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

package ai.susi.server.api.susi;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonPath;
import ai.susi.mind.SusiProcedures;
import ai.susi.mind.SusiThought;
import ai.susi.mind.SusiTransfer;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.HttpClient;
import ai.susi.json.JsonTray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* examples:
 * http://localhost:4000/susi/console.json?q=SELECT%20*%20FROM%20rss%20WHERE%20url=%27https://www.reddit.com/search.rss?q=loklak%27;
 * http://localhost:4000/susi/console.json?q=SELECT%20plaintext%20FROM%20wolframalpha%20WHERE%20query=%27berlin%27;
 * http://localhost:4000/susi/console.json?q=SELECT%20extract%20FROM%20wikipedia%20WHERE%20query=%27tschunk%27%20AND%20language=%27de%27;
 * http://localhost:4000/susi/console.json?q=SELECT%20plaintext%20FROM%20youtubesearch%20WHERE%20query=%27tschunk%27;
 * http://localhost:4000/susi/console.json?q=SELECT%20plaintext%20FROM%20soundcloudsearch%20WHERE%20query=%27tschunk%27;
 */

public class ConsoleService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/console.json";
    }

    public final static SusiProcedures dbAccess = new SusiProcedures();

    public static void addGenericConsole(String serviceName, String serviceURL, String path) {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?" + serviceName + " +?WHERE +?query ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            byte[] b = new byte[0];
            String bs = "";
            try {
                String testquery = matcher.group(2);
                Map<String, String> request_header = new HashMap<>();
                request_header.put("Accept","application/json");
                b = loadDataWithQuery(serviceURL, request_header, testquery);
                bs = new String(b, StandardCharsets.UTF_8);
                JSONArray data = JsonPath.parse(b, path);
                json.setQuery(testquery);
                SusiTransfer transfer = new SusiTransfer(matcher.group(1));
                if (data != null) json.setData(transfer.conclude(data));
                json.setHits(json.getCount());
            } catch (Throwable e) {
                DAO.severe(e);
                DAO.severe(bs);
            }
            return json;
        });
    }

    public static byte[] loadDataWithQuery(String serviceURL, Map<String, String> request_header, String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        int qp = serviceURL.indexOf("$query$");
        String url = qp < 0 ? serviceURL + encodedQuery : serviceURL.substring(0,  qp) + encodedQuery + serviceURL.substring(qp + 7);
        return loadData(url, request_header);
    }

    public static byte[] loadData(String url, Map<String, String> request_header) throws IOException {
        byte[] b = HttpClient.loadGet(url, request_header);

        // check if this is jsonp
        //System.out.println("DEBUG CONSOLE:" + new String(b, StandardCharsets.UTF_8));
        int i = b.length;
        while (i-- > 0) if (b[i] > 32) break;
        if (i > 2) {
            if (b[i] == ';' && b[i - 1] == ')') {
                // end of jsonp, look for start
                int j = 0;
                while (j < i) if (b[j++] == '(') {
                    // found the json prefix. cut out json
                    byte[] c = new byte[i - 1 - j];
                    System.arraycopy(b, j, c, 0, i - 1 - j);
                    return c;
                }
            }
        }
        return b;
    }

    static {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?\\( ??SELECT +?(.*?) ??\\) +?WHERE +?(.*?) ?+IN ?+\\((.*?)\\) ??;?"), (flow, matcher) -> {
            String subquery = matcher.group(2).trim();
            if (!subquery.endsWith(";")) subquery = subquery + ";";
            String filter_name = matcher.group(3);
            JSONArray a0 = dbAccess.inspire("SELECT " + subquery).getData();
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
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?rss +?WHERE +?url ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = RSSReaderService.readRSS(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?wolframalpha +?WHERE +?query ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            try {
                String query = matcher.group(2);
                JsonTray apiKeys = DAO.apiKeys;
                JSONObject publicKeys = apiKeys.getJSONObject("public");
                String appid = DAO.getConfig("wolframalpha.appid", "");
                if (appid.length() == 0 && publicKeys.has("wolframalphaKey")) {
                    appid = publicKeys.getJSONObject("wolframalphaKey").getString("value");
                }
                String serviceURL = "https://api.wolframalpha.com/v2/query?input=$query$&format=plaintext&output=JSON&appid=" + appid;
                Map<String, String> request_header = new HashMap<>();
                request_header.put("Accept","application/json");
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(loadDataWithQuery(serviceURL, request_header, query)));
                JSONObject wa = new JSONObject(serviceResponse);
                JSONArray pods = wa.getJSONObject("queryresult").getJSONArray("pods");
                // get the relevant pod
                JSONObject subpod = pods.getJSONObject(1).getJSONArray("subpods").getJSONObject(0);
                String response = subpod.getString("plaintext");
                int p = response.indexOf('\n');
                if (p >= 0) response = response.substring(0, p);
                p = response.lastIndexOf('|');
                if (p >= 0) response = response.substring(p + 1).trim();
                subpod.put("plaintext", response);
                json.setQuery(query);
                SusiTransfer transfer = new SusiTransfer(matcher.group(1));
                json.setData(transfer.conclude(new JSONArray().put(subpod)));
                json.setHits(json.getCount());
            } catch (Throwable e) {
                // probably a time-out or a json error
                DAO.severe(e);
            }
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?wikipedia +?WHERE +?query ??= ??'(.*?)' +?AND +?language ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            try {
                String query = matcher.group(2);
                String language = matcher.group(3);
                String serviceURL = "https://" + language + ".wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exlimit=max&explaintext&exintro&titles=$query$&redirects=true";
                Map<String, String> request_header = new HashMap<>();
                request_header.put("Accept","application/json");
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(loadDataWithQuery(serviceURL, request_header, query)));
                JSONObject w = new JSONObject(serviceResponse);
                JSONObject q = w.has("query") ? w.getJSONObject("query") : null;
                JSONObject p = q != null && q.has("pages") ? q.getJSONObject("pages") : null;
                JSONObject c = p != null && p.length() > 0 ? p.getJSONObject(p.keys().next()) : null;
                String extract = c != null && c.has("extract") ? c.getString("extract") : null;
                int r = extract == null ? -1 : extract.indexOf('.');
                if (r >= 0 && extract.substring(r - 4, r).equals("Corp")) r = extract.indexOf('.', r + 1);
                extract = extract == null || r < 0 ? null : extract.substring(0, r + 1).replaceAll("\\(.*\\) ", "").replaceAll("\\[.*\\] ", "");
                json.setQuery(query);
                if (extract != null) json.setData(new JSONArray().put(new JSONObject().put("extract", extract)));
                json.setHits(extract == null ? 0 : 1);
            } catch (Throwable e) {
                DAO.severe(e);
                // probably a time-out or a json error
            }
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?youtubesearch +?WHERE +?query ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            Pattern videoPattern = Pattern.compile("\"/watch\\?v=.*? aria-describedby");
            Pattern keyPattern = Pattern.compile("\"/watch\\?v=(.*?)\"");
            Pattern titlePattern = Pattern.compile("title=\"(.*?)\"");
            try {
                String query = matcher.group(2);
                String serviceURL = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8");
                String s = new String(HttpClient.loadGet(serviceURL), "UTF-8");
                JSONArray a = new JSONArray();
                //System.out.println(s);
                Matcher m = videoPattern.matcher(s);
                while (m.find()) {
                    String fragment = m.group(0);
                    Matcher keyMatcher = keyPattern.matcher(fragment);
                    JSONObject j = null;
                    if (keyMatcher.find()) {
                        String key = keyMatcher.group(1);
                        if (key.indexOf('&') < 0) {
                            Matcher titleMatcher = titlePattern.matcher(fragment);
                            if (titleMatcher.find()) {
                                String title = titleMatcher.group(1);
                                j = new JSONObject(true);
                                j.put("title", title);
                                j.put("youtube", key);
                            }
                        }
                    }
                    if (j != null) a.put(j);
                }
                json.setQuery(query);
                json.setData(a);
            } catch (Throwable e) {
                DAO.severe(e);
                // probably a time-out or a json error
            }
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?soundcloudsearch +?WHERE +?query ??= ??'(.*?)' ??;?"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            Pattern videoPattern = Pattern.compile("<li><h2><a href=\"(.*?)\">(.*?)</a></h2></li>");
            try {
                String query = matcher.group(2);
                String serviceURL = "https://soundcloud.com/search?q=" + URLEncoder.encode(query, "UTF-8");
                String s = new String(HttpClient.loadGet(serviceURL), "UTF-8");
                DAO.log("loaded " + s.length() + " bytes from soundcloud");
                JSONArray a = new JSONArray();
                //System.out.println(s);
                Matcher m = videoPattern.matcher(s);
                while (m.find()) {
                    String path = m.group(1);
                    String title = m.group(2);
                    JSONObject j = new JSONObject(true);
                    j.put("title", title);
                    j.put("soundcloud", path);
                    a.put(j);
                }
                json.setQuery(query);
                json.setData(a);
            } catch (Throwable e) {
                DAO.severe(e);
                // probably a time-out or a json error
            }
            return json;
        });
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        DAO.observe(); // get a database update
        return new ServiceResponse(dbAccess.inspire(q).toJSON());
    }

}
