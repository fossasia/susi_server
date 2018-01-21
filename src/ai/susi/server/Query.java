/**
 *  Query
 *  Copyright 19.05.2016 by Michael Peter Christen, @0rb1t3r
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


package ai.susi.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.io.ByteStreams;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.tools.DateParser;

public class Query {
    
    private HttpServletRequest request;
    private Map<String, byte[]> qm;
    public AccessTracker.Track track;
    
    public Query(final HttpServletRequest request) {
        this.qm = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
            this.qm.put(entry.getKey(), entry.getValue()[0].getBytes(StandardCharsets.UTF_8));
        }
        this.request = request;
        
        // discover remote host
        String clientHost = request.getRemoteHost();
        String XRealIP = request.getHeader("X-Real-IP");
        if (XRealIP != null && XRealIP.length() > 0) clientHost = XRealIP; // get IP through nginx config "proxy_set_header X-Real-IP $remote_addr;"
        
        // start tracking: get calling thread and start tracking for that
        this.track = DAO.access.startTracking(request.getServletPath(), clientHost);
        this.track.setTimeSinceLastAccess(this.track.getDate().getTime() - RemoteAccess.latestVisit(request.getServletPath(), clientHost));
        this.track.setDoSBlackout(SusiServer.blacklistedHosts.contains(clientHost) || (!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.blackout", 100))));
        this.track.setDoSServicereduction(!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.servicereduction", 1000)));
    }
    public void finalize() {
        this.track.finalize();
    }
    public void initGET(final Map<String, String> q) {
        q.forEach((k, v) -> this.qm.put(k, v.getBytes(StandardCharsets.UTF_8)));
        this.track.setQuery(q);
    }
    public void initPOST(final Map<String, byte[]> map) {
        if (this.qm == null) this.qm = map; else this.qm.putAll(map);
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
    public String get(String key) {
        String val = this.request == null ? null : this.request.getParameter(key);
        if (val == null && this.qm.containsKey(key)) return new String(this.qm.get(key), StandardCharsets.UTF_8);
        return val;
    }
    public String get(String key, String dflt) {
        String val = this.request == null ? null : this.request.getParameter(key);
        if (val == null && this.qm.containsKey(key)) return new String(this.qm.get(key), StandardCharsets.UTF_8);
        return val == null ? dflt : val;
    }
    public String[] get(String key, String[] dflt, String delim) {
        String val = get(key);
        return val == null || val.length() == 0 ? dflt : val.split(delim);
    }
    public int get(String key, int dflt) {
        String val = get(key);
        return val == null || val.length() == 0 ? dflt : Integer.parseInt(val.trim());
    }
    public long get(String key, long dflt) {
        String val = get(key);
        return val == null || val.length() == 0 ? dflt : Long.parseLong(val.trim());
    }
    public double get(String key, double dflt) {
        String val = get(key);
        return val == null || val.length() == 0 ? dflt : Double.parseDouble(val.trim());
    }
    public boolean get(String key, boolean dflt) {
        String val = get(key);
        return val == null ? dflt : "true".equals(val = val.trim()) || "1".equals(val);
    }
    public Date get(String key, Date dflt, int timezoneOffset) {
        String val = get(key);
        try {
            return val == null || val.length() == 0 ? dflt : DateParser.parse(val.trim(), timezoneOffset).getTime();
        } catch (ParseException e) {
            return dflt;
        }
    }
    public JSONObject getJSONBody() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(request.getInputStream(), baos);
            baos.close();
            String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            if (data == null || data.length() == 0) return null;
            data = data.trim();
            if (data.charAt(0) =='{' && data.charAt(data.length() - 1) == '}') try {
                return new JSONObject(new JSONTokener(data));
            } catch (JSONException e) {
                return null;
            }
            return null;
        } catch (IOException e) {
        }
        return null;
    }
    public Set<String> getKeys() {
        if (this.request == null || this.request.getParameterMap().size() == 0) return this.qm.keySet();
        return request.getParameterMap().keySet();
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
    public HttpServletRequest getRequest() {
        return this.request;
    }
    public String toString() {
        if (this.qm == null) return "";
        Map<String, String> outcopy = new LinkedHashMap<>();
        this.qm.entrySet().stream()
            .filter(e -> !e.getKey().equals("password"))
            .filter(e -> !e.getKey().equals("asset"))
            .forEach(e -> outcopy.put(e.getKey(), new String(e.getValue(), StandardCharsets.UTF_8)));
        return outcopy.toString().replaceAll(", ", "&").replaceFirst("\\{", "").replaceAll("\\}", "").replaceAll(" ", "%20");
     }
}