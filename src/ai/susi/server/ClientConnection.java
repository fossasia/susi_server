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

package ai.susi.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import ai.susi.DAO;

/**
 * Helper class to provide BufferedReader Objects for get and post connections
 */
public class ClientConnection {
    
    //public static String USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36";
    
    public  static final String CHARSET = "UTF-8";
    private static final byte LF = 10;
    private static final byte CR = 13;
    public static final byte[] CRLF = {CR, LF};

    private static RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(60000)
            .setConnectTimeout(60000)
            .setConnectionRequestTimeout(60000)
            .setContentCompressionEnabled(true)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build();

    private final static CloseableHttpClient httpClient = getClosableHttpClient();
    
    private int status;
    public BufferedInputStream inputStream;
    private HttpResponse httpResponse;
    
    private static class TrustAllHostNameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	/**
     * GET request
     * @param urlstring URL String to send request
     * @param useAuthentication Flag for using authentication
     * @throws IOException
     */
    public ClientConnection(String urlstring) throws IOException {
        HttpRequestBase request = new HttpGet(urlstring);
        request.setHeader("User-Agent", USER_AGENT);
        this.executeRequest(request);
    }
    
    /**
     * POST request
     * @param urlstring URL String to send request
     * @param map
     * @param useAuthentication Flag for using authentication
     * @throws ClientProtocolException 
     * @throws IOException
     */
    public ClientConnection(String urlstring, Map<String, byte[]> map) throws ClientProtocolException, IOException {
        HttpRequestBase request = new HttpPost(urlstring);        
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            entityBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        ((HttpPost) request).setEntity(entityBuilder.build());
        request.setHeader("User-Agent", USER_AGENT);
        this.executeRequest(request);
    }

    public final static CloseableHttpClient getClosableHttpClient() {
        boolean trustAllCerts = !"none".equals(DAO.getConfig("httpsclient.trustselfsignedcerts", "peers"))
                && ("all".equals(DAO.getConfig("httpsclient.trustselfsignedcerts", "peers")));
        return HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(getConnctionManager(trustAllCerts))
                .setDefaultRequestConfig(defaultRequestConfig)
                .setMaxConnPerRoute(200)
                .setMaxConnTotal(500)
                .build();
    }
    
    public InputStream getInputStream() {
        return this.inputStream;
    }
    
    private static HttpClientConnectionManager getConnctionManager(boolean trustAllCerts){

        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
        if(trustAllCerts){
            try {
                SSLConnectionSocketFactory trustSelfSignedSocketFactory = new SSLConnectionSocketFactory(
                            new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                            new TrustAllHostNameVerifier());
                socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory> create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", trustSelfSignedSocketFactory)
                        .build();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                DAO.severe(e);
            }
        }

        PoolingHttpClientConnectionManager cm = (trustAllCerts && socketFactoryRegistry != null) ?
                new PoolingHttpClientConnectionManager(socketFactoryRegistry):
                new PoolingHttpClientConnectionManager();

        // twitter specific options
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
        
        return cm;
    }

    private void executeRequest(HttpRequestBase request) throws IOException {

        this.httpResponse = null;
        HttpContext context = HttpClientContext.create();
        try {
            this.httpResponse = httpClient.execute(request, context);
        } catch (UnknownHostException e) {
            request.reset();
            throw new IOException("client connection failed: unknown host " + request.getURI().getHost());
        } catch (SocketTimeoutException e){
            request.reset();
            throw new IOException("client connection timeout for request: " + request.getURI());
        } catch (SSLHandshakeException e){
            request.reset();
            throw new IOException("client connection handshake error for domain " + request.getURI().getHost() + ": " + e.getMessage());
        } catch (Throwable e) {
            request.reset();
            throw new IOException("server fail: " + e.getMessage());
        }
        HttpEntity httpEntity = this.httpResponse.getEntity();
        if (httpEntity != null) {
            if (this.httpResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    this.inputStream = new BufferedInputStream(httpEntity.getContent());
                } catch (IOException e) {
                    request.reset();
                    throw e;
                }
                Map<String, List<String>> header = new HashMap<String, List<String>>();
                for (Header h: httpResponse.getAllHeaders()) {
                    List<String> vals = header.get(h.getName());
                    if (vals == null) { vals = new ArrayList<String>(); header.put(h.getName(), vals); }
                    vals.add(h.getValue());
                }
            } else {
                request.reset();
                throw new IOException("client connection to " + request.getURI() + " fail: " + status + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            request.reset();
            throw new IOException("client connection to " + request.getURI() + " fail: no connection");
        }
    }
    
    /**
     * get a redirect for an url: this method shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring URL String for redirection
     * @param useAuthentication Flag for authentication
     * @return the redirect url for the given urlstring
     * @throws IOException if the url is not redirected
     */
    public static String getRedirect(String urlstring, boolean useAuthentication) throws IOException {
        HttpGet get = new HttpGet(urlstring);
        get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        get.setHeader("User-Agent", USER_AGENT);
        CloseableHttpClient httpClient = HttpClients.custom()
        		.setConnectionManager(getConnctionManager(useAuthentication))
        		.setDefaultRequestConfig(defaultRequestConfig)
        		.build();
        HttpResponse httpResponse = httpClient.execute(get);
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 301) {
                for (Header header: httpResponse.getAllHeaders()) {
                    if (header.getName().equalsIgnoreCase("location")) {
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
    
    /**
     * get a redirect for an url: this method shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring URL String for redirection
     * @return
     * @throws IOException
     */
    public static String getRedirect(String urlstring) throws IOException {
    	return getRedirect(urlstring, true);
    }
    
    public void close() {
        if (this.httpResponse != null) {
            HttpEntity httpEntity = this.httpResponse.getEntity();
            if (httpEntity != null) EntityUtils.consumeQuietly(httpEntity);
            this.httpResponse = null;
        }
        if (this.inputStream != null) {
            try {this.inputStream.close();} catch (IOException e) {}
            this.inputStream = null;
        }
    }
    
    public void finalize() {
        this.close();
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
                	DAO.severe(e.getMessage());
                } finally {
                    os.close();
                }
            } catch (IOException e) {
            	DAO.severe(e.getMessage());
            } finally {
                connection.close();
            }
        } catch (IOException e) {
        	DAO.severe(e.getMessage());
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
            	DAO.severe(e.getMessage());
            } finally {
                connection.close();
            }
            return baos.toByteArray();
        } catch (IOException e) {
        	DAO.severe(e.getMessage());
            return null;
        }
    }
    
    public static void main(String[] args) {
        try {
            byte[] b = download("http://api.duckduckgo.com/?q=ford&format=json&pretty=1");
            System.out.println(new String(b, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
