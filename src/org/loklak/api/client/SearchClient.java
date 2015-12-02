/**
 *  SearchClient
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.loklak.data.DAO;
import org.loklak.data.ProviderType;
import org.loklak.data.Timeline;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.http.ClientConnection;

public class SearchClient {

    public final static String backend_hash = Integer.toHexString(Integer.MAX_VALUE);
    public final static String frontpeer_hash = Integer.toHexString(Integer.MAX_VALUE - 1);

    // possible values: cache, twitter, all
    public static Timeline search(final String protocolhostportstub, final String query, final Timeline.Order order, final String source, final int count, final int timezoneOffset, final String provider_hash, final long timeout) throws IOException {
        Timeline tl = new Timeline(order);
        String urlstring = "";
        try {
            urlstring = protocolhostportstub + "/api/search.json?q=" + URLEncoder.encode(query.replace(' ', '+'), "UTF-8") + "&timezoneOffset=" + timezoneOffset + "&maximumRecords=" + count + "&source=" + (source == null ? "all" : source) + "&minified=true&timeout=" + timeout;
        } catch (UnsupportedEncodingException e1) {
            return tl;
        }
        byte[] json = ClientConnection.download(urlstring);
        if (json == null || json.length == 0) return tl;
        Map<String, Object> map = DAO.jsonMapper.readValue(json, DAO.jsonTypeRef);
        Object statuses_obj = map.get("statuses");
        @SuppressWarnings("unchecked") List<Map<String, Object>> statuses = statuses_obj instanceof List<?> ? (List<Map<String, Object>>) statuses_obj : null;
        if (statuses != null) {
            for (Map<String, Object> tweet: statuses) {
                @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) tweet.remove("user");
                if (user == null) continue;
                tweet.put("provider_type", (Object) ProviderType.REMOTE.name());
                tweet.put("provider_hash", provider_hash);
                UserEntry u = new UserEntry(user);
                MessageEntry t = new MessageEntry(tweet);
                tl.add(t, u);
            }
        }
        Object metadata_obj = map.get("search_metadata");
        @SuppressWarnings("unchecked") Map<String, Object> metadata = metadata_obj instanceof Map<?,?> ? (Map<String, Object>) metadata_obj : null;
        if (metadata != null) {
            Integer hits = (Integer) metadata.get("hits");
            if (hits != null) tl.setHits(hits.intValue());
            String scraperInfo = (String) metadata.get("scraperInfo");
            if (scraperInfo != null) tl.setScraperInfo(scraperInfo);
        }
        //System.out.println(parser.text());
        return tl;
    }
    
    public static void main(String[] args) {
        try {
            Timeline tl = search("http://loklak.org", "beer", Timeline.Order.CREATED_AT, "cache", 20, -120, backend_hash, 10000);
            System.out.println(tl.toJSON(false).toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
