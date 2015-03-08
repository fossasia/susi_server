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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage of a peer list which can be used for peer-to-peer communication.
 * This is a static class to provide access to all other objects easily.
 * We store the IPs here only temporary, not permanently.
 */
public class RemoteAccess {

    public static Map<String, RemoteAccess> history = new ConcurrentHashMap<String, RemoteAccess>();
    
    public static String log(String remoteHost, String localPath, Integer localHTTPPort, Integer localHTTPSPort) {
        if (localHTTPPort == null || localHTTPSPort == null) {
            // if port configuration is omitted, just update the value if it exist
            RemoteAccess ra = history.get(remoteHost);
            if (ra == null) {
                history.put(remoteHost, new RemoteAccess(remoteHost, localPath, localHTTPPort, localHTTPSPort));
            } else {
                assert ra.remoteHost.equals(remoteHost);
                ra.localPath = localPath;
                ra.accessTime = System.currentTimeMillis();
            }
        } else {
            // overwrite if new port configuration is submitted
            history.put(remoteHost, new RemoteAccess(remoteHost, localPath, localHTTPPort, localHTTPSPort));
        }
        return hostHash(remoteHost);
    }
    
    public static long latestVisit(String remoteHost) {
        RemoteAccess ra = history.get(remoteHost);
        return ra == null ? -1 : ra.accessTime;
    }
    
    public static String hostHash(String remoteHost) {
        return Integer.toHexString(Math.abs(remoteHost.hashCode()));
    }
    
    private String remoteHost, localPath;
    private int localHTTPPort, localHTTPSPort;
    private long accessTime;
    
    private RemoteAccess(String remoteHost, String localPath, Integer localHTTPPort, Integer localHTTPSPort) {
        this.remoteHost = remoteHost;
        this.localPath = localPath;
        this.localHTTPPort = localHTTPPort == null ? -1 : localHTTPPort.intValue();
        this.localHTTPSPort = localHTTPSPort == null ? -1 : localHTTPSPort.intValue();
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
    
    public static boolean isLocalhost(String host) {
        return "0:0:0:0:0:0:0:1".equals(host) || "fe80:0:0:0:0:0:0:1%1".equals(host) || "127.0.0.1".equals(host) || "localhost".equals(host);
        
    }
}
