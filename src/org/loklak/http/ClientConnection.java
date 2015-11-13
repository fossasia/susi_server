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

package org.loklak.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Helper class to provide BufferedReader Objects for get and post connections
 */
public class ClientConnection {

    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    
    public  static final String CHARSET = "UTF-8";
    private static final byte LF = 10;
    private static final byte CR = 13;
    public static final byte[] CRLF = {CR, LF};

    private static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private static CloseableHttpClient httpClient;
    
    public int status;
    public BufferedInputStream inputStream;
    public Map<String, List<String>> header;
    
    
    static {
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
        HttpHost twitter = new HttpHost("twitter.com", 443);
        cm.setMaxPerRoute(new HttpRoute(twitter), 50);
        RequestConfig defaultRequestConfig = RequestConfig.custom()
             .setSocketTimeout(3000)
             .setConnectTimeout(3000)
             .setConnectionRequestTimeout(3000)
             .build();
        httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(defaultRequestConfig).build();
    }
    
    /**
     * GET request
     * @param urlstring
     * @throws IOException
     */
    public ClientConnection(String urlstring) throws IOException {
        HttpGet get = new HttpGet(urlstring);
        get.setHeader("User-Agent", USER_AGENT);
        this.init(get);
    }
    
    /**
     * POST request
     * @param urlstring
     * @param map
     * @throws IOException
     */
    public ClientConnection(String urlstring, Map<String, byte[]> map) throws IOException {
        HttpPost post = new HttpPost(urlstring);        
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            entityBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        post.setEntity(entityBuilder.build());
        post.setHeader("User-Agent", USER_AGENT);
        this.init(post);
    }

    private void init(HttpUriRequest request) throws IOException {
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
        } catch (UnknownHostException e) {
            throw new IOException(e.getMessage());
        }
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                this.inputStream = new BufferedInputStream(httpEntity.getContent());
                this.header = new HashMap<String, List<String>>();
                for (Header header: httpResponse.getAllHeaders()) {
                    List<String> vals = this.header.get(header.getName());
                    if (vals == null) { vals = new ArrayList<String>(); this.header.put(header.getName(), vals); }
                    vals.add(header.getValue());
                }
            } else {
                throw new IOException("client connection to " + request.getURI() + " fail: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            throw new IOException("client connection to " + request.getURI() + " fail: no connection");
        }
    }
    
    /**
     * get a redirect for an url: this mehtod shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring
     * @return the redirect url for the given urlstring
     * @throws IOException if the url is not redirected
     */
    public static String getRedirect(String urlstring) throws IOException {
        
        
        
        HttpGet get = new HttpGet(urlstring);
        get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        get.setHeader("User-Agent", USER_AGENT);
        HttpResponse httpResponse = httpClient.execute(get);
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 301) {
                for (Header header: httpResponse.getAllHeaders()) {
                    if (header.getName().toLowerCase().equals("location")) return header.getValue();
                }
                throw new IOException("redirect for  " + urlstring+ ": no location attribute found");
            } else {
                throw new IOException("no redirect for  " + urlstring+ " fail: " + httpResponse.getStatusLine().getStatusCode() + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            throw new IOException("client connection to " + urlstring + " fail: no connection");
        }
    }
    
    public void close() {
        try {this.inputStream.close();} catch (IOException e) {}
    }
    
    public static void download(String source_url, File target_file) throws IOException {
        byte[] buffer = new byte[2048];
        ClientConnection connection = new ClientConnection(source_url);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(target_file));
        int count;
        try {
            while ((count = connection.inputStream.read(buffer)) > 0) os.write(buffer, 0, count);
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.close();
        os.close();
    }
    
    public static byte[] download(String source_url) throws IOException {
        byte[] buffer = new byte[2048];
        ClientConnection connection = new ClientConnection(source_url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        try {
            while ((count = connection.inputStream.read(buffer)) > 0) baos.write(buffer, 0, count);
        } catch (IOException e) {}
        connection.close();
        return baos.toByteArray();
    }
}
