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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

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
import ai.susi.server.BaseUserRole;
import ai.susi.server.ClientConnection;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import api.external.transit.BahnService;
import api.external.transit.BahnService.NoStationFoundException;

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
            byte[] b = new byte[0];
            try {
                String testquery = matcher.group(2);
                b = loadData(serviceURL, testquery);
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(b));
                JSONArray data = JsonPath.parse(serviceResponse, path);
                json.setQuery(testquery);
                SusiTransfer transfer = new SusiTransfer(matcher.group(1));
                if (data != null) json.setData(transfer.conclude(data));
                json.setHits(json.getCount());
            } catch (Throwable e) {
                DAO.severe(e);
                DAO.severe(new String(b, StandardCharsets.UTF_8));
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
        dbAccess.put(Pattern.compile("SELECT +?(.*?) +?FROM +?bahn +?WHERE +?from ??= ??'(.*?)' +?to ??= ??'(.*?)' ??;"), (flow, matcher) -> {
        	String query = matcher.group(1);
        	String from = matcher.group(2);
        	String to = matcher.group(3);
        	SusiThought json = new SusiThought();
			try {
				json = (new BahnService()).getConnections(from, to);
				SusiTransfer transfer = new SusiTransfer(query);
                json.setData(transfer.conclude(json.getData()));
                return json;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoStationFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return json;
        });
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "");
        //int timezoneOffset = post.get("timezoneOffset", 0);
        try {
            DAO.susi.observe(); // learn new console intents if there are new one
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResponse(dbAccess.inspire(q).toJSON());
    }
    
}
