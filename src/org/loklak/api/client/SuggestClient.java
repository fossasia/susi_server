/**
 *  SuggestClient
 *  Copyright 13.11.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.ResultList;
import org.loklak.tools.DateParser;
import org.loklak.tools.UTF8;

public class SuggestClient {

    public static ResultList<QueryEntry> suggest(
            final String protocolhostportstub,
            final String q,
            final String source,
            final int count,
            final String order,
            final String orderby,
            final int timezoneOffset,
            final String since,
            final String until,
            final String selectby,
            final int random) throws IOException {
        int httpport = (int) DAO.getConfig("port.http", 9000);
        int httpsport = (int) DAO.getConfig("port.https", 9443);
        String peername = (String) DAO.getConfig("peername", "anonymous");
        ResultList<QueryEntry>  rl = new ResultList<QueryEntry>();
        String urlstring = "";
        urlstring = protocolhostportstub + "/api/suggest.json?q=" + URLEncoder.encode(q.replace(' ', '+'), "UTF-8") +
                "&timezoneOffset=" + timezoneOffset +
                "&count=" + count +
                "&source=" + (source == null ? "all" : source) +
                (order == null ? "" : ("&order=" + order)) +
                (orderby == null ? "" : ("&orderby=" + orderby)) +
                (since == null ? "" : ("&since=" + since)) +
                (until == null ? "" : ("&until=" + until)) +
                (selectby == null ? "" : ("&selectby=" + selectby)) +
                (random < 0 ? "" : ("&random=" + random)) +
                "&minified=true" +
                "&port.http=" + httpport +
                "&port.https=" + httpsport +
                "&peername=" + peername;
        byte[] response = ClientConnection.downloadPeer(urlstring);
        if (response == null || response.length == 0) return rl;
        JSONObject json = new JSONObject(UTF8.String(response));
        JSONArray queries = json.has("queries") ? json.getJSONArray("queries") : null;
        if (queries != null) {
            for (Object query_obj: queries) {
                if (query_obj == null) continue;
                QueryEntry qe = new QueryEntry((JSONObject) query_obj);
                rl.add(qe);
            }
        }
        
        Object metadata_obj = json.get("search_metadata");
        if (metadata_obj != null && metadata_obj instanceof Map<?,?>) {
            Integer hits = (Integer) ((JSONObject) metadata_obj).get("hits");
            if (hits != null) rl.setHits(hits.longValue());
        }
        return rl;
    }
    
    public static void main(String[] args) {
        try {
            ResultList<QueryEntry> rl = suggest("http://loklak.org", "","query",1000,"asc","retrieval_next",DateParser.getTimezoneOffset(),null,"now","retrieval_next",3);
            for (QueryEntry qe: rl) {
                System.out.println(UTF8.String(qe.toJSONBytes()));
            }
            System.out.println("hits: " + rl.getHits());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
