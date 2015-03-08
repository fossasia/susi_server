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

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.ClientHelper;

public class SearchClient {
    
    public static String search(String protocolhostportstub, String query, int count) {
        String urlstring = "";
        try {urlstring = protocolhostportstub + "/api/search.json?q=" + URLEncoder.encode(query, "UTF-8") + "&maximumRecords=" + count;} catch (UnsupportedEncodingException e) {}
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
        String json = search("http://loklak.org", "beer", 20);
        //System.out.println(json);
        try {
            XContentParser parser = JsonXContent.jsonXContent.createParser(json);
            System.out.println(parser.text());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
