/**
 *  StatusServlet
 *  Copyright 27.02.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.susi;

import ai.susi.Caretaker;
import ai.susi.SusiServer;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.OS;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

// http://127.0.0.1:4000/susi/status.json
public class StatusService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public String getAPIPath() {
        return "/susi/status.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }


    public static JSONObject status(final String protocolhostportstub) throws IOException {
        final String urlstring = protocolhostportstub + "/susi/status.json";
        byte[] response = ClientConnection.download(urlstring);
        if (response == null || response.length == 0) return new JSONObject();
        final byte[] bytes = response;
        JSONObject json = new JSONObject(bytes == null ? "" : new String(bytes, 0, bytes.length, StandardCharsets.UTF_8));
        return json;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        post.setResponse(response, "application/javascript");

        // generate json
        Runtime runtime = Runtime.getRuntime();
        JSONObject json = new JSONObject(true);
        JSONObject system = new JSONObject(true);
        system.put("assigned_memory", runtime.maxMemory());
        system.put("used_memory", runtime.totalMemory() - runtime.freeMemory());
        system.put("available_memory", runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory());
        system.put("cores", runtime.availableProcessors());
        system.put("threads", Thread.activeCount());
        system.put("runtime", System.currentTimeMillis() - Caretaker.startupTime);
        system.put("load_system_average", OS.getSystemLoadAverage());
        system.put("load_system_cpu", OS.getSystemCpuLoad());
        system.put("load_process_cpu", OS.getProcessCpuLoad());
        system.put("server_threads", SusiServer.getServerThreads());
        system.put("server_uri", SusiServer.getServerURI());
        SusiServer.hostInfo.forEach((key, value) -> system.put(key, value));

        JSONObject index = new JSONObject(true);
        JSONObject messages = new JSONObject(true);
        JSONObject queue = new JSONObject(true);
        messages.put("queue", queue);
        JSONObject users = new JSONObject(true);
        JSONObject queries = new JSONObject(true);
        JSONObject accounts = new JSONObject(true);
        JSONObject user = new JSONObject(true);
        index.put("messages", messages);
        index.put("users", users);
        index.put("queries", queries);
        index.put("accounts", accounts);
        index.put("user", user);

        
        JSONObject client_info = new JSONObject(true);
        client_info.put("RemoteHost", post.getClientHost());
        client_info.put("IsLocalhost", post.isLocalhostAccess() ? "true" : "false");

        JSONObject request_header = new JSONObject(true);
        Enumeration<String> he = post.getRequest().getHeaderNames();
        while (he.hasMoreElements()) {
            String h = he.nextElement();
            request_header.put(h, post.getRequest().getHeader(h));
        }
        client_info.put("request_header", request_header);
        
        json.put("system", system);
        json.put("index", index);
        json.put("client_info", client_info);

        return new ServiceResponse(json);
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
