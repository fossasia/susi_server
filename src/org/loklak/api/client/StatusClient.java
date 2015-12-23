/**
 *  StatusClient
 *  Copyright 23.12.2015 by Michael Peter Christen, @0rb1t3r
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
import java.util.HashMap;
import java.util.Map;

import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;

public class StatusClient {

    public static Map<String, Object> status(final String protocolhostportstub) throws IOException {
        final String urlstring = protocolhostportstub + "/api/status.json";
        byte[] response = ClientConnection.download(urlstring);
        byte[] json = response;
        if (json == null || json.length == 0) return new HashMap<>();
        Map<String, Object> map = DAO.jsonMapper.readValue(json, DAO.jsonTypeRef);
        return map;
    }
    
    public static void main(String[] args) {
        try {
            Map<String, Object> json = status("http://loklak.org");
            Map<String, Object> index_sizs = (Map<String, Object>) json.get("index_sizes");
            System.out.println(json.toString());
            System.out.println(index_sizs.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
