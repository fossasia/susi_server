/**
 *  Messages
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
import java.util.EnumSet;

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
import org.loklak.api.server.CampaignServlet;
import org.loklak.api.server.CrawlerServlet;
import org.loklak.api.server.DumpDownloadServlet;
import org.loklak.api.server.HelloServlet;
import org.loklak.api.server.PeersServlet;
import org.loklak.api.server.PushServlet;
import org.loklak.api.server.SearchServlet;
import org.loklak.api.server.StatusServlet;
import org.loklak.api.server.SuggestServlet;
import org.loklak.data.DAO;
import org.loklak.tools.Browser;

public class Main {

    public static String USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    
    private static Server server = null;
    private static Caretaker caretaker = null;
    
    public static void main(String[] args) throws Exception {

        // init config, log and elasticsearch
        File data = new File(new File("."), "data");
        if (!data.exists()) data.mkdirs();
        File tmp = new File(data, "tmp");
        if (!tmp.exists()) data.mkdirs();
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
        Main.server = new Server();
        Main.server.setStopAtShutdown(true);
        ServerConnector connector = new ServerConnector(Main.server);
        int httpPort = (int) DAO.getConfig("port.http", 9100);
        connector.setPort(httpPort);
        connector.setName("httpd:" + httpPort);
        connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
        Main.server.addConnector(connector);

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
        servletHandler.addServlet(CampaignServlet.class, "/api/campaign.json"); 
        ServletHolder pushServletHolder = new ServletHolder(PushServlet.class);
        pushServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(tmp.getAbsolutePath()));
        servletHandler.addServlet(pushServletHolder, "/api/push.json");
        
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
        Main.server.setHandler(handlerlist2);
 
        Main.server.start();
        Main.caretaker = new Caretaker();
        Main.caretaker.start();
        Browser.openBrowser("http://localhost:" + httpPort + "/");
        
        // ** services are now running **
        
        // start a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Log.getLog().info("catched main termination signal");
                    Main.server.stop();
                    Main.caretaker.shutdown();
                    DAO.close();
                    Log.getLog().info("main terminated, goodby.");
                } catch (Exception e) {
                }
            }
        });

        // ** wait for shutdown signal, do this with a kill HUP (default level 1, 'kill -1') signal **
        
        Main.server.join();
        Log.getLog().info("server terminated");
        
        // After this, the jvm processes all shutdown hooks and terminates then.
        // The main termination line is therefore inside the shutdown hook.
    }
    
    
}