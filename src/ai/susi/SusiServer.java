/**
 *  SUSIServer
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

package ai.susi;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import javax.servlet.Servlet;

import ai.susi.server.api.aaa.*;
import ai.susi.server.api.cms.*;
import io.swagger.jaxrs.listing.ApiListingResource;
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
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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

import ai.susi.server.APIHandler;
import ai.susi.server.FileHandler;
import ai.susi.server.HttpsMode;
import ai.susi.server.RemoteAccess;
import ai.susi.server.api.learning.ConsoleLearning;
import ai.susi.server.api.service.EmailSenderService;
import ai.susi.server.api.susi.ConsoleService;
import ai.susi.server.api.susi.GenericScraper;
import ai.susi.server.api.susi.JsonPathTestService;
import ai.susi.server.api.susi.MindService;
import ai.susi.server.api.susi.RSSReaderService;
import ai.susi.server.api.susi.StatusService;
import ai.susi.server.api.susi.SusiService;
import ai.susi.server.api.susi.UnansweredServlet;
import ai.susi.server.api.susi.UserService;
import ai.susi.server.api.vis.MapServlet;
import ai.susi.server.api.vis.MarkdownServlet;
import ai.susi.server.api.vis.PieChartServlet;
import ai.susi.tools.OS;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;


public class SusiServer {
	
    public final static Set<String> blacklistedHosts = new ConcurrentHashSet<>();

    private static Server server = null;
    private static Caretaker caretaker = null;
    private static HttpsMode httpsMode = HttpsMode.OFF;
    public static Class<? extends Servlet>[] services;
    public static HandlerCollection handlerCollection = new HandlerCollection();
    public static ContextHandler buildSwaggerUI() throws Exception {
        ResourceHandler rh = new ResourceHandler();
        rh.setResourceBase(SusiServer.class.getClassLoader()
                .getResource("META-INF/resources/webjars/swagger-ui/2.1.4")
                .toURI().toString());
        ContextHandler context = new ContextHandler();
        context.setContextPath("/docs/");
        context.setHandler(rh);
        return context;
    }

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
        
        Log.getLog().info("Starting SUSI initialization");

        // prepare shutdown signal
        File pid = new File(dataFile, "susi.pid");
        if (pid.exists()) pid.deleteOnExit(); // thats a signal for the stop.sh script that SUSI has terminated
        
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
        int httpPort = httpPortS == null ? 4000 : Integer.parseInt(httpPortS);
        if(env.containsKey("PORT")) {
            httpPort = Integer.parseInt(env.get("PORT"));
        }
        String httpsPortS = config.get("port.https");
        int httpsPort = httpsPortS == null ? 9443 : Integer.parseInt(httpsPortS);
        if(env.containsKey("PORTSSL")) {
            httpsPort = Integer.parseInt(env.get("PORTSSL"));
        }
        
        // check if a SUSI service is already running on configured port
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
            e.printStackTrace();
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
        
        SusiServer.server.start();
        SusiServer.caretaker = new Caretaker();
        SusiServer.caretaker.start();
        
        // if this is not headless, we can open a browser automatically
        OS.openBrowser("http://127.0.0.1:" + httpPort + "/");
        
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
                    SusiServer.caretaker.shutdown();
                    SusiServer.server.stop();
                    DAO.close();
                    Log.getLog().info("main terminated, goodby.");

                    Log.getLog().info("Shutting down log4j2");
                    LogManager.shutdown();

                } catch (Exception e) {
                }
            }
        });

        // ** wait for shutdown signal, do this with a kill HUP (default level 1, 'kill -1') signal **
        
        SusiServer.server.join();
        Log.getLog().info("server terminated");
        
        // After this, the jvm processes all shutdown hooks and terminates then.
        // The main termination line is therefore inside the shutdown hook.
    }
    
    //initiate http server
    private static void setupHttpServer(int httpPort, int httpsPort) throws Exception{
    	QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMaxThreads(500);
        SusiServer.server = new Server(pool);
        SusiServer.server.setStopAtShutdown(true);
        
        //http
        if(!httpsMode.equals(HttpsMode.ONLY)){
	        HttpConfiguration http_config = new HttpConfiguration();
	        if(httpsMode.equals(HttpsMode.REDIRECT)) { //redirect
	        	http_config.addCustomizer(new SecureRequestCustomizer());
	        	http_config.setSecureScheme("https");
	        	http_config.setSecurePort(httpsPort);
	        }
	        
	        ServerConnector connector = new ServerConnector(SusiServer.server);
	        connector.addConnectionFactory(new HttpConnectionFactory(http_config));
	        connector.setPort(httpPort);
	        connector.setName("httpd:" + httpPort);
	        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        SusiServer.server.addConnector(connector);
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
	        
	        //ServerConnector sslConnector = new ServerConnector(SUSIServer.server, ssl, alpn, http2, http1);
	        ServerConnector sslConnector = new ServerConnector(SusiServer.server, ssl, http1);
	        sslConnector.setPort(httpsPort);
	        sslConnector.setName("httpd:" + httpsPort);
	        sslConnector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        SusiServer.server.addConnector(sslConnector);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setServerHandler(File dataFile){
    	
    	
    	// create security handler for http auth and http-to-https redirects
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        
        boolean redirect = httpsMode.equals(HttpsMode.REDIRECT);
        boolean auth = "true".equals(DAO.getConfig("http.auth", "false"));
        
        if(redirect || auth){
        	
            org.eclipse.jetty.security.LoginService loginService = new org.eclipse.jetty.security.HashLoginService("SUSIRealm", DAO.conf_dir.getAbsolutePath() + "/http_auth");
        	if(auth) SusiServer.server.addBean(loginService);
        	
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

        ServletContextHandler servletHandler = new ServletContextHandler();

        // add services
        services = new Class[]{
                // aaa services
                StatusService.class,
                AppsService.class,
                AuthorizationDemoService.class,
                LoginService.class,
                PasswordRecoveryService.class,
                PasswordResetService.class,
                PublicKeyRegistrationService.class,
                DownloadDataSettings.class,
                SignUpService.class,
                TopMenuService.class,
                PasswordChangeService.class,
                ListSettingsService.class,
                ConvertSkillJsonToTxtService.class,
                GroupListService.class,
                ConvertSkillTxtToJsonService.class,
                GetSkillJsonService.class,
                GetSkillTxtService.class,
                CreateSkillService.class,
                PostSkillJsonService.class,
                PostSkillTxtService.class,
                ModelListService.class,
                LanguageListService.class,
                ListUserSettings.class,
                ListSkillService.class,
                GetAllLanguages.class,
                DeleteGroupService.class,
                ExampleSkillService.class,
                UserManagementService.class,
                ChangeUserSettings.class,
                UserAccountPermissions.class,
                JsonPathTestService.class,
                RateSkillService.class,
                DeleteSkillService.class,
                ModifySkillService.class,
                HistorySkillService.class,
                GetCommitHistory.class,
                // susi search aggregation services
                ConsoleService.class,
                RSSReaderService.class,
                SusiService.class,
                MindService.class,
                UserService.class,
                
                // learning services
                ConsoleLearning.class,
                
                // services
                EmailSenderService.class,

                //User Roles
                ChangeUserRoles.class,

                //Get all Users
                GetAllUsers.class,

                //Groups
                GetGroupDetails.class,
                CreateGroupService.class,
                GetAllGroups.class,
                GetSkillRatingService.class
        };
        for (Class<? extends Servlet> service: services)
            try {
                servletHandler.addServlet(service, ((APIHandler) (service.newInstance())).getAPIPath());
            } catch (InstantiationException | IllegalAccessException e) {
                Log.getLog().warn(service.getName() + " instantiation error", e);
                e.printStackTrace();
            }
        
        // susi api
        servletHandler.addServlet(UnansweredServlet.class, "/susi/unanswered.txt");
        
        // aaa api
        servletHandler.addServlet(AccessServlet.class, "/aaa/access.json");
        servletHandler.addServlet(AccessServlet.class, "/aaa/access.html");
        servletHandler.addServlet(AccessServlet.class, "/aaa/access.txt");
        servletHandler.addServlet(Sitemap.class, "/sitemap.xml");
        servletHandler.addServlet(ThreaddumpServlet.class, "/threaddump.txt");
        
        // aggregation api
        servletHandler.addServlet(GenericScraper.class, "/susi/genericscraper.json");

        // vis api
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
        gzipHandler.setIncludedMimeTypes("text/html,text/plain,text/xml,text/css,application/javascript,text/javascript,application/json");
        gzipHandler.setHandler(handlerlist2);
        
        HashSessionIdManager idmanager = new HashSessionIdManager();
        SusiServer.server.setSessionIdManager(idmanager);
        SessionHandler sessions = new SessionHandler(new HashSessionManager());
        sessions.setHandler(gzipHandler);
        securityHandler.setHandler(sessions);
        ipaccess.setHandler(securityHandler);
        handlerCollection.addHandler(ipaccess);


        ContextHandlerCollection contexts = new ContextHandlerCollection();

        ResourceConfig resourceConfig = new ResourceConfig();

        resourceConfig.packages(SusiServer.class.getPackage().getName(),
                ApiListingResource.class.getPackage().getName());

        ServletContainer sc = new ServletContainer(resourceConfig);
        ServletHolder holder = new ServletHolder(sc);

        servletHandler.addServlet(holder, "/docs/*");
        handlerCollection.addHandler(contexts);

        SusiServer.server.setHandler(handlerCollection);

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
	            throw new IOException("port " + httpPort + " is already occupied by another service, maybe another SUSI is running on this port already. exit.");
	        } finally {
	            // close the socket again
	            if (ss != null) ss.close();
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
	        	throw new IOException("port " + httpsPort + " is already occupied by another service, maybe another SUSI is running on this port already. exit.");
	        } finally {
	            // close the socket again
	            if (sss != null) sss.close();
	        }
        }
    }
}
