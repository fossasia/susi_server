/**
 *  ClientHelper
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.loklak.LoklakServer;

/**
 * Helper class to provide BufferedReader Objects for get and post connections
 */
public class ClientConnection {

    public  static final String CHARSET = "UTF-8";
    private static final String CRLF = "\r\n";
    private static final String HYPHENS = "--";
    private static final String BOUNDARY =  "*****" + Long.toString(System.currentTimeMillis()); // pseudo-random boundary string

    public HttpURLConnection con;
    public int status;
    public InputStream inputStream;
    public Map<String, List<String>> header;
    
    /**
     * GET request
     * @param urlstring
     * @throws IOException
     */
    public ClientConnection(String urlstring) throws IOException {
        URL url = new URL(urlstring);
        this.con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(10000 /* milliseconds */);
        con.setConnectTimeout(15000 /* milliseconds */);
        con.setRequestMethod("GET");
        con.setUseCaches(false);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("User-Agent", LoklakServer.USER_AGENT);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.connect();
        this.status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            this.inputStream = con.getInputStream();
            this.header = con.getHeaderFields();
        } else {
            throw new IOException("server fail: " + status + ": " + con.getResponseMessage());
        }
    }
    
    /**
     * POST request
     * @param urlstring
     * @param map
     * @throws IOException
     */
    public ClientConnection(String urlstring, Map<String, byte[]> map) throws IOException {
        // This may be done actually more elegant using the apache hc library.
        // However, this is sufficient and not too bloated to send http POST 'manually'
        // (there is no other way when using only core java classes)
        URL url = new URL(urlstring);
        this.con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(10000 /* milliseconds */);
        con.setConnectTimeout(15000 /* milliseconds */);
        con.setRequestMethod("POST");
        con.setUseCaches(false);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("User-Agent", LoklakServer.USER_AGENT);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
        
        // write form elements
        DataOutputStream os = new DataOutputStream(con.getOutputStream());
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            os.writeBytes(HYPHENS); os.writeBytes(BOUNDARY); os.writeBytes(CRLF);
            os.writeBytes("Content-Disposition: form-data; name=\""); os.writeBytes(entry.getKey()); os.writeBytes("\""); os.writeBytes(CRLF);
            os.writeBytes("Content-Type: text/plain; charset="); os.writeBytes(CHARSET); os.writeBytes(CRLF);
            os.writeBytes(CRLF);
            os.write(entry.getValue()); os.writeBytes(CRLF);
            os.flush();
        }
        
        // finish and return response
        os.writeBytes(CRLF);
        os.writeBytes(HYPHENS); os.writeBytes(BOUNDARY); os.writeBytes(HYPHENS); os.writeBytes(CRLF);
        os.flush();
        os.close();

        this.status = con.getResponseCode();
        this.header = con.getHeaderFields();
        if (this.status == HttpURLConnection.HTTP_OK) {
            this.inputStream = con.getInputStream();
        } else {
            throw new IOException("server fail: " + status + ": " + con.getResponseMessage());
        }
            
    }
    
    public void close() {
        try {this.inputStream.close();} catch (IOException e) {}
        try {this.con.disconnect();} catch (Throwable e) {}
    }
    
}
