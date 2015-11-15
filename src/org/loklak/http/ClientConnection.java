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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

/**
 * Helper class to provide BufferedReader Objects for get and post connections
 */
public class ClientConnection {

    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    
    public  static final String CHARSET = "UTF-8";
    private static final byte LF = 10;
    private static final byte CR = 13;
    public static final byte[] CRLF = {CR, LF};

    public static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private static RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .setContentCompressionEnabled(true)
            .build();
    
    private int status;
    public BufferedInputStream inputStream;
    private Map<String, List<String>> header;
    private CloseableHttpClient httpClient;
    private HttpRequestBase request;
    private HttpResponse httpResponse;
    
    
    static {
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
        HttpHost twitter = new HttpHost("twitter.com", 443);
        cm.setMaxPerRoute(new HttpRoute(twitter), 50);
    }
    
    /**
     * GET request
     * @param urlstring
     * @throws IOException
     */
    public ClientConnection(String urlstring) throws IOException {
        this.httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(defaultRequestConfig).build();
        this.request = new HttpGet(urlstring);
        this.request.setHeader("User-Agent", USER_AGENT);
        this.init();
    }
    
    /**
     * POST request
     * @param urlstring
     * @param map
     * @throws ClientProtocolException 
     * @throws IOException
     */
    public ClientConnection(String urlstring, Map<String, byte[]> map) throws ClientProtocolException, IOException {
        this.httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(defaultRequestConfig).build();
        this.request = new HttpPost(urlstring);        
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            entityBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        ((HttpPost) this.request).setEntity(entityBuilder.build());
        this.request.setHeader("User-Agent", USER_AGENT);
        this.init();
    }

    private void init() throws IOException {
        this.httpResponse = null;
        try {
            this.httpResponse = httpClient.execute(this.request);
        } catch (UnknownHostException e) {
            this.request.releaseConnection();
            throw new IOException(e.getMessage());
        }
        HttpEntity httpEntity = this.httpResponse.getEntity();
        if (httpEntity != null) {
            if (this.httpResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    this.inputStream = new BufferedInputStream(httpEntity.getContent());
                } catch (IOException e) {
                    this.request.releaseConnection();
                    throw e;
                }
                this.header = new HashMap<String, List<String>>();
                for (Header header: httpResponse.getAllHeaders()) {
                    List<String> vals = this.header.get(header.getName());
                    if (vals == null) { vals = new ArrayList<String>(); this.header.put(header.getName(), vals); }
                    vals.add(header.getValue());
                }
            } else {
                this.request.releaseConnection();
                throw new IOException("client connection to " + this.request.getURI() + " fail: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            this.request.releaseConnection();
            throw new IOException("client connection to " + this.request.getURI() + " fail: no connection");
        }
    }
    
    /**
     * get a redirect for an url: this method shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring
     * @return the redirect url for the given urlstring
     * @throws IOException if the url is not redirected
     */
    public static String getRedirect(String urlstring) throws IOException {
        HttpGet get = new HttpGet(urlstring);
        get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        get.setHeader("User-Agent", USER_AGENT);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(defaultRequestConfig).build();
        HttpResponse httpResponse = httpClient.execute(get);
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 301) {
                for (Header header: httpResponse.getAllHeaders()) {
                    if (header.getName().toLowerCase().equals("location")) {
                        EntityUtils.consumeQuietly(httpEntity);
                        return header.getValue();
                    }
                }
                EntityUtils.consumeQuietly(httpEntity);
                throw new IOException("redirect for  " + urlstring+ ": no location attribute found");
            } else {
                EntityUtils.consumeQuietly(httpEntity);
                throw new IOException("no redirect for  " + urlstring+ " fail: " + httpResponse.getStatusLine().getStatusCode() + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            throw new IOException("client connection to " + urlstring + " fail: no connection");
        }
    }
    
    public void close() {
        HttpEntity httpEntity = this.httpResponse.getEntity();
        if (httpEntity != null) EntityUtils.consumeQuietly(httpEntity);
        try {
            this.inputStream.close();
        } catch (IOException e) {} finally {
            this.request.releaseConnection();
        }
    }
    
    public static void download(String source_url, File target_file) {
        try {
            ClientConnection connection = new ClientConnection(source_url);
            try {
                OutputStream os = new BufferedOutputStream(new FileOutputStream(target_file));
                int count;
                byte[] buffer = new byte[2048];
                try {
                    while ((count = connection.inputStream.read(buffer)) > 0) os.write(buffer, 0, count);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static byte[] download(String source_url) throws IOException {
        try {
            ClientConnection connection = new ClientConnection(source_url);
            if (connection.inputStream == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int count;
            byte[] buffer = new byte[2048];
            try {
                while ((count = connection.inputStream.read(buffer)) > 0) baos.write(buffer, 0, count);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
