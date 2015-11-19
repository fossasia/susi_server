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
import java.util.List;
import java.util.Map;

import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;
import org.loklak.data.ResultList;
import org.loklak.http.ClientConnection;
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
                "&minified=true";
        byte[] response = ClientConnection.download(urlstring);
        byte[] json = response;
        if (json == null || json.length == 0) return rl;
        Map<String, Object> map = DAO.jsonMapper.readValue(json, DAO.jsonTypeRef);
        Object queries_obj = map.get("queries");
        @SuppressWarnings("unchecked") List<Map<String, Object>> queries = queries_obj instanceof List<?> ? (List<Map<String, Object>>) queries_obj : null;
        if (queries != null) {
            for (Map<String, Object> query: queries) {
                if (query == null) continue;
                QueryEntry qe = new QueryEntry(query);
                rl.add(qe);
            }
        }
        
        Object metadata_obj = map.get("search_metadata");
        if (metadata_obj != null && metadata_obj instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Integer hits = (Integer) ((Map<String, Object>) metadata_obj).get("hits");
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
