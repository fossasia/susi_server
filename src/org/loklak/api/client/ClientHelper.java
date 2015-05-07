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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.loklak.Main;

/**
 * Helper class to provide BufferedReader Objects for get and post connections
 */
public class ClientHelper {

    private static final String CRLF = "\r\n";
    private static final String HYPHENS = "--";
    private static final String CHARSET = "UTF-8";
    private static final String BOUNDARY =  "*****" + Long.toString(System.currentTimeMillis()); // pseudo-random boundary string
    
    public static BufferedReader getConnection(String urlstring) throws IOException {
        URL url = new URL(urlstring);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(10000 /* milliseconds */);
        con.setConnectTimeout(15000 /* milliseconds */);
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setRequestProperty("User-Agent", Main.USER_AGENT);
        con.connect();
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            return new BufferedReaderConnection(new InputStreamReader(con.getInputStream(), CHARSET), con);
        } else {
            throw new IOException("server fail: " + status + ": " + con.getResponseMessage());
        }
    }
    
    public static BufferedReader postConnection(String urlstring, Map<String, byte[]> map) throws IOException {
        // This may be done actually more elegant using the apache hc library.
        // However, this is sufficient and not too bloated to send http POST 'manually'
        // (there is no other way when using only core java classes)
        URL url = new URL(urlstring);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(10000 /* milliseconds */);
        con.setConnectTimeout(15000 /* milliseconds */);
        con.setRequestMethod("POST");
        con.setUseCaches(false);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("User-Agent", Main.USER_AGENT);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
        // write form elements
        DataOutputStream os = new DataOutputStream(con.getOutputStream());
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            formEntry(os, entry.getKey(), entry.getValue());
        }
        // finish and return response
        return finish(os, con);
    }
    
    private static void formEntry(DataOutputStream os, String key, byte[] value) throws IOException {
        os.writeBytes(HYPHENS); os.writeBytes(BOUNDARY); os.writeBytes(CRLF);
        os.writeBytes("Content-Disposition: form-data; name=\""); os.writeBytes(key); os.writeBytes("\""); os.writeBytes(CRLF);
        os.writeBytes("Content-Type: text/plain; charset="); os.writeBytes(CHARSET); os.writeBytes(CRLF);
        os.writeBytes(CRLF);
        os.write(value); os.writeBytes(CRLF);
        os.flush();
    }
    
    private static BufferedReaderConnection finish(DataOutputStream os, HttpURLConnection con) throws IOException {
        os.writeBytes(CRLF);
        os.writeBytes(HYPHENS); os.writeBytes(BOUNDARY); os.writeBytes(HYPHENS); os.writeBytes(CRLF);
        os.flush();
        os.close();
 
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            return new BufferedReaderConnection(new InputStreamReader(con.getInputStream(), CHARSET), con);
        } else {
            throw new IOException("server fail: " + status + ": " + con.getResponseMessage());
        }
    }
    
    /**
     * a buffered reader which knows the creating HttpURLConnection and closes that
     * connection if the reader is closed
     */
    public static class BufferedReaderConnection extends BufferedReader {
        HttpURLConnection con;
        public BufferedReaderConnection(Reader in, HttpURLConnection con) {
            super(in);
            this.con = con;
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            this.con.disconnect();
        }
    }
    
}
