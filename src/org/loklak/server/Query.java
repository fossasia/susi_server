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


package org.loklak.server;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.LoklakServer;
import org.loklak.data.DAO;
import org.loklak.http.AccessTracker;
import org.loklak.http.RemoteAccess;
import org.loklak.tools.DateParser;
import org.loklak.tools.UTF8;

public class Query {
    
    private HttpServletRequest request;
    private Map<String, String> qm;
    public AccessTracker.Track track;
    
    public Query(final HttpServletRequest request) {
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
        this.track.setDoSBlackout(LoklakServer.blacklistedHosts.contains(clientHost) || (!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.blackout", 100))));
        this.track.setDoSServicereduction(!this.track.isLocalhostAccess() && (this.track.getTimeSinceLastAccess() < DAO.getConfig("DoS.servicereduction", 1000)));
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
    public long get(String key, long dflt) {
        String val = qm == null ? request.getParameter(key) : qm.get(key);
        return val == null || val.length() == 0 ? dflt : Long.parseLong(val);
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
    public Set<String> getKeys() {
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

    public HttpServletRequest getRequest(){ return this.request; }
}