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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

public class FileHandler extends ResourceHandler implements Handler {
    
    private int expiresSeconds = 0;
    
    /**
     * cerate a custom ResourceHandler with more caching
     * @param expiresSeconds the time each file shall stay in the cache
     */
    public FileHandler(int expiresSeconds) {
        this.expiresSeconds = expiresSeconds;
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
}
