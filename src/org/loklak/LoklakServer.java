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
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.loklak.api.server.AccessServlet;
import org.loklak.api.server.AssetServlet;
import org.loklak.api.server.CampaignServlet;
import org.loklak.api.server.CrawlerServlet;
import org.loklak.api.server.DumpDownloadServlet;
import org.loklak.api.server.GeocodeServlet;
import org.loklak.api.server.HelloServlet;
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
import org.loklak.tools.Browser;
import org.loklak.tools.OS;
import org.loklak.vis.server.MapServlet;
import org.loklak.vis.server.MarkdownServlet;

public class LoklakServer {

    public final static Set<String> blacklistedHosts = new ConcurrentHashSet<>();

    
    private static Server server = null;
    private static Caretaker caretaker = null;
    
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
    
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
        
        // init config, log and elasticsearch
        Path data = FileSystems.getDefault().getPath("data");
        File dataFile = data.toFile();
        if (!dataFile.exists()) dataFile.mkdirs(); // should already be there since the start.sh script creates it

        // load the config file(s);
        Map<String, String> config = readConfig(data);
        
        // check if a loklak service is already running on configured port
        String httpPortS = config.get("port.http");
        int httpPort = httpPortS == null ? 9000 : Integer.parseInt(httpPortS);
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(httpPort);
            ss.setReuseAddress(true);
        } catch (IOException e) {
            // the socket is already occupied by another service
            Log.getLog().info("port " + httpPort + " is already occupied by another service, maybe another loklak is running on this port already. exit.");
            Browser.openBrowser("http://localhost:" + httpPort + "/");
            System.exit(-1);
        } finally {
            // close the socket again
            if (ss != null) {try {ss.close();} catch (IOException e) {}}
        }
        
        // prepare shutdown signal
        File pid = new File(dataFile, "loklak.pid");
        if (pid.exists()) pid.deleteOnExit(); // thats a signal for the stop.sh script that loklak has terminated
        
        // initialize all data        
        DAO.init(config, data);
        
        /// https
        // keytool -genkey -alias sitename -keyalg RSA -keystore keystore.jks -keysize 2048
/*
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(9999);
         
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
         
        SslContextFactory sslContextFactory = new SslContextFactory();
        File keystore = new File(DAO.conf_dir, DAO.getConfig("keystore.name", "keystore.jks"));
        sslContextFactory.setKeyStorePath(keystore.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(DAO.getConfig("keystore.password", ""));
        sslContextFactory.setKeyManagerPassword(DAO.getConfig("keystore.password", ""));
        
        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(9998);
        
        server.setConnectors(new Connector[] { connector, sslConnector });
*/
        /// https
        
        // init the http server
        
        
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(300);
        ExecutorThreadPool pool = new ExecutorThreadPool(10, 100, 10000, TimeUnit.MILLISECONDS, queue);;
        LoklakServer.server = new Server(pool);
        LoklakServer.server.setStopAtShutdown(true);
        ServerConnector connector = new ServerConnector(LoklakServer.server);
        connector.setPort(httpPort);
        connector.setName("httpd:" + httpPort);
        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
        LoklakServer.server.addConnector(connector);

        // Setup IPAccessHandler for blacklists
        String blacklist = config.get("server.blacklist");
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
        FilterHolder filter = servletHandler.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        filter.setInitParameter("mimeTypes", "text/plain");
        servletHandler.addServlet(DumpDownloadServlet.class, "/dump/*");
        servletHandler.addServlet(ShortlinkFromTweetServlet.class, "/x");
        servletHandler.addServlet(AccessServlet.class, "/api/access.json");
        servletHandler.addServlet(AccessServlet.class, "/api/access.html");
        servletHandler.addServlet(AccessServlet.class, "/api/access.txt");
        servletHandler.addServlet(HelloServlet.class, "/api/hello.json");
        servletHandler.addServlet(PeersServlet.class, "/api/peers.json");
        servletHandler.addServlet(CrawlerServlet.class, "/api/crawler.json");
        servletHandler.addServlet(StatusServlet.class, "/api/status.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.rss");
        servletHandler.addServlet(SearchServlet.class, "/api/search.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.txt");
        servletHandler.addServlet(SuggestServlet.class, "/api/suggest.json");
        servletHandler.addServlet(AccountServlet.class, "/api/account.json");
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
        
        ResourceHandler fileHandler = new ResourceHandler();
        fileHandler.setDirectoriesListed(true);
        fileHandler.setWelcomeFiles(new String[]{ "index.html" });
        fileHandler.setResourceBase("html");
        
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
        LoklakServer.server.setHandler(handlerlist2);

        LoklakServer.server.start();
        LoklakServer.caretaker = new Caretaker();
        LoklakServer.caretaker.start();
        
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
                    LoklakServer.server.stop();
                    LoklakServer.caretaker.shutdown();
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
    
    
}
