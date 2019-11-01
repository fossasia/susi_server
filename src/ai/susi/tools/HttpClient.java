/**
 *  ClientConnection
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

package ai.susi.tools;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class HttpClient {
    


    public static final int defaultClientTimeout = 6000;
    public static final int minimumLocalDeltaInit  =  10; // the minimum time difference between access of the same local domain
    public static final int minimumGlobalDeltaInit = 500; // the minimum time difference between access of the same global domain
    
    public static class Agent {
        public final String userAgent;    // the name that is send in http request to identify the agent
        public final String[] robotIDs;   // the name that is used in robots.txt to identify the agent
        public final int    minimumDelta; // the minimum delay between two accesses
        public Agent(final String userAgent, final String[] robotIDs, final int minimumDelta, final int clientTimeout) {
            this.userAgent = userAgent;
            this.robotIDs = robotIDs;
            this.minimumDelta = minimumDelta;
        }
    }
    
    private final static String[] browserAgents = new String[]{ // fake browser user agents are NOT AVAILABLE IN P2P OPERATION, only on special customer configurations (commercial users demanded this, I personally think this is inadvisable)
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/536.30.1 (KHTML, like Gecko) Version/6.0.5 Safari/536.30.1",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.71 Safari/537.36",
        "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36",
        "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:22.0) Gecko/20100101 Firefox/22.0",
        "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0",
        "Mozilla/5.0 (Windows NT 5.1; rv:22.0) Gecko/20100101 Firefox/22.0",
        "Mozilla/5.0 (Windows NT 6.1; rv:22.0) Gecko/20100101 Firefox/22.0",
        "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36",
        "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:21.0) Gecko/20100101 Firefox/21.0",
        "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"
        };
    private static final Random random = new Random(System.currentTimeMillis());
    private static Map<String, Agent> agents = new ConcurrentHashMap<String, Agent>();
    public final static String yacyInternetCrawlerAgentName = "YaCy Internet (cautious)";
    public static Agent yacyInternetCrawlerAgent = null; // defined later in static
    public final static String yacyIntranetCrawlerAgentName = "YaCy Intranet (greedy)";
    public static Agent yacyIntranetCrawlerAgent = null; // defined later in static
    public final static String googleAgentName = "Googlebot";
    public final static Agent googleAgentAgent = new Agent("Googlebot/2.1 (+http://www.google.com/bot.html)", new String[]{"Googlebot", "Googlebot-Mobile"}, minimumGlobalDeltaInit / 2, defaultClientTimeout);
    public final static String yacyProxyAgentName = "YaCyProxy";
    public final static Agent yacyProxyAgent = new Agent("yacy - this is a proxy access through YaCy from a browser, not a robot (the yacy bot user agent is 'yacybot')", new String[]{"yacy"}, minimumGlobalDeltaInit, defaultClientTimeout);
    public final static String customAgentName = "Custom Agent";
    public final static String browserAgentName = "Random Browser";
    public static Agent browserAgent;
    
    static {
        generateYaCyBot("new");
        browserAgent = new Agent(browserAgents[random.nextInt(browserAgents.length)], new String[]{"Mozilla"}, minimumLocalDeltaInit, defaultClientTimeout);
        agents.put(googleAgentName, googleAgentAgent);
        agents.put(browserAgentName, browserAgent);
        agents.put(yacyProxyAgentName, yacyProxyAgent);
    }
    
    /**
     * provide system information (this is part of YaCy protocol)
     */
    public static final String yacySystem = System.getProperty("os.arch", "no-os-arch") + " " +
            System.getProperty("os.name", "no-os-name") + " " + System.getProperty("os.version", "no-os-version") +
            "; " + "java " + System.getProperty("java.version", "no-java-version") + "; " + generateLocation();

    /**
     * produce a YaCy user agent string
     * @param addinfo
     * @return
     */
    public static void generateYaCyBot(String addinfo) {
        String agentString = "yacybot (v2 " + addinfo + "; " + yacySystem  + ") http://yacy.net/bot.html";
        yacyInternetCrawlerAgent = new Agent(agentString, new String[]{"yacybot"}, minimumGlobalDeltaInit, defaultClientTimeout);
        yacyIntranetCrawlerAgent = new Agent(agentString, new String[]{"yacybot"}, minimumLocalDeltaInit, defaultClientTimeout); // must have the same userAgent String as the web crawler because this is also used for snippets
        agents.put(yacyInternetCrawlerAgentName, yacyInternetCrawlerAgent);
        agents.put(yacyIntranetCrawlerAgentName, yacyIntranetCrawlerAgent);
    }
    
    public static void generateCustomBot(String name, String string, int minimumdelta, int clienttimeout) {
        if (name.toLowerCase().indexOf("yacy") >= 0 || string.toLowerCase().indexOf("yacy") >= 0) return; // don't allow 'yacy' in custom bot strings
        String agentString = string.replace("$$SYSTEM$$", yacySystem.replace("java", "O"));
        agents.put(customAgentName, new Agent(agentString, new String[]{name}, minimumdelta, clienttimeout));
    }

    /**
     * get the default agent
     * @param newagent
     */
    public static Agent getAgent(String agentName) {
        if (agentName == null || agentName.length() == 0) return yacyInternetCrawlerAgent;
        Agent agent = agents.get(agentName);
        return agent == null ? yacyInternetCrawlerAgent : agent;
    }
    
    /**
     * generating the location string
     * 
     * @return
     */
    public static String generateLocation() {
        String loc = System.getProperty("user.timezone", "nowhere");
        final int p = loc.indexOf('/');
        if (p > 0) {
            loc = loc.substring(0, p);
        }
        loc = loc + "/" + System.getProperty("user.language", "dumb");
        return loc;
    }

    /**
     * gets the location out of the user agent
     * 
     * location must be after last ; and before first )
     * 
     * @param userAgent in form "useragentinfo (some params; _location_) additional info"
     * @return
     */
    public static String parseLocationInUserAgent(final String userAgent) {
        final String location;

        final int firstOpenParenthesis = userAgent.indexOf('(');
        final int lastSemicolon = userAgent.lastIndexOf(';');
        final int firstClosedParenthesis = userAgent.indexOf(')');

        if (lastSemicolon < firstClosedParenthesis) {
            // ; Location )
            location = (firstClosedParenthesis > 0) ? userAgent.substring(lastSemicolon + 1, firstClosedParenthesis)
                    .trim() : userAgent.substring(lastSemicolon + 1).trim();
        } else {
            if (firstOpenParenthesis < userAgent.length()) {
                if (firstClosedParenthesis > firstOpenParenthesis) {
                    // ( Location )
                    location = userAgent.substring(firstOpenParenthesis + 1, firstClosedParenthesis).trim();
                } else {
                    // ( Location <end>
                    location = userAgent.substring(firstOpenParenthesis + 1).trim();
                }
            } else {
                location = "";
            }
        }

        return location;
    }

    public  static final String CHARSET = "UTF-8";
    private static final byte LF = 10;
    private static final byte CR = 13;
    public static final byte[] CRLF = {CR, LF};

    public final static RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(defaultClientTimeout)
            .setConnectTimeout(defaultClientTimeout)
            .setConnectionRequestTimeout(defaultClientTimeout)
            .setContentCompressionEnabled(true)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build();

    private int status;
    public BufferedInputStream inputStream;
    private Map<String, List<String>> response_header;
    private CloseableHttpClient httpClient;
    private HttpRequestBase request;
    private HttpResponse httpResponse;
    private ContentType contentType;

    private static class TrustAllHostNameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * GET request
     * @param urlstring
     * @param useAuthentication
     * @throws IOException
     */
    public HttpClient(String urlstring, boolean useAuthentication) throws IOException {
        this.httpClient = HttpClients.custom()
            .useSystemProperties()
            .setConnectionManager(getConnctionManager(useAuthentication))
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();
        this.request = new HttpGet(urlstring);
        this.request.setHeader("User-Agent", getAgent(yacyInternetCrawlerAgentName).userAgent);
        this.init();
    }

    /**
     * GET request
     * @param urlstring
     * @throws IOException
     */
    public HttpClient(String urlstring) throws IOException {
        this(urlstring, true);
    }

    /**
     * POST request
     * @param urlstring
     * @param map
     * @param useAuthentication
     * @throws ClientProtocolException 
     * @throws IOException
     */
    public HttpClient(String urlstring, Map<String, byte[]> map, boolean useAuthentication) throws ClientProtocolException, IOException {
        this.httpClient = HttpClients.custom()
            .useSystemProperties()
            .setConnectionManager(getConnctionManager(useAuthentication))
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();
        this.request = new HttpPost(urlstring);        
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, byte[]> entry: map.entrySet()) {
            entityBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        ((HttpPost) this.request).setEntity(entityBuilder.build());
        this.request.setHeader("User-Agent", getAgent(yacyInternetCrawlerAgentName).userAgent);
        this.init();
    }

    public HttpClient setHeader(String key, String value) {
        this.request.setHeader(key, value);
        return this;
    }

    /**
     * POST request
     * @param urlstring
     * @param map
     * @throws ClientProtocolException
     * @throws IOException
     */
    public HttpClient(String urlstring, Map<String, byte[]> map) throws ClientProtocolException, IOException {
        this(urlstring, map, true);
    }

    private static SSLConnectionSocketFactory getSSLSocketFactory() {
        final TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                    throws CertificateException {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                    throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
        } catch (final NoSuchAlgorithmException e) {
            // should not happen
            e.printStackTrace();
        } catch (final KeyManagementException e) {
            // should not happen
            e.printStackTrace();
        }

        final SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(
                sslContext,
                new NoopHostnameVerifier());
        return sslSF;
    }

    public static PoolingHttpClientConnectionManager getConnctionManager(boolean useAuthentication){

        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
        /*
        SSLConnectionSocketFactory trustSelfSignedSocketFactory = new SSLConnectionSocketFactory(
                    new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    new TrustAllHostNameVerifier());
        */
        socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", getSSLSocketFactory())
                .build();
        
        PoolingHttpClientConnectionManager cm = socketFactoryRegistry != null ? 
                new PoolingHttpClientConnectionManager(socketFactoryRegistry):
                new PoolingHttpClientConnectionManager();

        // twitter specific options
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
        HttpHost twitter = new HttpHost("twitter.com", 443);
        cm.setMaxPerRoute(new HttpRoute(twitter), 50);

        return cm;
    }

    private void init() throws IOException {

        this.httpResponse = null;
        try {
            this.httpResponse = httpClient.execute(this.request);
        } catch (UnknownHostException e) {
            this.request.releaseConnection();
            throw new IOException("client connection failed: unknown host " + this.request.getURI().getHost());
        } catch (SocketTimeoutException e){
            this.request.releaseConnection();
            throw new IOException("client connection timeout for request: " + this.request.getURI());
        } catch (SSLHandshakeException e){
            this.request.releaseConnection();
            throw new IOException("client connection handshake error for domain " + this.request.getURI().getHost() + ": " + e.getMessage());
        }
        HttpEntity httpEntity = this.httpResponse.getEntity();
        this.contentType = ContentType.get(httpEntity);
        if (httpEntity != null) {
            if (this.httpResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    this.inputStream = new BufferedInputStream(httpEntity.getContent());
                } catch (IOException e) {
                    this.request.releaseConnection();
                    throw e;
                }
                this.response_header = new HashMap<String, List<String>>();
                for (Header header: httpResponse.getAllHeaders()) {
                    List<String> vals = this.response_header.get(header.getName());
                    if (vals == null) { vals = new ArrayList<String>(); this.response_header.put(header.getName(), vals); }
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

    public ContentType getContentType() {
        return this.contentType == null ? ContentType.DEFAULT_BINARY : this.contentType;
    }

    /**
     * get a redirect for an url: this method shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring
     * @param useAuthentication
     * @return the redirect url for the given urlstring
     * @throws IOException if the url is not redirected
     */
    public static String getRedirect(String urlstring, boolean useAuthentication) throws IOException {
        HttpGet get = new HttpGet(urlstring);
        get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        get.setHeader("User-Agent", getAgent(yacyInternetCrawlerAgentName).userAgent);
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
     * @param urlstring
     * @return
     * @throws IOException
     */
    public static String getRedirect(String urlstring) throws IOException {
        return getRedirect(urlstring, true);
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

    public static void download(String source_url, File target_file, boolean useAuthentication) {
        try {
            HttpClient connection = new HttpClient(source_url, useAuthentication);
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

    public static class Response {

        private Map<String, String> request_header;
        private Map<String, List<String>> response_header;
        private byte[] data;

        public Response(String source_url, Map<String, String> request_header) throws IOException {
            this.request_header = request_header;
            final List<byte[]> content = new ArrayList<>(1);
            final List<IOException> exception = new ArrayList<>(1);
            Thread loadThread = new Thread() {
                public void run() {
                    try {
                        final HttpClient connection = new HttpClient(source_url);
                        if (request_header != null) {
                            request_header.forEach((key, value) -> connection.setHeader(key, value));
                        }
                        byte[] c = connection.load();
                        Response.this.response_header = connection.response_header;
                        if (c != null) content.add(c);
                    } catch (IOException e) {
                        exception.add(e);
                    }
                }
            };
            loadThread.start();
            try {
                loadThread.join(defaultClientTimeout);
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            }
            if (loadThread.isAlive()) loadThread.interrupt();
            if (!exception.isEmpty()) throw exception.get(0);
            if (content.isEmpty()) {
                throw new IOException("no content available for url " + source_url);
            }
            this.data = content.get(0);
        }

        public byte[] getData() {
            return this.data;
        }

        public Map<String, String> getRequest() {
            return this.request_header;
        }

        public Map<String, List<String>> getResponse() {
            return this.response_header;
        }
    }

    public static void load(String source_url, File target_file) {
        download(source_url, target_file, true);
    }

    /**
     * make GET request
     * @param source_url
     * @return the response 
     * @throws IOException
     */
    public static byte[] loadGet(String source_url) throws IOException {
        return loadGet(source_url, null);
    }

    public static byte[] loadGet(String source_url, Map<String, String> request_header) throws IOException {
        Response response = new Response(source_url, request_header);
        return response.getData();
    }

    /**
     * make POST request
     * @param source_url
     * @param post
     * @return the response
     * @throws IOException
     */
    public static byte[] loadPost(String source_url, Map<String, byte[]> post) throws IOException {
        HttpClient connection = new HttpClient(source_url, post);
        return connection.load();
    }

    private byte[] load() throws IOException {
        if (this.inputStream == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        byte[] buffer = new byte[2048];
        try {
            while ((count = this.inputStream.read(buffer)) > 0) baos.write(buffer, 0, count);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.close();
        }
        return baos.toByteArray();
    }

    public static JSONArray loadGetJSONArray(String source_url) throws IOException {
        byte[] b = loadGet(source_url);
        return new JSONArray(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }
    public static JSONArray loadPostJSONArray(String source_url, Map<String, byte[]> params) throws IOException {
        byte[] b = loadPost(source_url, params);
        return new JSONArray(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }

    public static JSONObject loadGetJSONObject(String source_url) throws IOException {
        byte[] b = loadGet(source_url);
        return new JSONObject(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }
    public static JSONObject loadPostJSONObject(String source_url, Map<String, byte[]> params) throws IOException {
        byte[] b = loadPost(source_url, params);
        return new JSONObject(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }

}
