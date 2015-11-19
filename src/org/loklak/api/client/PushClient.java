/**
 *  PushClient
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
import java.util.HashMap;
import java.util.Map;

import org.loklak.data.Timeline;
import org.loklak.http.ClientConnection;
import org.loklak.tools.UTF8;
import org.loklak.tools.json.JSONException;

public class PushClient {
    
    /**
     * transmit the timeline to several hosts
     * @param timeline
     * @param hoststubs a list of host stubs, i.e. ["http://remoteserver.eu"]
     * @return true if the data was transmitted to at least one target peer
     */
    public static boolean push(String[] hoststubs, Timeline timeline) {
        // transmit the timeline
        assert timeline.size() != 0;
        if (timeline.size() == 0) return true;
        
        try {
            String data = timeline.toJSON(false).toString();
            assert data != null;
            boolean transmittedToAtLeastOnePeer = false;
            for (String hoststub: hoststubs) {
                if (hoststub.endsWith("/")) hoststub = hoststub.substring(0, hoststub.length() - 1);
                Map<String, byte[]> post = new HashMap<String, byte[]>();
                post.put("data", UTF8.getBytes(data)); // optionally implement a gzipped form here
                ClientConnection connection = null;
                try {
                    connection = new ClientConnection(hoststub + "/api/push.json", post);
                    transmittedToAtLeastOnePeer = true;
                } catch (IOException e) {
                    //e.printStackTrace();
                } finally {
                    if (connection != null) connection.close();
                }
            }
            return transmittedToAtLeastOnePeer;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
