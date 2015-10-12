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

package org.loklak.api.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.elasticsearch.common.Base64;
import org.loklak.LoklakServer;
import org.loklak.data.AccessTracker;
import org.loklak.data.DAO;
import org.loklak.tools.DateParser;
import org.loklak.tools.UTF8;
import org.loklak.visualization.graphics.RasterPlotter;

/**
 * Storage of a peer list which can be used for peer-to-peer communication.
 * This is a static class to provide access to all other objects easily.
 * We store the IPs here only temporary, not permanently.
 */
public class RemoteAccess {

    public static Map<String, Map<String, RemoteAccess>> history = new ConcurrentHashMap<String, Map<String, RemoteAccess>>();
    
    public static Post evaluate(final HttpServletRequest request) {
        String path = request.getServletPath();
        Map<String, String> qm = getQueryMap(request.getQueryString());
        Post post = new Post(request);
        post.initGET(qm);
        String httpports = qm == null ? request.getParameter("port.http") : qm.get("port.http");
        Integer httpport = httpports == null ? null : Integer.parseInt(httpports);
        String httpsports = qm == null ? request.getParameter("port.https") : qm.get("port.https");
        Integer httpsport = httpsports == null ? null : Integer.parseInt(httpsports);
        String peername = qm == null ? request.getParameter("peername") : qm.get("peername");
        if (peername == null || peername.length() > 132) peername = "anonymous";
        final String remoteHost = post.getClientHost();
        Map<String, RemoteAccess> hmap = history.get(post.track.getClassName());
        if (hmap == null) {hmap = new ConcurrentHashMap<>(); history.put(post.track.getClassName(), hmap);}
        if (httpport == null || httpsport == null) {
            // if port configuration is omitted, just update the value if it exist
            RemoteAccess ra = hmap.get(remoteHost);
            if (ra == null) {
                hmap.put(remoteHost, new RemoteAccess(remoteHost, path, httpport, httpsport, peername));
            } else {
                assert ra.remoteHost.equals(remoteHost);
                ra.localPath = path;
                ra.accessTime = System.currentTimeMillis();
            }
        } else {
            // overwrite if new port configuration is submitted
            RemoteAccess ra = new RemoteAccess(remoteHost, path, httpport, httpsport, peername);
            hmap.put(remoteHost, ra);
            DAO.updateFrontPeerCache(ra);
        }
        return post;
    }
    
    public static long latestVisit(String servlet, String remoteHost) {
        Map<String, RemoteAccess> hmap = history.get(servlet);
        if (hmap == null) {hmap = new ConcurrentHashMap<>(); history.put(servlet, hmap);}
        RemoteAccess ra = hmap.get(remoteHost);
        return ra == null ? -1 : ra.accessTime;
    }
    
    public static String hostHash(String remoteHost) {
        return Integer.toHexString(Math.abs(remoteHost.hashCode()));
    }
    
    public static class Post {
        private HttpServletRequest request;
        private Map<String, String> qm;
        private AccessTracker.Track track;
        public Post(final HttpServletRequest request) {
            this.qm = new HashMap<>();
            for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
                this.qm.put(entry.getKey(), entry.getValue()[0]);
            }
            this.request = request;
            
            // discover remote host
            String clientHost = request.getRemoteHost();
            String XRealIP = request.getHeader("X-Real-IP");
            if (XRealIP != null && XRealIP.length() > 0) clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
            
            // start tracking: get calling thread and start tracking for that
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            StackTraceElement caller = stackTraceElements[3];
            this.track = DAO.access.startTracking(caller.getClassName(), clientHost);
            
            this.track.setTimeSinceLastAccess(this.track.getDate().getTime() - RemoteAccess.latestVisit(this.track.getClassName(), clientHost));
            //System.out.println("*** this.time_since_last_access = " + this.time_since_last_access);
            this.track.setDoSBlackout(LoklakServer.blacklistedHosts.contains(clientHost) || (!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.blackout", 100) || sleeping4clients.contains(clientHost))));
            this.track.setDoSServicereduction(!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.servicereduction", 1000) || sleeping4clients.contains(clientHost)));
        }
        public void finalize() {
            this.track.finalize();
        }
        public void initGET(final Map<String, String> qm) {
            this.qm = qm;
            this.track.setQuery(qm);
        }
        public void initPOST(final Map<String, byte[]> map) {
            this.qm = new HashMap<>();
            for (Map.Entry<String, byte[]> entry: map.entrySet()) this.qm.put(entry.getKey(), UTF8.String(entry.getValue()));
        }
        public String getClientHost() {
            return this.track.getClientHost();
        }
        public boolean isLocalhostAccess() {
            return this.track.isLocalhostAccess();
        }
        public long getAccessTime() {
            return this.track.getDate().getTime();
        }
        public long getTimeSinceLastAccess() {
            return this.track.getTimeSinceLastAccess();
        }
        public boolean isDoS_blackout() {
            return this.track.isDoSBlackout();
        }
        public boolean isDoS_servicereduction() {
            return this.track.isDoSServicereduction();
        }
        public void recordEvent(String eventName, Object eventValue) {
            this.track.put(AccessTracker.EVENT_PREFIX + eventName, eventValue);
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
            return val == null || val.length() == 0 ? dflt : Integer.parseInt(val);
        }
        public double get(String key, double dflt) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null || val.length() == 0 ? dflt : Double.parseDouble(val);
        }
        public boolean get(String key, boolean dflt) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            return val == null ? dflt : "true".equals(val) || "1".equals(val);
        }
        public Date get(String key, Date dflt, int timezoneOffset) {
            String val = qm == null ? request.getParameter(key) : qm.get(key);
            try {
                return val == null || val.length() == 0 ? dflt : DateParser.parse(val, timezoneOffset).getTime();
            } catch (ParseException e) {
                return dflt;
            }
        }
        public void setResponse(final HttpServletResponse response, final String mime) {
            long access_time = this.getAccessTime();
            response.setDateHeader("Last-Modified", access_time);
            response.setDateHeader("Expires", access_time + 2 * DAO.getConfig("DoS.servicereduction", 1000));
            response.setContentType(mime);
            response.setHeader("X-Robots-Tag",  "noindex,noarchive,nofollow,nosnippet");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
        }
        public int hashCode() {
            return qm.hashCode();
        }
    }
    
    private static HashSet<String> sleeping4clients = new HashSet<>();
    
    public static void sleep(String host, long time) {
        try {
            sleeping4clients.add(host);
            Thread.sleep(time);
        } catch (InterruptedException e) {
        } finally {
            sleeping4clients.remove(host);
        }
    }
    
    public static boolean isSleepingForClient(String host) {
        return sleeping4clients.contains(host);
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
    
    private static Set<String> localhostNames = new HashSet<>();
    static {
        localhostNames.add("0:0:0:0:0:0:0:1");
        localhostNames.add("fe80:0:0:0:0:0:0:1%1");
        localhostNames.add("127.0.0.1");
        localhostNames.add("localhost");
        try {localhostNames.add(InetAddress.getLocalHost().getHostAddress());} catch (UnknownHostException e) {}
        try {localhostNames.add(InetAddress.getLocalHost().getHostName());} catch (UnknownHostException e) {}
        try {localhostNames.add(InetAddress.getLocalHost().getCanonicalHostName());} catch (UnknownHostException e) {}
        try {for (InetAddress a: InetAddress.getAllByName(null)) {localhostNames.add(a.getHostAddress()); localhostNames.add(a.getHostName()); localhostNames.add(a.getCanonicalHostName());}} catch (UnknownHostException e) {}
        try {for (InetAddress a: InetAddress.getAllByName("localhost")) {localhostNames.add(a.getHostAddress()); localhostNames.add(a.getHostName()); localhostNames.add(a.getCanonicalHostName());}} catch (UnknownHostException e) {}
        //System.out.println(localhostNames);
    }
    
    public static boolean isLocalhost(String host) {
        return localhostNames.contains(host);
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
    
    public static Map<String, byte[]> getPostMap(HttpServletRequest request) {
        Map<String, byte[]> map = new HashMap<>();
        Map<String, String[]> pm = request.getParameterMap();
        if (pm != null && pm.size() > 0) {
            for (Map.Entry<String, String[]> entry: pm.entrySet()) {
                String[] v = entry.getValue();
                if (v != null && v.length > 0) map.put(entry.getKey(), UTF8.getBytes(v[0]));
            }
        } else try {
            final byte[] b = new byte[1024];
            for (Part part: request.getParts()) {
                String name = part.getName();
                InputStream is = part.getInputStream();
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int c;
                try {while ((c = is.read(b, 0, b.length)) > 0) {
                    baos.write(b, 0, c);
                }} finally {is.close();}
                map.put(name, baos.toByteArray());
            }
        } catch (IOException e) {
        } catch (ServletException e) {
        }
        return map;
    }

    public static enum FileType {
        UNKNOWN, PNG, JPG, GIF, JSON, RSS;
    }
    
    public static class FileTypeEncoding {
        public final FileType fileType;
        public final boolean base64;
        private FileTypeEncoding(final FileType fileType, final boolean base64) {
            this.fileType = fileType;
            this.base64 = base64;
        }
        private FileTypeEncoding(final FileType fileType) {
            this.fileType = fileType;
            this.base64 = false;
        }
    }
    
    public static FileTypeEncoding getFileType(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath.endsWith(".gif")) return new FileTypeEncoding(FileType.GIF);
        if (servletPath.endsWith(".gif.base64")) return new FileTypeEncoding(FileType.GIF, true);
        if (servletPath.endsWith(".png")) return new FileTypeEncoding(FileType.PNG);
        if (servletPath.endsWith(".png.base64")) return new FileTypeEncoding(FileType.PNG, true);
        if (servletPath.endsWith(".jpg")) return new FileTypeEncoding(FileType.JPG);
        if (servletPath.endsWith(".jpg.base64")) return new FileTypeEncoding(FileType.JPG, true);
        return new FileTypeEncoding(FileType.UNKNOWN);
    }
    
    public static void writeImage(final FileTypeEncoding fileType, final HttpServletResponse response, Post post, final RasterPlotter matrix) throws IOException {
        // write image
        ServletOutputStream sos = response.getOutputStream();
        if (fileType.fileType == FileType.PNG) {
            post.setResponse(response, fileType.base64 ? "application/octet-stream" : "image/png");
            sos.write(fileType.base64 ? Base64.encodeBytes(matrix.pngEncode(1)).getBytes() : matrix.pngEncode(1));
        }
        if (fileType.fileType == FileType.GIF) {
            post.setResponse(response, fileType.base64 ? "application/octet-stream" : "image/gif");
            if (fileType.base64) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(matrix.getImage(), "gif", baos);
                baos.close();
                sos.write(Base64.encodeBytes(baos.toByteArray()).getBytes());
            } else {
                ImageIO.write(matrix.getImage(), "gif", sos);
            }
        }
        if (fileType.fileType == FileType.JPG) {
            post.setResponse(response, fileType.base64 ? "application/octet-stream" : "image/jpeg");
            if (fileType.base64) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(matrix.getImage(), "jpg", baos);
                baos.close();
                sos.write(Base64.encodeBytes(baos.toByteArray()).getBytes());
            } else {
                ImageIO.write(matrix.getImage(), "jpg", sos);
            }
        }
    }
}
