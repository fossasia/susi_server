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
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.tools.UTF8;

public class SearchClient {

    public final static String backend_hash = Integer.toHexString(Integer.MAX_VALUE);
    public final static String frontpeer_hash = Integer.toHexString(Integer.MAX_VALUE - 1);

    // possible values: cache, twitter, all
    public static Timeline search(final String protocolhostportstub, final String query, final Timeline.Order order, final String source, final int count, final int timezoneOffset, final String provider_hash, final long timeout) throws IOException {
        Timeline tl = new Timeline(order);
        String urlstring = "";
        try {
            urlstring = protocolhostportstub + "/api/search.json?q=" + URLEncoder.encode(query.replace(' ', '+'), "UTF-8") + "&timezoneOffset=" + timezoneOffset + "&maximumRecords=" + count + "&source=" + (source == null ? "all" : source) + "&minified=true&timeout=" + timeout;
            byte[] jsonb = ClientConnection.download(urlstring);
            if (jsonb == null || jsonb.length == 0) throw new IOException("empty content from " + protocolhostportstub);
            String jsons = UTF8.String(jsonb);
            JSONObject json = new JSONObject(jsons);
            if (json == null || json.length() == 0) return tl;
            JSONArray statuses = json.getJSONArray("statuses");
            if (statuses != null) {
                for (int i = 0; i < statuses.length(); i++) {
                    JSONObject tweet = statuses.getJSONObject(i);
                    JSONObject user = tweet.getJSONObject("user");
                    if (user == null) continue;
                    tweet.remove("user");
                    UserEntry u = new UserEntry(user);
                    MessageEntry t = new MessageEntry(tweet);
                    tl.add(t, u);
                }
            }
            JSONObject metadata = json.getJSONObject("search_metadata");
            if (metadata != null) {
                Integer hits = metadata.has("hits") ? (Integer) metadata.get("hits") : null;
                if (hits != null) tl.setHits(hits.intValue());
                String scraperInfo = metadata.has("scraperInfo") ? (String) metadata.get("scraperInfo") : null;
                if (scraperInfo != null) tl.setScraperInfo(scraperInfo);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
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
