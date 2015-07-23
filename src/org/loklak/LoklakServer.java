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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppContext;
import org.loklak.api.server.AssetServlet;
import org.loklak.api.server.CampaignServlet;
import org.loklak.api.server.CrawlerServlet;
import org.loklak.api.server.DumpDownloadServlet;
import org.loklak.api.server.GeocodeServlet;
import org.loklak.api.server.HelloServlet;
import org.loklak.api.server.PeersServlet;
import org.loklak.api.server.ProxyServlet;
import org.loklak.api.server.PushServlet;
import org.loklak.api.server.GeoJsonPushServlet;
import org.loklak.api.server.SearchServlet;
import org.loklak.api.server.SettingsServlet;
import org.loklak.api.server.StatusServlet;
import org.loklak.api.server.SuggestServlet;
import org.loklak.api.server.AccountServlet;
import org.loklak.api.server.ThreaddumpServlet;
import org.loklak.api.server.FossasiaPushServlet;
import org.loklak.data.DAO;
import org.loklak.harvester.TwitterScraper;
import org.loklak.tools.Browser;
import org.loklak.vis.server.MapServlet;
import org.loklak.vis.server.MarkdownServlet;

public class LoklakServer {

    private final static Set<PosixFilePermission> securePerm = new HashSet<PosixFilePermission>();
    
    static {
        securePerm.add(PosixFilePermission.OWNER_READ);
        securePerm.add(PosixFilePermission.OWNER_WRITE);
        securePerm.add(PosixFilePermission.OWNER_EXECUTE);
    }
    
    public final static void protectPath(Path path) {
        try {
            Files.setPosixFilePermissions(path, LoklakServer.securePerm);
        } catch (UnsupportedOperationException | IOException e) {}
    }
    
    private static Server server = null;
    private static Caretaker caretaker = null;
    
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
        
        // init config, log and elasticsearch
        Path data = FileSystems.getDefault().getPath("data");
        File dataFile = data.toFile();
        if (!dataFile.exists()) dataFile.mkdirs(); // should already be there since the start.sh script creates it

        File pid = new File(dataFile, "loklak.pid");
        if (pid.exists()) pid.deleteOnExit(); // thats a signal for the stop.sh script that loklak has terminated
        
        File tmp = new File(dataFile, "tmp");
        if (!tmp.exists()) dataFile.mkdirs();
        DAO.init(data);
        
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
        LoklakServer.server = new Server();
        LoklakServer.server.setStopAtShutdown(true);
        ServerConnector connector = new ServerConnector(LoklakServer.server);
        int httpPort = (int) DAO.getConfig("port.http", 9000);
        connector.setPort(httpPort);
        connector.setName("httpd:" + httpPort);
        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
        LoklakServer.server.addConnector(connector);

        WebAppContext htrootContext = new WebAppContext();
        htrootContext.setContextPath("/");
        
        ServletContextHandler servletHandler = new ServletContextHandler();
        FilterHolder filter = servletHandler.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        filter.setInitParameter("mimeTypes", "text/plain");
        servletHandler.addServlet(DumpDownloadServlet.class, "/dump/*");
        servletHandler.addServlet(HelloServlet.class, "/api/hello.json");
        servletHandler.addServlet(PeersServlet.class, "/api/peers.json");
        servletHandler.addServlet(CrawlerServlet.class, "/api/crawler.json");
        servletHandler.addServlet(StatusServlet.class, "/api/status.json");
        servletHandler.addServlet(SearchServlet.class, "/api/search.rss");  // both have same servlet class
        servletHandler.addServlet(SearchServlet.class, "/api/search.json"); // both have same servlet class
        servletHandler.addServlet(SuggestServlet.class, "/api/suggest.json");
        servletHandler.addServlet(AccountServlet.class, "/api/account.json");
        servletHandler.addServlet(CampaignServlet.class, "/api/campaign.json");
        servletHandler.addServlet(SettingsServlet.class, "/api/settings.json");
        servletHandler.addServlet(GeocodeServlet.class, "/api/geocode.json");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.gif");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.png");
        servletHandler.addServlet(ProxyServlet.class, "/api/proxy.jpg");
        ServletHolder pushServletHolder = new ServletHolder(PushServlet.class);
        pushServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(pushServletHolder, "/api/push.json");
        ServletHolder geojsonPushServletHolder = new ServletHolder(GeoJsonPushServlet.class);
        geojsonPushServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(geojsonPushServletHolder, "/api/push/geojson.json");
        servletHandler.addServlet(FossasiaPushServlet.class, "/api/push/fossasia.json");
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
