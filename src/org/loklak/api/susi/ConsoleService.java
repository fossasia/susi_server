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

package org.loklak.api.susi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.susi.SusiProcedures;
import org.loklak.susi.SusiThought;
import org.loklak.susi.SusiTransfer;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.http.HttpServletResponse;

/* examples:
 * http://localhost:4000/susi/console.json?q=SELECT%20*%20FROM%20rss%20WHERE%20url=%27https://www.reddit.com/search.rss?q=loklak%27;
 * http://localhost:4000/susi/console.json?q=SELECT%20plaintext%20FROM%20wolframalpha%20WHERE%20query=%27berlin%27;
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
        return "/susi/console.json";
    }
    
    public final static SusiProcedures dbAccess = new SusiProcedures();
    
    public static void addGenericConsole(String serviceName, String serviceURL, String path) {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?" + serviceName + " +?WHERE +?query ??= ??'(.*?)' ??;"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            try {
                String testquery = matcher.group(2);
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(loadData(serviceURL, testquery)));
                JSONArray data = ConsoleService.parseJSONPath(serviceResponse, path);
                json.setQuery(testquery);
                SusiTransfer transfer = new SusiTransfer(matcher.group(1));
                if (data != null) json.setData(transfer.conclude(data));
                json.setHits(json.getCount());
            } catch (Throwable e) {
                //e.printStackTrace(); // probably a time-out
            }
            return json;
        });
    }
    
    public static byte[] loadData(String serviceURL, String testquery) throws IOException {
        String encodedQuery = URLEncoder.encode(testquery, "UTF-8");
        int qp = serviceURL.indexOf("$query$");
        String url = qp < 0 ? serviceURL + encodedQuery : serviceURL.substring(0,  qp) + encodedQuery + serviceURL.substring(qp + 7);
        return loadData(url);
    }
    
    public static byte[] loadData(String url) throws IOException {
        ClientConnection cc = new ClientConnection(url);
        
        // fully read the input stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int n;
        byte[] buffer = new byte[16384];
        try {while ((n = cc.inputStream.read(buffer, 0, buffer.length)) != -1) baos.write(buffer, 0, n);} catch (IOException e) {}
        baos.flush();
        byte[] b = baos.toByteArray();
        
        // finished, close
        cc.close();
        return b;
    }
    
    /**
     * very simple JSONPath decoder which always creates a JSONArray as result
     * @param tokener contains the parsed JSON
     * @param jsonPath a path as defined by http://goessner.net/articles/JsonPath/
     * @return a JSONArray with the data part of a console query
     */
    public static JSONArray parseJSONPath(JSONTokener tokener, String jsonPath) {
        try {
            if (tokener == null) return null;
            String[] dompath = jsonPath.split("\\.");
            if (dompath == null || dompath.length < 1 || !dompath[0].equals("$")) return null; // wrong syntax of jsonPath
            if (dompath.length == 1) {
                // the tokener contains already the data array
                return new JSONArray(tokener);
            }
            Object decomposition = null;
            for (int domc = 1; domc < dompath.length; domc++) {
                String path = dompath[domc];
                int p = path.indexOf('[');
                if (p < 0) {
                    decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path);
                } else if (p == 0) {
                    int idx = Integer.parseInt(path.substring(1, path.length() - 1));
                    decomposition = ((decomposition == null) ? new JSONArray(tokener) : ((JSONArray) decomposition)).get(idx);
                } else {
                    int idx = Integer.parseInt(path.substring(p + 1, path.length() - 1));
                    path = path.substring(0, p);
                    decomposition = ((decomposition == null) ? new JSONObject(tokener) : ((JSONObject) decomposition)).get(path);
                    decomposition = ((JSONArray) decomposition).get(idx);
                }
            }
            if (decomposition instanceof JSONArray) return (JSONArray) decomposition;
        } catch (JSONException e) {}
        return null;
    }
    
    static {
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?\\( ??SELECT +?(.*?) ??\\) +?WHERE +?(.*?) ?+IN ?+\\((.*?)\\) ??;"), (flow, matcher) -> {
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
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?rss +?WHERE +?url ??= ??'(.*?)' ??;"), (flow, matcher) -> {
            SusiThought json = RSSReaderService.readRSS(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?wolframalpha +?WHERE +?query ??= ??'(.*?)' ??;"), (flow, matcher) -> {
            SusiThought json = new SusiThought();
            try {
                String query = matcher.group(2);
                String appid = DAO.getConfig("wolframalpha.appid", "");
                String serviceURL = "https://api.wolframalpha.com/v2/query?input=$query$&format=plaintext&output=JSON&appid=" + appid;
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(loadData(serviceURL, query)));
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
            }
            return json;
        });
    }

    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        try {
            DAO.susi.observe(); // learn new console skills if there are new one
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dbAccess.inspire(q).toJSON();
    }
    
}
