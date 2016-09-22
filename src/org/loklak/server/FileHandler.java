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
import java.util.ArrayList;
import java.util.List;
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
import org.loklak.tools.ByteBuffer;
import org.loklak.tools.UTF8;


public class FileHandler extends ResourceHandler implements Handler {
    
    private final long CACHE_LIMIT = 128L * 1024L;
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
	if (mimeType == null && resource.getName().endsWith(".css")) mimeType = "text/css";
        super.doResponseHeaders(response, resource, mimeType);
        // modify the caching strategy of ResourceHandler
        setCaching(response, this.expiresSeconds);
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
            if(expiresSeconds == 0){
                fields.put(HttpHeader.CACHE_CONTROL, "public, no-store, max-age=" + Integer.toString(expiresSeconds)); // seconds
            }
            else {
                fields.put(HttpHeader.CACHE_CONTROL, "public, max-age=" + Integer.toString(expiresSeconds)); // seconds
            }
        } else {
            response.setHeader(HttpHeader.LAST_MODIFIED.asString(), ""); // not really the best wqy to remove this header but there is no other option
            if(expiresSeconds == 0){
                response.setHeader(HttpHeader.CACHE_CONTROL.asString(), "public, no-store, max-age=" + Integer.toString(expiresSeconds));
            }
            else{
                response.setHeader(HttpHeader.CACHE_CONTROL.asString(), "public, max-age=" + Integer.toString(expiresSeconds));
            }

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
            if (f.isDirectory() && !path.equals("/")) return resource;
            CacheResource cache = resourceCache.get(f);
            if (cache != null) return cache;
            if (f.length() < CACHE_LIMIT || f.getName().endsWith(".html") || path.equals("/")) {
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
    private final static byte[] SSI_START = "<!--#include file=\"".getBytes();
    private final static byte[] SSI_END   = "\" -->".getBytes();
    
    private static class CacheResource extends Resource {

        private byte[] buffer;
        private long lastModified;
        private File file;
        private List<File> includes;
        
        public CacheResource(PathResource pathResource) throws IOException {
            this.file = pathResource.getFile();
            if (this.file.isDirectory()) this.file = new File(this.file, "index.html");
            this.includes = new ArrayList<>(8);
            initCache(System.currentTimeMillis());
            pathResource.close();
            
        }
        
        private void initCache(long nextLastModified) throws IOException {
            this.buffer = Files.readAllBytes(this.file.toPath());
            if (this.file.getName().endsWith(".html")) this.buffer = insertSSI(this.buffer);
            this.lastModified = nextLastModified;
        }
        
        private byte[] insertSSI(byte[] b) throws IOException {
            this.includes.clear();
            for (int p = findSSI_start(b, 0); p >= 0; p = findSSI_start(b, p)) {
                int q = findSSI_end(b, p);
                if (q < 0) break;
                byte[] f = new byte[q - p - SSI_START.length];
                System.arraycopy(b, p + SSI_START.length, f, 0, f.length);
                File ff = new File(this.file.getParent(), UTF8.String(f)).getCanonicalFile();
                if (!ff.exists()) {
                    byte[] b0 = new byte[b.length - (q - p) - SSI_END.length];
                    System.arraycopy(b, 0, b0, 0, p);
                    System.arraycopy(b, q + SSI_END.length, b0, p, b.length - q - SSI_END.length);
                    b = b0;
                    continue;
                }
                this.includes.add(ff);
                byte[] i = Files.readAllBytes(ff.toPath());
                byte[] b0 = new byte[b.length - (q - p) - SSI_END.length + i.length];
                System.arraycopy(b, 0, b0, 0, p);
                System.arraycopy(i, 0, b0, p, i.length);
                System.arraycopy(b, q + SSI_END.length, b0, p + i.length, b.length - q - SSI_END.length);
                b = b0;
            }
            return b;
        }

        private int findSSI_start(byte[] b, int p) {
            return ByteBuffer.indexOf(b, SSI_START, p);
        }
        
        private int findSSI_end(byte[] b, int p) {
            return ByteBuffer.indexOf(b, SSI_END, p);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        private long actualLastModified() {
            long l = this.file.lastModified();
            for (File d: this.includes) l = Math.max(l, d.lastModified());
            return l;
        }
        
        @Override
        public long lastModified() {
            long l = actualLastModified();
            if (actualLastModified() > this.lastModified) try {initCache(l);} catch (IOException e) {}
            this.lastModified = l;
            return this.lastModified;
        }

        @Override
        public long length() {
            return this.buffer.length;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            long l = actualLastModified();
            if (actualLastModified() > this.lastModified) initCache(l);
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
