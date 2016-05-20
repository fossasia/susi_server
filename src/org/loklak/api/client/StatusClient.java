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

import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.tools.UTF8;

public class StatusClient {

    public static JSONObject status(final String protocolhostportstub) throws IOException {
        final String urlstring = protocolhostportstub + "/api/status.json";
        byte[] response = ClientConnection.downloadPeer(urlstring);
        if (response == null || response.length == 0) return new JSONObject();
        JSONObject json = new JSONObject(UTF8.String(response));
        return json;
    }
    
    public static void main(String[] args) {
        try {
            JSONObject json = status("http://loklak.org");
            JSONObject index_sizs = (JSONObject) json.get("index_sizes");
            System.out.println(json.toString());
            System.out.println(index_sizs.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
