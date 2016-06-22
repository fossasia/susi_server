/**
 *  HelloServlet
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

package org.loklak.api.p2p;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.loklak.api.admin.StatusServlet;
import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.tools.DateParser;
import org.loklak.tools.UTF8;

/**
 * Servlet to span the message peer-to-peer network.
 * This servlet is called to announce the existence of the remote peer.
 */
public class HelloService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 1839868262296635665L;


    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/api/hello.json";
    }
    

    public static void propagate(final String[] hoststubs) {
        // get some configuration
        int httpport = (int) DAO.getConfig("port.http", 9000);
        int httpsport = (int) DAO.getConfig("port.https", 9443);
        String peername = (String) DAO.getConfig("peername", "anonymous");

        // retrieve some simple statistics from the index
        final String backend = DAO.getConfig("backend", "");
        final boolean backend_push = DAO.getConfig("backend.push.enabled", false);
        JSONObject backend_status = null;
        JSONObject backend_status_index_sizes = null;
        if (backend.length() > 0 && !backend_push) {
            try {
                backend_status = StatusServlet.status(backend);
            } catch (IOException e) {
            	Log.getLog().warn(e);
            }
            backend_status_index_sizes = backend_status == null ? null : (JSONObject) backend_status.get("index_sizes");
        }
        long backend_messages = backend_status_index_sizes == null ? 0 : ((Number) backend_status_index_sizes.get("messages")).longValue();
        long backend_users = backend_status_index_sizes == null ? 0 : ((Number) backend_status_index_sizes.get("users")).longValue();
        long local_messages = DAO.countLocalMessages(-1);
        long local_users = DAO.countLocalUsers();
        int timezoneOffset = DateParser.getTimezoneOffset();
        
        // retrieve more complex data: date histogram
        LinkedHashMap<String, Long> fullDateHistogram = DAO.FullDateHistogram(timezoneOffset); // complex operation can take some time!
        String peakDay = ""; long peakCount = -1;
        String lastDay = ""; long lastCount = -1;
        for (Map.Entry<String, Long> day: fullDateHistogram.entrySet()) {
            lastDay = day.getKey(); lastCount = day.getValue();
            if (lastCount > peakCount) {peakDay = lastDay; peakCount = lastCount;}
        }
        String firstDay = ""; long firstCount = -1;
        if (fullDateHistogram.size() > 0) {
            Map.Entry<String, Long> firstDayEntry = fullDateHistogram.entrySet().iterator().next();
            firstDay = firstDayEntry.getKey(); firstCount = firstDayEntry.getValue();
        }
        
        // send data to peers
        for (String hoststub: hoststubs) {
            if (hoststub.endsWith("/")) hoststub = hoststub.substring(0, hoststub.length() - 1);
            try {
                String urlstring = hoststub + "/api/hello.json?port.http=" + httpport + "&port.https=" + httpsport +
                        "&peername=" + peername +
                        "&time=" + System.currentTimeMillis() +
                        "&timezoneOffset=" + timezoneOffset +
                        "&local_messages=" + local_messages +
                        "&local_users=" + local_users +
                        "&backend_messages=" + backend_messages +
                        "&backend_users=" + backend_users +
                        "&peakDay=" + peakDay +
                        "&peakCount=" + peakCount +
                        "&firstDay=" + firstDay +
                        "&firstCount=" + firstCount +
                        "&lastDay=" + lastDay +
                        "&lastCount=" + lastCount;
                byte[] jsonb = ClientConnection.downloadPeer(urlstring);
                if (jsonb == null || jsonb.length == 0) throw new IOException("empty content from " + hoststub);
                String jsons = UTF8.String(jsonb);
                JSONObject json = new JSONObject(jsons);
                Log.getLog().info("Hello response: " + json.toString());
            } catch (IOException e) {
            }
        }
    }
    
    @Override
    public JSONObject serviceImpl(Query call, Authorization rights) {
    	JSONObject res = DAO.public_settings.toJSONObject();
    	res.put("status", "ok");
    	return res;
    }
}
