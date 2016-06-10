/**
 *  FileHandler
 *  Copyright 10.02.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

public class FileHandler extends ResourceHandler implements Handler {
    
    private final long CACHE_LIMIT = 1024L * 1024L;
    private int expiresSeconds = 0;
    
    /**
     * cerate a custom ResourceHandler with more caching
     * @param expiresSeconds the time each file shall stay in the cache
     */
    public FileHandler(int expiresSeconds) {
        this.expiresSeconds = expiresSeconds;
        //this.setMinMemoryMappedContentLength((int) CACHE_LIMIT);
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // use the ResourceHandler to handle the request. This method calls doResponseHeaders internally
        super.handle(target, baseRequest, request, response);
    }

    @Override
    protected void doResponseHeaders(HttpServletResponse response, Resource resource, String mimeType) {
        super.doResponseHeaders(response, resource, mimeType);
        // modify the caching strategy of ResourceHandler
        if (this.expiresSeconds > 0) setCaching(response, this.expiresSeconds);
    }
    
    public static void setCaching(final HttpServletResponse response, final int expiresSeconds) {
        if (response instanceof org.eclipse.jetty.server.Response) {
            org.eclipse.jetty.server.Response r = (org.eclipse.jetty.server.Response) response;
            HttpFields fields = r.getHttpFields();
            
            // remove the last-modified field since caching otherwise does not work
            /*
               https://www.ietf.org/rfc/rfc2616.txt
               "if the response does have a Last-Modified time, the heuristic
               expiration value SHOULD be no more than some fraction of the interval
               since that time. A typical setting of this fraction might be 10%."
            */
            fields.remove(HttpHeader.LAST_MODIFIED); // if this field is present, the reload-time is a 10% fraction of ttl and other caching headers do not work

            // cache-control: allow shared caching (i.e. proxies) and set expires age for cache
            fields.put(HttpHeader.CACHE_CONTROL, "public, max-age=" + Integer.toString(expiresSeconds)); // seconds
        } else {
            response.setHeader(HttpHeader.LAST_MODIFIED.asString(), ""); // not really the best wqy to remove this header but there is no other option
            response.setHeader(HttpHeader.CACHE_CONTROL.asString(), "public, max-age=" + Integer.toString(expiresSeconds));
        }

        // expires: define how long the file shall stay in a cache if cache-control is not used for this information
        response.setDateHeader(HttpHeader.EXPIRES.asString(), System.currentTimeMillis() + expiresSeconds * 1000);
    }

    @Override
    public Resource getResource(String path) {
        Resource resource = super.getResource(path);
        if (resource == null) return null;
        if (!(resource instanceof PathResource) || !resource.exists()) return resource;
        try {
            File f = resource.getFile();
            if (f.isDirectory()) return resource;
            CacheResource cache = resourceCache.get(f);
            if (cache != null) return cache;
            if (f.length() < CACHE_LIMIT || f.getName().endsWith(".html")) {
                cache = new CacheResource((PathResource) resource);
                resourceCache.put(f, cache);
                return cache;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resource;
    }

    private final Map<File, CacheResource> resourceCache = new ConcurrentHashMap<>();

    private static class CacheResource extends Resource {

        private byte[] buffer;
        private long lastModified;
        private File file;
        
        public CacheResource(PathResource pathResource) throws IOException {
            this.file = pathResource.getFile();
            initCache(false);
            pathResource.close();
        }
        
        private void initCache(boolean trueLastModified) throws IOException {
            this.buffer = Files.readAllBytes(this.file.toPath());
            this.lastModified = trueLastModified ? this.file.lastModified() : System.currentTimeMillis();
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public long lastModified() {
            if (this.file.lastModified() > this.lastModified) try {initCache(true);} catch (IOException e) {}
            return this.lastModified;
        }

        @Override
        public long length() {
            return this.buffer.length;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (this.file.lastModified() > this.lastModified) initCache(true);
            return new ByteArrayInputStream(this.buffer);
        }

        @Override
        public ReadableByteChannel getReadableByteChannel() throws IOException {
            return Channels.newChannel(this.getInputStream());
        }

        @Override
        public String getName() {
            return this.file.getName();
        }

        @Override public void close() {}
        @Override public String[] list() {throw new UnsupportedOperationException();}
        @Override public boolean delete() throws SecurityException {throw new UnsupportedOperationException();}
        @Override public Resource addPath(String arg0) throws IOException, MalformedURLException {throw new UnsupportedOperationException();}
        @Override public File getFile() throws IOException {throw new UnsupportedOperationException();}
        @Override public URL getURL() {throw new UnsupportedOperationException();}
        @Override public boolean isContainedIn(Resource arg0) throws MalformedURLException {throw new UnsupportedOperationException();}
        @Override public boolean renameTo(Resource arg0) throws SecurityException {throw new UnsupportedOperationException();}
    }
    
    
}
