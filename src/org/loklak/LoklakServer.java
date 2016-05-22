/**
 *  LoklakServer
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

package org.loklak;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.servlets.gzip.GzipHandler;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.loklak.api.p2p.Hello;
import org.loklak.api.server.AccessServlet;
import org.loklak.api.server.AppsServlet;
import org.loklak.api.server.AssetServlet;
import org.loklak.api.server.CampaignServlet;
import org.loklak.api.server.CrawlerServlet;
import org.loklak.api.server.DumpDownloadServlet;
import org.loklak.api.server.GeocodeServlet;
import org.loklak.api.server.PeersServlet;
import org.loklak.api.server.ProxyServlet;
import org.loklak.api.server.PushServlet;
import org.loklak.api.server.ShortlinkFromTweetServlet;
import org.loklak.api.server.UserServlet;
import org.loklak.api.server.push.GeoJsonPushServlet;
import org.loklak.api.server.SearchServlet;
import org.loklak.api.server.SettingsServlet;
import org.loklak.api.server.StatusServlet;
import org.loklak.api.server.SuggestServlet;
import org.loklak.api.server.AccountServlet;
import org.loklak.api.server.ThreaddumpServlet;
import org.loklak.api.server.ValidateServlet;
import org.loklak.api.server.push.FossasiaPushServlet;
import org.loklak.api.server.push.OpenWifiMapPushServlet;
import org.loklak.api.server.push.NodelistPushServlet;
import org.loklak.api.server.push.FreifunkNodePushServlet;
import org.loklak.api.server.push.NetmonPushServlet;
import org.loklak.api.server.ImportProfileServlet;
import org.loklak.data.DAO;
import org.loklak.harvester.TwitterScraper;
import org.loklak.http.RemoteAccess;
import org.loklak.server.FileHandler;
import org.loklak.tools.Browser;
import org.loklak.tools.OS;
import org.loklak.vis.server.MapServlet;
import org.loklak.vis.server.MarkdownServlet;


public class LoklakServer {
	
	private enum HttpsMode {

	    OFF(0),
	    ON(1),
	    REDIRECT(2),
	    ONLY(3);

	    private Integer mode;

	    HttpsMode(int mode) {
	        this.mode = mode;
	    }

	    public boolean equals(HttpsMode other) {return this.mode == other.mode;}
	    //public boolean isSmallerThan(HttpsMode other) {return this.mode < other.mode;}
	    //public boolean isSmallerOrEqualTo(HttpsMode other) {return this.mode <= other.mode;}
	    //public boolean isGreaterThan(HttpsMode other) {return this.mode > other.mode;}
	    public boolean isGreaterOrEqualTo(HttpsMode other) {return this.mode >= other.mode;}
	}
	
    public final static Set<String> blacklistedHosts = new ConcurrentHashSet<>();

    
    private static Server server = null;
    private static Caretaker caretaker = null;
    public  static QueuedIndexing queuedIndexing = null;
    private static DumpImporter dumpImporter = null;
    private static HttpsMode httpsMode = HttpsMode.OFF;
    
    public static Map<String, String> readConfig(Path data) throws IOException {
        File conf_dir = new File("conf");
        Properties prop = new Properties();
        prop.load(new FileInputStream(new File(conf_dir, "config.properties")));
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<Object, Object> entry: prop.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
        Path settings_dir = data.resolve("settings");
        settings_dir.toFile().mkdirs();
        OS.protectPath(settings_dir);
        File customized_config = new File(settings_dir.toFile(), "customized_config.properties");
        if (!customized_config.exists()) {
            BufferedWriter w = new BufferedWriter(new FileWriter(customized_config));
            w.write("# This file can be used to customize the configuration file conf/config.properties\n");
            w.close();
        }
        Properties customized_config_props = new Properties();
        customized_config_props.load(new FileInputStream(customized_config));
        for (Map.Entry<Object, Object> entry: customized_config_props.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
        return config;
    }
    
    public static int getServerThreads() {
        return server.getThreadPool().getThreads() - server.getThreadPool().getIdleThreads();
    }
    
    public static String getServerURI() {
        return server.getURI().toASCIIString();
    }
    
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
        
        // init config, log and elasticsearch
        Path data = FileSystems.getDefault().getPath("data");
        File dataFile = data.toFile();
        if (!dataFile.exists()) dataFile.mkdirs(); // should already be there since the start.sh script creates it

        // load the config file(s);
        Map<String, String> config = readConfig(data);
        
        // set localhost pattern
        String server_localhost = config.get("server.localhost");
        if (server_localhost != null && server_localhost.length() > 0) {
            for (String h: server_localhost.split(",")) RemoteAccess.addLocalhost(h);
        }
        
        // check for https modus
        String httpsString = config.get("https.mode");
        httpsMode = HttpsMode.OFF;
        if("on".equals(httpsString)) httpsMode = HttpsMode.ON;
        else if("redirect".equals(httpsString)) httpsMode = HttpsMode.REDIRECT;
        else if("only".equals(httpsString)) httpsMode = HttpsMode.ONLY;
        
        // get server ports
        Map<String, String> env = System.getenv();
        String httpPortS = config.get("port.http");
        int httpPort = httpPortS == null ? 9000 : Integer.parseInt(httpPortS);
        if(env.containsKey("PORT")) {
            httpPort = Integer.parseInt(env.get("PORT"));
        }
        String httpsPortS = config.get("port.https");
        int httpsPort = httpsPortS == null ? 9443 : Integer.parseInt(httpsPortS);
        if(env.containsKey("PORTSSL")) {
            httpsPort = Integer.parseInt(env.get("PORTSSL"));
        }
        
        // check if a loklak service is already running on configured port
        try{
        	checkServerPorts(httpPort, httpsPort);
        }
        catch(IOException e){
        	Log.getLog().warn(e.getMessage());
			System.exit(-1);
        }
        
        
        // prepare shutdown signal
        File pid = new File(dataFile, "loklak.pid");
        if (pid.exists()) pid.deleteOnExit(); // thats a signal for the stop.sh script that loklak has terminated
        
        // initialize all data        
        DAO.init(config, data);
        
        // init the http server
        try {
			setupHttpServer(httpPort, httpsPort);
		} catch (Exception e) {
			Log.getLog().warn(e.getMessage());
			System.exit(-1);
		}
        setServerHandler(dataFile);
        
        
        LoklakServer.server.start();
        LoklakServer.caretaker = new Caretaker();
        LoklakServer.caretaker.start();
        LoklakServer.queuedIndexing = new QueuedIndexing();
        LoklakServer.queuedIndexing.start();
        LoklakServer.dumpImporter = new DumpImporter(Integer.MAX_VALUE);
        LoklakServer.dumpImporter.start();
        
        
        // read upgrade interval
        Caretaker.upgradeTime = Caretaker.startupTime + DAO.getConfig("upgradeInterval", 86400000);
        
        // if this is not headless, we can open a browser automatically
        Browser.openBrowser("http://localhost:" + httpPort + "/");
        
        // ** services are now running **
        
        // start a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Log.getLog().info("catched main termination signal");
                    LoklakServer.dumpImporter.shutdown();
                    LoklakServer.queuedIndexing.shutdown();
                    LoklakServer.caretaker.shutdown();
                    LoklakServer.server.stop();
                    DAO.close();
                    TwitterScraper.executor.shutdown();
                    Harvester.executor.shutdown();
                    Log.getLog().info("main terminated, goodby.");
                } catch (Exception e) {
                }
            }
        });

        // ** wait for shutdown signal, do this with a kill HUP (default level 1, 'kill -1') signal **
        
        LoklakServer.server.join();
        Log.getLog().info("server terminated");
        
        // After this, the jvm processes all shutdown hooks and terminates then.
        // The main termination line is therefore inside the shutdown hook.
    }
    
    //initiate http server
    private static void setupHttpServer(int httpPort, int httpsPort) throws Exception{
    	QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMaxThreads(500);
        LoklakServer.server = new Server(pool);
        LoklakServer.server.setStopAtShutdown(true);
        
        //http
        if(!httpsMode.equals(HttpsMode.ONLY)){
	        HttpConfiguration http_config = new HttpConfiguration();
	        if(httpsMode.equals(HttpsMode.REDIRECT)) { //redirect
	        	http_config.addCustomizer(new SecureRequestCustomizer());
	        	http_config.setSecureScheme("https");
	        	http_config.setSecurePort(httpsPort);
	        }
	        
	        ServerConnector connector = new ServerConnector(LoklakServer.server);
	        connector.addConnectionFactory(new HttpConnectionFactory(http_config));
	        connector.setPort(httpPort);
	        connector.setName("httpd:" + httpPort);
	        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        LoklakServer.server.addConnector(connector);
        }
        
        //https
        //uncommented lines for http2 (jetty 9.3 / java 8)        
        if(httpsMode.isGreaterOrEqualTo(HttpsMode.ON)){
        	
        	String keySource = DAO.getConfig("https.keysource", "keystore");
        	String keystorePath = null;
        	String keystorePass = null;
        	String keystoreManagerPass = null;
        	
        	//check for key source. Can be a java keystore or in pem format (gets converted automatically)
        	if("keystore".equals(keySource)){
        		//use native keystore format
        		
        		File keystore = new File(DAO.conf_dir, DAO.getConfig("keystore.name", "keystore.jks"));
        		if(!keystore.exists() || !keystore.isFile() || !keystore.canRead()){
        			throw new Exception("Could not find keystore");
        		}
        		keystorePath = keystore.getAbsolutePath();
        		keystorePass = DAO.getConfig("keystore.password", "");
        		keystoreManagerPass = DAO.getConfig("keystore.password", "");
        	}
        	else if ("key-cert".equals(keySource)){
        		//use more common pem format as used by openssl
        		
        		//get key and cert
        		File key = new File(DAO.getConfig("https.key", ""));
        		if(!key.exists() || !key.isFile() || !key.canRead()){
        			throw new Exception("Could not find key file");
        		}
        		File cert = new File(DAO.getConfig("https.cert", ""));
        		if(!cert.exists() || !cert.isFile() || !cert.canRead()){
        			throw new Exception("Could not find cert file");
        		}
        		
        		
        		//generate random password
        		char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        		StringBuilder sb = new StringBuilder();
        		Random random = new Random();
        		for (int i = 0; i < 20; i++) {
        		    char c = chars[random.nextInt(chars.length)];
        		    sb.append(c);
        		}
        		keystorePass = keystoreManagerPass = sb.toString();
        		
        		
        		//temporary keystore files
        		String pkcs12_temp = DAO.conf_dir.getAbsolutePath() + "/keystore_temp.pkcs12";
        		keystorePath = DAO.conf_dir.getAbsolutePath() + "/keystore_temp.jks";
        		File temp = new File(pkcs12_temp);
        		if(temp.exists()) temp.delete();
        		temp = new File(keystorePath);
        		if(temp.exists()) temp.delete();
        		
        		//create temporary pkcs12 file from key and cert
        		Runtime rt = Runtime.getRuntime();
        		try{
        			Process p = rt.exec("openssl pkcs12 -export -out " + pkcs12_temp
        					+ " -in " + cert.getAbsolutePath()
        					+ " -inkey " + key.getAbsolutePath()
        					+ " -passout pass:" + keystorePass);
        			p.waitFor();
        		}
        		catch(IOException e){
        			throw new Exception("Key/Cert conversion failed");
        		}
        		
        		//import pkcs12 file into keystore
        		try{
        			Process p = rt.exec("keytool -importkeystore -noprompt" 
        					+ " -srckeystore " + pkcs12_temp
        					+ " -srcstorepass " + keystorePass
        					+ " -srcstoretype PKCS12"
        					+ " -destkeystore " + keystorePath
        					+ " -storepass " + keystorePass);
        			p.waitFor();
        		}
        		catch(IOException e){
        			throw new Exception("Import of temporary pkcs12 file failed");
        		}
        		finally{
        			//remove intermediate keystore
        			temp = new File(pkcs12_temp);
        			if (temp.exists()) temp.delete();
        		}
        		
        		//remove temporary java keystore on program exit
        		File keystore = new File(keystorePath);
        		if (keystore.exists()) keystore.deleteOnExit();
        		
        		
        		Log.getLog().info("Successfully imported keystore from key/cert files");
        	}
        	else{
        		throw new Exception("Invalid option for https.keysource");
        	}
        	        	
        	
        	HttpConfiguration https_config = new HttpConfiguration();
	        https_config.addCustomizer(new SecureRequestCustomizer());
	        
	        HttpConnectionFactory http1 = new HttpConnectionFactory(https_config);
	        //HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(https_config);
	        
	        //NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
	        //ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
	        //alpn.setDefaultProtocol(http1.getProtocol());
	         
	        SslContextFactory sslContextFactory = new SslContextFactory();
	        
	        sslContextFactory.setKeyStorePath(keystorePath);
	        sslContextFactory.setKeyStorePassword(keystorePass);
	        keystorePass = null;
	        sslContextFactory.setKeyManagerPassword(keystoreManagerPass);
	        //sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
	        //sslContextFactory.setUseCipherSuitesOrder(true);
	        
	        
	        //SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
	        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "http/1.1");
	        
	        //ServerConnector sslConnector = new ServerConnector(LoklakServer.server, ssl, alpn, http2, http1);
	        ServerConnector sslConnector = new ServerConnector(LoklakServer.server, ssl, http1);
	        sslConnector.setPort(httpsPort);
	        sslConnector.setName("httpd:" + httpsPort);
	        sslConnector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        LoklakServer.server.addConnector(sslConnector);
        }
    }
    
    private static void setServerHandler(File dataFile){
    	
    	
    	// create security handler for http auth and http-to-https redirects
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        
        boolean redirect = httpsMode.equals(HttpsMode.REDIRECT);
        boolean auth = "true".equals(DAO.getConfig("http.auth", "false"));
        
        if(redirect || auth){
        	
        	LoginService loginService = new HashLoginService("LoklakRealm", 
        			DAO.conf_dir.getAbsolutePath() + "/http_auth");
        	if(auth) LoklakServer.server.addBean(loginService);
        	
        	Constraint constraint = new Constraint();
        	if(redirect) constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
        	if(auth){
	        	constraint.setAuthenticate(true);
	            constraint.setRoles(new String[] { "user", "admin" });
        	}
        	
        	
        	//makes the constraint apply to all uri paths        
        	ConstraintMapping mapping = new ConstraintMapping();
        	mapping.setPathSpec( "/*" );
        	mapping.setConstraint(constraint);

        	securityHandler.addConstraintMapping(mapping);
        	
        	if(auth){
	        	securityHandler.setAuthenticator(new BasicAuthenticator());
	            securityHandler.setLoginService(loginService);
        	}
        	
        	if(redirect) Log.getLog().info("Activated http-to-https redirect");
        	if(auth) Log.getLog().info("Activated basic http auth");
        }
        
        // Setup IPAccessHandler for blacklists
        String blacklist = DAO.getConfig("server.blacklist", "");
        if (blacklist != null && blacklist.length() > 0) try {
            IPAccessHandler ipaccess = new IPAccessHandler();
            String[] bx = blacklist.split(",");
            ipaccess.setBlack(bx);
            for (String b: bx) {
                int p = b.indexOf('|');
                blacklistedHosts.add(p < 0 ? b : b.substring(0, p));
            }
            LoklakServer.server.setHandler(ipaccess);
        } catch (IllegalArgumentException e) {
            Log.getLog().warn("bad blacklist:" + blacklist, e);
        }
        
        WebAppContext htrootContext = new WebAppContext();
        htrootContext.setContextPath("/");

        File tmp = new File(dataFile, "tmp");
        ServletContextHandler servletHandler = new ServletContextHandler();
        FilterHolder filter = servletHandler.addFilter(GzipFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        filter.setInitParameter("mimeTypes", "text/plain");
        servletHandler.addServlet(DumpDownloadServlet.class, "/dump/*");
        servletHandler.addServlet(ShortlinkFromTweetServlet.class, "/x");
        servletHandler.addServlet(AccessServlet.class, "/api/access.json");
        servletHandler.addServlet(AccessServlet.class, "/api/access.html");
        servletHandler.addServlet(AccessServlet.class, "/api/access.txt");
        servletHandler.addServlet(AppsServlet.class, "/api/apps.json");
        servletHandler.addServlet(Hello.class, new Hello().getAPIPath() /*"/api/hello.json"*/);
        servletHandler.addServlet(PeersServlet.class, "/api/peers.json");
        servletHandler.addServlet(CrawlerServlet.class, "/api/crawler.json");
        servletHandler.addServlet(StatusServlet.class, "/api/status.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.rss");
        servletHandler.addServlet(SearchServlet.class, "/api/search.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.txt");
        servletHandler.addServlet(SuggestServlet.class, "/api/suggest.json");
        ServletHolder accountServletHolder = new ServletHolder(AccountServlet.class);
        accountServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(accountServletHolder, "/api/account.json");
        servletHandler.addServlet(UserServlet.class, "/api/user.json");
        servletHandler.addServlet(CampaignServlet.class, "/api/campaign.json");
        servletHandler.addServlet(ImportProfileServlet.class, "/api/import.json");
        servletHandler.addServlet(SettingsServlet.class, "/api/settings.json");
        servletHandler.addServlet(GeocodeServlet.class, "/api/geocode.json");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.gif");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.png");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.jpg");
        servletHandler.addServlet(ValidateServlet.class, "/api/validate.json");
        ServletHolder pushServletHolder = new ServletHolder(PushServlet.class);
        pushServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(pushServletHolder, "/api/push.json");
        ServletHolder geojsonPushServletHolder = new ServletHolder(GeoJsonPushServlet.class);
        geojsonPushServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(geojsonPushServletHolder, "/api/push/geojson.json");
        servletHandler.addServlet(FossasiaPushServlet.class, "/api/push/fossasia.json");
        servletHandler.addServlet(OpenWifiMapPushServlet.class, "/api/push/openwifimap.json");
        servletHandler.addServlet(NodelistPushServlet.class, "/api/push/nodelist.json");
        servletHandler.addServlet(FreifunkNodePushServlet.class, "/api/push/freifunknode.json");
        servletHandler.addServlet(NetmonPushServlet.class, "/api/push/netmon.xml");
        ServletHolder assetServletHolder = new ServletHolder(AssetServlet.class);
        assetServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(assetServletHolder, "/api/asset");
        servletHandler.addServlet(ThreaddumpServlet.class, "/api/threaddump.txt");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.gif");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.gif.base64");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.png");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.png.base64");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.jpg");
        servletHandler.addServlet(MarkdownServlet.class, "/vis/markdown.jpg.base64");
        servletHandler.addServlet(MapServlet.class, "/vis/map.gif");
        servletHandler.addServlet(MapServlet.class, "/vis/map.gif.base64");
        servletHandler.addServlet(MapServlet.class, "/vis/map.png");
        servletHandler.addServlet(MapServlet.class, "/vis/map.png.base64");
        servletHandler.addServlet(MapServlet.class, "/vis/map.jpg");
        servletHandler.addServlet(MapServlet.class, "/vis/map.jpg.base64");
        servletHandler.setMaxFormContentSize(10 * 1024 * 1024); // 10 MB

        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setShowStacks(true);
        servletHandler.setErrorHandler(errorHandler);
        
        FileHandler fileHandler = new FileHandler(Integer.parseInt(DAO.getConfig("www.expires","600")));
        fileHandler.setDirectoriesListed(true);
        fileHandler.setWelcomeFiles(new String[]{ "index.html" });
        fileHandler.setResourceBase(DAO.getConfig("www.path","html"));
        
        RewriteHandler rewriteHandler = new RewriteHandler();
        rewriteHandler.setRewriteRequestURI(true);
        rewriteHandler.setRewritePathInfo(false);
        rewriteHandler.setOriginalPathAttribute("originalPath"); // the attribute name where the original request is stored
        RewriteRegexRule rssSearchRule = new RewriteRegexRule();
        rssSearchRule.setRegex("/rss/(.*)");
        rssSearchRule.setReplacement("/search.rss?q=$1");
        rewriteHandler.addRule(rssSearchRule);
        rewriteHandler.setHandler(servletHandler);
        
        HandlerList handlerlist2 = new HandlerList();
        handlerlist2.setHandlers(new Handler[]{fileHandler, rewriteHandler, new DefaultHandler()});
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setHandler(handlerlist2);
        
        securityHandler.setHandler(gzipHandler);
        
        LoklakServer.server.setHandler(securityHandler);
    }
    
    private static void checkServerPorts(int httpPort, int httpsPort) throws IOException{
    	
    	// check http port
        if(!httpsMode.equals(HttpsMode.ONLY)){
	        ServerSocket ss = null;
	        try {
	            ss = new ServerSocket(httpPort);
	            ss.setReuseAddress(true);
	            ss.setReceiveBufferSize(65536);
	        } catch (IOException e) {
	            // the socket is already occupied by another service
	            throw new IOException("port " + httpPort + " is already occupied by another service, maybe another loklak is running on this port already. exit.");
	        } finally {
	            // close the socket again
	            if (ss != null) {try {ss.close();} catch (IOException e) {}}
	        }
        }
        
        // check https port
        if(httpsMode.isGreaterOrEqualTo(HttpsMode.ON)){
	        ServerSocket sss = null;
	        try {
	            sss = new ServerSocket(httpsPort);
	            sss.setReuseAddress(true);
	            sss.setReceiveBufferSize(65536);
	        } catch (IOException e) {
	            // the socket is already occupied by another service
	        	throw new IOException("port " + httpsPort + " is already occupied by another service, maybe another loklak is running on this port already. exit.");
	        } finally {
	            // close the socket again
	            if (sss != null) {try {sss.close();} catch (IOException e) {}}
	        }
        }
    }
}
