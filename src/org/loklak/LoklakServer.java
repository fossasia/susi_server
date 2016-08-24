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

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.apache.logging.log4j.LogManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
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
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.loklak.api.admin.AccessServlet;
import org.loklak.api.admin.CampaignServlet;
import org.loklak.api.admin.CrawlerServlet;
import org.loklak.api.admin.SettingsServlet;
import org.loklak.api.admin.StatusService;
import org.loklak.api.admin.ThreaddumpServlet;
import org.loklak.api.amazon.AmazonProductService;
import org.loklak.api.cms.*;
import org.loklak.api.geo.GeocodeServlet;
import org.loklak.api.iot.FossasiaPushServlet;
import org.loklak.api.iot.FreifunkNodePushServlet;
import org.loklak.api.iot.FreifunkNodeFetchServlet;
import org.loklak.api.iot.NMEAServlet;
import org.loklak.api.iot.NOAAAlertServlet;
import org.loklak.api.iot.GeoJsonPushServlet;
import org.loklak.api.iot.ImportProfileServlet;
import org.loklak.api.iot.NetmonPushServlet;
import org.loklak.api.iot.NodelistPushServlet;
import org.loklak.api.iot.OpenWifiMapPushServlet;
import org.loklak.api.iot.StuffInSpaceServlet;
import org.loklak.api.iot.ValidateServlet;
import org.loklak.api.iot.YahiHazeServlet;
import org.loklak.api.iot.EarthquakeServlet;
import org.loklak.api.p2p.HelloService;
import org.loklak.api.p2p.PeersServlet;
import org.loklak.api.p2p.PushServlet;
import org.loklak.api.search.SearchServlet;
import org.loklak.api.search.ShortlinkFromTweetServlet;
import org.loklak.api.search.SuggestServlet;
import org.loklak.api.search.SusiService;
import org.loklak.api.search.TimeAndDateService;
import org.loklak.api.search.ConsoleService;
import org.loklak.api.search.EventBriteCrawlerService;
import org.loklak.api.search.UserServlet;
import org.loklak.api.search.WordpressCrawlerService;
import org.loklak.api.search.GenericScraper;
import org.loklak.api.search.GithubProfileScraper;
import org.loklak.api.search.InstagramProfileScraper;
import org.loklak.api.search.LocationWiseTimeService;
import org.loklak.api.search.WeiboUserInfo;
import org.loklak.api.search.WikiGeoData;
import org.loklak.api.search.MeetupsCrawlerService;
import org.loklak.api.search.QuoraProfileScraper;
import org.loklak.api.search.RSSReaderService;
import org.loklak.api.tools.CSVServlet;
import org.loklak.api.tools.XMLServlet;
import org.loklak.api.vis.MapServlet;
import org.loklak.api.vis.MarkdownServlet;
import org.loklak.api.vis.PieChartServlet;
import org.loklak.data.DAO;
import org.loklak.harvester.TwitterScraper;
import org.loklak.http.RemoteAccess;
import org.loklak.server.APIHandler;
import org.loklak.server.FileHandler;
import org.loklak.server.HttpsMode;
import org.loklak.tools.Browser;
import org.loklak.tools.OS;


public class LoklakServer {
	
    public final static Set<String> blacklistedHosts = new ConcurrentHashSet<>();

    
    private static Server server = null;
    private static Caretaker caretaker = null;
    public  static QueuedIndexing queuedIndexing = null;
    private static DumpImporter dumpImporter = null;
    private static HttpsMode httpsMode = HttpsMode.OFF;
    public static Class<? extends Servlet>[] services;

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
        
        Log.getLog().info("Starting loklak initialization");

        // prepare shutdown signal
        File pid = new File(dataFile, "loklak.pid");
        if (pid.exists()) pid.deleteOnExit(); // thats a signal for the stop.sh script that loklak has terminated
        
        // prepare signal for startup script
        File startup = new File(dataFile, "startup.tmp");
        if (startup.exists()){
	        startup.deleteOnExit();
	        FileWriter writer = new FileWriter(startup);
			writer.write("startup");
			writer.close();
        }
        
		
        // load the config file(s);
        Map<String, String> config = readConfig(data);
        
        // set localhost pattern
        String server_localhost = config.get("server.localhost");
        if (server_localhost != null && server_localhost.length() > 0) {
            for (String h: server_localhost.split(",")) RemoteAccess.addLocalhost(h);
        }
        
        // check for https modus
        switch(config.get("https.mode")){
        	case "on": httpsMode = HttpsMode.ON; break;
        	case "redirect": httpsMode = HttpsMode.REDIRECT; break;
        	case "only": httpsMode = HttpsMode.ONLY; break;
        	default: httpsMode = HttpsMode.OFF;
        }
        
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
        
        // initialize all data        
        try{
        	DAO.init(config, data);
        } catch(Exception e){
        	Log.getLog().warn(e.getMessage());
        	Log.getLog().warn("Could not initialize DAO. Exiting.");
        	System.exit(-1);
        }
        
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
        Browser.openBrowser("http://127.0.0.1:" + httpPort + "/");
        
        Log.getLog().info("finished startup!");
        
        // signal to startup script
        if (startup.exists()){
        	FileWriter writer = new FileWriter(startup);
			writer.write("done");
			writer.close();
        }
        
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

                    Log.getLog().info("Shutting down log4j2");
                    LogManager.shutdown();

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

            Log.getLog().info("HTTPS activated");
        	
        	String keySource = DAO.getConfig("https.keysource", "keystore");
            KeyStore keyStore;
        	String keystoreManagerPass;
        	
        	//check for key source. Can be a java keystore or in pem format (gets converted automatically)
        	if("keystore".equals(keySource)){
                Log.getLog().info("Loading keystore from disk");

        		//use native keystore format
        		
        		File keystoreFile = new File(DAO.conf_dir, DAO.getConfig("keystore.name", "keystore.jks"));
        		if(!keystoreFile.exists() || !keystoreFile.isFile() || !keystoreFile.canRead()){
        			throw new Exception("Could not find keystore");
        		}
        		keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(keystoreFile.getAbsolutePath()), DAO.getConfig("keystore.password", "").toCharArray());

        		keystoreManagerPass = DAO.getConfig("keystore.password", "");
        	}
        	else if ("key-cert".equals(keySource)){
                Log.getLog().info("Importing keystore from key/cert files");
        		//use more common pem format as used by openssl

                //generate random password
                char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
                StringBuilder sb = new StringBuilder();
                Random random = new Random();
                for (int i = 0; i < 20; i++) {
                    char c = chars[random.nextInt(chars.length)];
                    sb.append(c);
                }
                String password = keystoreManagerPass = sb.toString();

        		//get key and cert
        		File keyFile = new File(DAO.getConfig("https.key", ""));
        		if(!keyFile.exists() || !keyFile.isFile() || !keyFile.canRead()){
        			throw new Exception("Could not find key file");
        		}
        		File certFile = new File(DAO.getConfig("https.cert", ""));
        		if(!certFile.exists() || !certFile.isFile() || !certFile.canRead()){
        			throw new Exception("Could not find cert file");
        		}

                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

                byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
                byte[] certBytes = Files.readAllBytes(certFile.toPath());

                PEMParser parser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(certBytes)));
                X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate((X509CertificateHolder) parser.readObject());

                parser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(keyBytes)));
                PrivateKey key = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey((PrivateKeyInfo) parser.readObject());

                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);

                keyStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
                keyStore.setKeyEntry("defaultKey",key, password.toCharArray(), new Certificate[] {cert});

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

            sslContextFactory.setKeyStore(keyStore);
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

    @SuppressWarnings("unchecked")
    private static void setServerHandler(File dataFile){
    	
    	
    	// create security handler for http auth and http-to-https redirects
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        
        boolean redirect = httpsMode.equals(HttpsMode.REDIRECT);
        boolean auth = "true".equals(DAO.getConfig("http.auth", "false"));
        
        if(redirect || auth){
        	
            org.eclipse.jetty.security.LoginService loginService = new org.eclipse.jetty.security.HashLoginService("LoklakRealm", DAO.conf_dir.getAbsolutePath() + "/http_auth");
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
        IPAccessHandler ipaccess = new IPAccessHandler();
        String blacklist = DAO.getConfig("server.blacklist", "");
        if (blacklist != null && blacklist.length() > 0) try {
            ipaccess = new IPAccessHandler();
            String[] bx = blacklist.split(",");
            ipaccess.setBlack(bx);
            for (String b: bx) {
                int p = b.indexOf('|');
                blacklistedHosts.add(p < 0 ? b : b.substring(0, p));
            }
        } catch (IllegalArgumentException e) {
            Log.getLog().warn("bad blacklist:" + blacklist, e);
        }
        
        WebAppContext htrootContext = new WebAppContext();
        htrootContext.setContextPath("/");

        File tmp = new File(dataFile, "tmp");
        MultipartConfigElement multipartConfigDefault = new MultipartConfigElement(tmp.getAbsolutePath());
        MultipartConfigElement multipartConfig = new MultipartConfigElement(tmp.getAbsolutePath(), multipartConfigDefault.getMaxFileSize(), multipartConfigDefault.getMaxRequestSize(), 1024 * 1024); // reduce IO using a non-zero fileSizeThreshold
        ServletContextHandler servletHandler = new ServletContextHandler();

        // add services
        services = new Class[]{
                // admin
                StatusService.class,
                
                // cms
                AppsService.class,
                AuthorizationDemoService.class,
                ChangeUserRoleService.class,
                LoginService.class,
                PasswordRecoveryService.class,
                PasswordResetService.class,
                PublicKeyRegistrationService.class,
                SignUpService.class,
                TopMenuService.class,
                UserManagementService.class,
                TwitterAnalysisService.class,
                AmazonProductService.class,
                UserAccountPermissions.class,

                // geo
                
                // iot
                
                // p2p
                HelloService.class,
                
                // search
                ConsoleService.class,
                EventBriteCrawlerService.class,
                MeetupsCrawlerService.class,
                RSSReaderService.class,
                SusiService.class,
                WordpressCrawlerService.class,
                GithubProfileScraper.class,
                InstagramProfileScraper.class,
                LocationWiseTimeService.class,
                TimeAndDateService.class,
                WikiGeoData.class,
                QuoraProfileScraper.class
                
                // tools
                
                // vis
                
        };
        for (Class<? extends Servlet> service: services)
            try {
                servletHandler.addServlet(service, ((APIHandler) (service.newInstance())).getAPIPath());
            } catch (InstantiationException | IllegalAccessException e) {
                Log.getLog().warn(service.getName() + " instantiation error", e);
                e.printStackTrace();
            }
        
        // add servlets        
        servletHandler.addServlet(DumpDownloadServlet.class, "/dump/*");
        servletHandler.addServlet(ShortlinkFromTweetServlet.class, "/x");
        servletHandler.addServlet(AccessServlet.class, "/api/access.json");
        servletHandler.addServlet(AccessServlet.class, "/api/access.html");
        servletHandler.addServlet(AccessServlet.class, "/api/access.txt");
        servletHandler.addServlet(PeersServlet.class, "/api/peers.json");
        servletHandler.addServlet(PeersServlet.class, "/api/peers.csv");
        servletHandler.addServlet(CrawlerServlet.class, "/api/crawler.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.rss");
        servletHandler.addServlet(SearchServlet.class, "/api/search.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.txt");
        servletHandler.addServlet(SuggestServlet.class, "/api/suggest.json");
        servletHandler.addServlet(XMLServlet.class, "/api/xml2json.json");
        servletHandler.addServlet(CSVServlet.class, "/api/csv2json.json");
        ServletHolder accountServletHolder = new ServletHolder(AccountService.class);
        accountServletHolder.getRegistration().setMultipartConfig(multipartConfig);
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
        servletHandler.addServlet(GenericScraper.class, "/api/genericscraper.json");
        servletHandler.addServlet(WeiboUserInfo.class, "/api/weibo.json");
        ServletHolder pushServletHolder = new ServletHolder(PushServlet.class);
        pushServletHolder.getRegistration().setMultipartConfig(multipartConfig);
        servletHandler.addServlet(pushServletHolder, "/api/push.json");
        ServletHolder geojsonPushServletHolder = new ServletHolder(GeoJsonPushServlet.class);
        geojsonPushServletHolder.getRegistration().setMultipartConfig(multipartConfig);
        servletHandler.addServlet(geojsonPushServletHolder, "/api/push/geojson.json");
        servletHandler.addServlet(FossasiaPushServlet.class, "/api/push/fossasia.json");
        servletHandler.addServlet(OpenWifiMapPushServlet.class, "/api/push/openwifimap.json");
        servletHandler.addServlet(NodelistPushServlet.class, "/api/push/nodelist.json");
        servletHandler.addServlet(FreifunkNodePushServlet.class, "/api/push/freifunknode.json");
        servletHandler.addServlet(FreifunkNodeFetchServlet.class, "/api/freifunkfetch.json");
        servletHandler.addServlet(NetmonPushServlet.class, "/api/push/netmon.xml");
        servletHandler.addServlet(NMEAServlet.class, "/api/nmea.txt");
        servletHandler.addServlet(NOAAAlertServlet.class, "/api/noaa.json");
        servletHandler.addServlet(StuffInSpaceServlet.class, "/api/stuffinspace.json");
        servletHandler.addServlet(YahiHazeServlet.class, "/api/yahi.json");
        servletHandler.addServlet(EarthquakeServlet.class, "/api/earthquake.json");
        ServletHolder assetServletHolder = new ServletHolder(AssetServlet.class);
        assetServletHolder.getRegistration().setMultipartConfig(multipartConfig);
        servletHandler.addServlet(assetServletHolder, "/api/asset");
        servletHandler.addServlet(Sitemap.class, "/api/sitemap.xml");
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
        servletHandler.addServlet(PieChartServlet.class, "/vis/piechart.png");
        servletHandler.setMaxFormContentSize(10 * 1024 * 1024); // 10 MB

        ErrorHandler errorHandler = new LoklakErrorHandler();
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
        gzipHandler.setIncludedMimeTypes("text/html,text/plain,text/xml,text/css,application/javascript,text/javascript,application/json");
        gzipHandler.setHandler(handlerlist2);
        
        HashSessionIdManager idmanager = new HashSessionIdManager();
        LoklakServer.server.setSessionIdManager(idmanager);
        SessionHandler sessions = new SessionHandler(new HashSessionManager());
        sessions.setHandler(gzipHandler);
        securityHandler.setHandler(sessions);
        ipaccess.setHandler(securityHandler);
        
        LoklakServer.server.setHandler(ipaccess);
        
        
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
