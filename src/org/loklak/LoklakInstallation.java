/**
 *  LoklakInstallation
 *  Copyright 04.08.2016 by Robert Mader, @treba123
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

import org.apache.logging.log4j.LogManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.loklak.api.cms.InstallationPageService;
import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.server.FileHandler;
import org.loklak.server.HttpsMode;
import org.loklak.tools.Browser;

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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.loklak.LoklakServer.readConfig;


public class LoklakInstallation {
	
    public final static Set<String> blacklistedHosts = new ConcurrentHashSet<>();
    
    public static Server server = null;
    private static HttpsMode httpsMode = HttpsMode.OFF;
    
    public static void main(String[] args) throws Exception {
    	System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
        
        // init config, log and elasticsearch
        Path data = FileSystems.getDefault().getPath("data");
        File dataFile = data.toFile();
        if (!dataFile.exists()) dataFile.mkdirs(); // should already be there since the start.sh script creates it
        
        Log.getLog().info("Starting loklak-installation initialization");

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
        	default: httpsMode = HttpsMode.OFF; break;
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
        
        LoklakInstallation.server.start();

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
                    LoklakInstallation.server.stop();
                    DAO.close();
                    Log.getLog().info("main terminated, goodby.");

                    Log.getLog().info("Shutting down log4j2");
                    LogManager.shutdown();

                } catch (Exception e) {
                }
            }
        });

        // ** wait for shutdown signal, do this with a kill HUP (default level 1, 'kill -1') signal **
        
        LoklakInstallation.server.join();
        Log.getLog().info("server terminated");
        
        // After this, the jvm processes all shutdown hooks and terminates then.
        // The main termination line is therefore inside the shutdown hook.
    }
    
    //initiate http server
    private static void setupHttpServer(int httpPort, int httpsPort) throws Exception{
    	QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMaxThreads(500);
        LoklakInstallation.server = new Server(pool);
        LoklakInstallation.server.setStopAtShutdown(true);
        
        //http
        if(!httpsMode.equals(HttpsMode.ONLY)){
	        HttpConfiguration http_config = new HttpConfiguration();
	        if(httpsMode.equals(HttpsMode.REDIRECT)) { //redirect
	        	http_config.addCustomizer(new SecureRequestCustomizer());
	        	http_config.setSecureScheme("https");
	        	http_config.setSecurePort(httpsPort);
	        }
	        
	        ServerConnector connector = new ServerConnector(LoklakInstallation.server);
	        connector.addConnectionFactory(new HttpConnectionFactory(http_config));
	        connector.setPort(httpPort);
	        connector.setName("httpd:" + httpPort);
	        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        LoklakInstallation.server.addConnector(connector);
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
	        ServerConnector sslConnector = new ServerConnector(LoklakInstallation.server, ssl, http1);
	        sslConnector.setPort(httpsPort);
	        sslConnector.setName("httpd:" + httpsPort);
	        sslConnector.setIdleTimeout(20000); // timout in ms when no bytes send / received
	        LoklakInstallation.server.addConnector(sslConnector);
        }
    }
    
    private static void setServerHandler(File dataFile){
    	
    	
    	// create security handler for http auth and http-to-https redirects
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        
        boolean redirect = httpsMode.equals(HttpsMode.REDIRECT);
        boolean auth = "true".equals(DAO.getConfig("http.auth", "false"));
        
        if(redirect || auth){
        	
            org.eclipse.jetty.security.LoginService loginService = new org.eclipse.jetty.security.HashLoginService("LoklakRealm", DAO.conf_dir.getAbsolutePath() + "/http_auth");
        	if(auth) LoklakInstallation.server.addBean(loginService);
        	
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
        try {
            servletHandler.addServlet(InstallationPageService.class, (InstallationPageService.class.newInstance()).getAPIPath());
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        servletHandler.setMaxFormContentSize(10 * 1024 * 1024); // 10 MB

        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setShowStacks(true);
        servletHandler.setErrorHandler(errorHandler);
        
        FileHandler fileHandler = new FileHandler(0);
        fileHandler.setDirectoriesListed(true);
        fileHandler.setWelcomeFiles(new String[]{ "index.html" });
        fileHandler.setResourceBase("installation");
        
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
        LoklakInstallation.server.setSessionIdManager(idmanager);
        SessionHandler sessions = new SessionHandler(new HashSessionManager());
        sessions.setHandler(gzipHandler);
        securityHandler.setHandler(sessions);
        ipaccess.setHandler(securityHandler);
        
        LoklakInstallation.server.setHandler(ipaccess);
        
        
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
	        	throw new IOException("port " + httpsPort + " is already occupied by another service, maybe another loklak is running on this port already. exit.");
	        } finally {
	            // close the socket again
	            if (sss != null) sss.close();
	        }
        }
    }

    public static void shutdown(int exitcode){
        Log.getLog().info("Shutting down installation now");
        server.setStopTimeout(0);
        System.exit(exitcode);
    }
}
