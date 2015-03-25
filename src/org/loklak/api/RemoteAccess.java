/**
 *  RemoteAccess
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

package org.loklak.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.loklak.DAO;

/**
 * Storage of a peer list which can be used for peer-to-peer communication.
 * This is a static class to provide access to all other objects easily.
 * We store the IPs here only temporary, not permanently.
 */
public class RemoteAccess {

    public static Map<String, RemoteAccess> history = new ConcurrentHashMap<String, RemoteAccess>();
    
    public static Post evaluate(final HttpServletRequest request) {
        String clientHost = request.getRemoteHost();
        String XRealIP = request.getHeader("X-Real-IP"); if (XRealIP != null && XRealIP.length() > 0) clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
        String path = request.getServletPath();
        Map<String, String> qm = getQueryMap(request.getQueryString());
        Post post = new Post(request, qm);
        String httpports = qm == null ? request.getParameter("port.http") : qm.get("port.http");
        Integer httpport = httpports == null ? null : Integer.parseInt(httpports);
        String httpsports = qm == null ? request.getParameter("port.https") : qm.get("port.https");
        Integer httpsport = httpsports == null ? null : Integer.parseInt(httpsports);
        String peername = qm == null ? request.getParameter("peername") : qm.get("peername");
        if (peername == null || peername.length() > 132) peername = "anonymous";
        post.setRemoteAccess(init(clientHost, path, httpport, httpsport, peername));
        return post;
    }
    
    private static RemoteAccess init(final String remoteHost, final String localPath, final Integer localHTTPPort, final Integer localHTTPSPort, final String peername) {
        RemoteAccess ra;
        if (localHTTPPort == null || localHTTPSPort == null) {
            // if port configuration is omitted, just update the value if it exist
            ra = history.get(remoteHost);
            if (ra == null) {
                history.put(remoteHost, new RemoteAccess(remoteHost, localPath, localHTTPPort, localHTTPSPort, peername));
            } else {
                assert ra.remoteHost.equals(remoteHost);
                ra.localPath = localPath;
                ra.accessTime = System.currentTimeMillis();
            }
        } else {
            // overwrite if new port configuration is submitted
            ra = new RemoteAccess(remoteHost, localPath, localHTTPPort, localHTTPSPort, peername);
            history.put(remoteHost, ra);
        }
        return ra;
    }
    
    public static long latestVisit(String remoteHost) {
        RemoteAccess ra = history.get(remoteHost);
        return ra == null ? -1 : ra.accessTime;
    }
    
    public static String hostHash(String remoteHost) {
        return Integer.toHexString(Math.abs(remoteHost.hashCode()));
    }
    
    public static class Post {
        private HttpServletRequest request;
        private Map<String, String> qm;
        private RemoteAccess ra;
        private String clientHost;
        private long access_time, time_since_last_access;
        private boolean DoS_blackout, DoS_servicereduction;
        public Post(final HttpServletRequest request, final Map<String, String> qm) {
            this.qm = qm;
            this.request = request;
            this.ra = null;
            this.clientHost = request.getRemoteHost();
            String XRealIP = request.getHeader("X-Real-IP");
            if (XRealIP != null && XRealIP.length() > 0) this.clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
            this.access_time = System.currentTimeMillis();
            this.time_since_last_access = this.access_time - RemoteAccess.latestVisit(this.clientHost);
            this.DoS_blackout = this.time_since_last_access < DAO.getConfig("DoS.blackout", 100);
            this.DoS_servicereduction = this.time_since_last_access < DAO.getConfig("DoS.servicereduction", 1000);
        }
        public String getClientHost() {
            return this.clientHost;
        }
        public boolean isLocalhostAccess() {
            return isLocalhost(this.clientHost);
        }
        public long getAccessTime() {
            return this.access_time;
        }
        public long getTimeSinceLastAccess() {
            return this.time_since_last_access;
        }
        public boolean isDoS_blackout() {
            return this.DoS_blackout;
        }
        public boolean isDoS_servicereduction() {
            return this.DoS_servicereduction;
        }
        public String get(String key, String dflt) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null ? dflt : val;
        }
        public String[] get(String key, String[] dflt, String delim) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null || val.length() == 0 ? dflt : val.split(delim);
        }
        public int get(String key, int dflt) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null ? dflt : Integer.parseInt(val);
        }
        public boolean get(String key, boolean dflt) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null ? dflt : "true".equals(val) || "1".equals(val);
        }
        public void setRemoteAccess(final RemoteAccess ra) {
            this.ra = ra;
        }
        public RemoteAccess getRemoteAccess() {
            return this.ra;
        }
        public void setResponse(final HttpServletResponse response, final String mime) {
            response.setDateHeader("Last-Modified", this.access_time);
            response.setDateHeader("Expires", this.access_time + 2 * DAO.getConfig("DoS.servicereduction", 1000));
            response.setContentType(mime);
            response.setHeader("X-Robots-Tag",  "noindex,noarchive,nofollow,nosnippet");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
    
    private String remoteHost, localPath, peername;
    private int localHTTPPort, localHTTPSPort;
    private long accessTime;
    
    private RemoteAccess(final String remoteHost, final String localPath, final Integer localHTTPPort, final Integer localHTTPSPort, final String peername) {
        this.remoteHost = remoteHost;
        this.localPath = localPath;
        this.localHTTPPort = localHTTPPort == null ? -1 : localHTTPPort.intValue();
        this.localHTTPSPort = localHTTPSPort == null ? -1 : localHTTPSPort.intValue();
        this.peername = peername;
        this.accessTime = System.currentTimeMillis();
    }
    
    public String getRemoteHost() {
        return this.remoteHost;
    }
    
    public String getLocalPath() {
        return this.localPath;
    }

    public long getAccessTime() {
        return this.accessTime;
    }

    public int getLocalHTTPPort() {
        return this.localHTTPPort;
    }

    public int getLocalHTTPSPort() {
        return this.localHTTPSPort;
    }
    
    public String getPeername() {
        return this.peername;
    }
    
    public static boolean isLocalhost(String host) {
        return "0:0:0:0:0:0:0:1".equals(host) || "fe80:0:0:0:0:0:0:1%1".equals(host) || "127.0.0.1".equals(host) || "localhost".equals(host);
    }

    public static Map<String, String> getQueryMap(String query)   {  
        if (query == null) return null;
        String[] params = query.split("&");  
        Map<String, String> map = new HashMap<String, String>();  
        for (String param : params) {
            int p = param.indexOf('=');
            if (p >= 0)
                try {map.put(param.substring(0, p), URLDecoder.decode(param.substring(p + 1), "UTF-8"));} catch (UnsupportedEncodingException e) {}
        }  
        return map;  
    }  
    
    public static Map<String, String> getPostMap(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            final char[] buffer = new char[1024];
            for (Part part: request.getParts()) {
                String name = part.getName();
                InputStream is = part.getInputStream();
                final StringBuilder out = new StringBuilder();
                final Reader in = new InputStreamReader(is, "UTF-8");
                int c;
                try {while ((c = in.read(buffer, 0, buffer.length)) > 0) {
                    out.append(buffer, 0, c);
                }} finally {is.close();}
                map.put(name, out.toString());
            }
        } catch (IOException e) {
        } catch (ServletException e) {
        }
        return map;
    }
}
