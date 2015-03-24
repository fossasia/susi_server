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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.ProviderType;
import org.loklak.Timeline;
import org.loklak.Tweet;
import org.loklak.User;
import org.loklak.api.ClientHelper;

public class SearchClient {
    
    private final static String backend_hash = Integer.toHexString(Integer.MAX_VALUE);

    // possible values: cache, twitter, all
    public static Timeline search(String protocolhostportstub, String query, String source, int count) {
        Timeline tl = new Timeline();
        String json = searchJSON(protocolhostportstub, query, source, count);
        if (json == null || json.length() == 0) return tl;
        try {
            XContentParser parser = JsonXContent.jsonXContent.createParser(json);
            Map<String, Object> map = parser == null ? null : parser.map();
            Object statuses_obj = map.get("statuses");
            @SuppressWarnings("unchecked") List<Map<String, Object>> statuses = statuses_obj instanceof List<?> ? (List<Map<String, Object>>) statuses_obj : null;
            if (statuses != null) {
                for (Map<String, Object> tweet: statuses) {
                    @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) tweet.remove("user");
                    if (user == null) continue;
                    tweet.put("provider_type", (Object) ProviderType.REMOTE.name());
                    tweet.put("provider_hash", backend_hash);
                    User u = new User(user);
                    Tweet t = new Tweet(tweet);
                    tl.addUser(u);
                    tl.addTweet(t);
                }
            }
            //System.out.println(parser.text());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tl;
    }
    
    private static String searchJSON(String protocolhostportstub, String query, String source, int count) {
        String urlstring = "";
        try {urlstring = protocolhostportstub + "/api/search.json?q=" + URLEncoder.encode(query.replace(' ', '+'), "UTF-8") + "&maximumRecords=" + count + "&source=" + (source == null ? "all" : source) + "&minified=true";} catch (UnsupportedEncodingException e) {}
        try {
            BufferedReader br = ClientHelper.getConnection(urlstring);
            if (br == null) return "";
            StringBuilder sb = new StringBuilder();
            try {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
            } catch (IOException e) {
               e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        };
        return "";
    }
    
    public static void main(String[] args) {
        Timeline tl = search("http://loklak.org", "beer", "cache", 20);
        System.out.println(tl.toJSON(false));
    }
    
}
