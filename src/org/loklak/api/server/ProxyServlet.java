/**
 *  ProxyServlet
 *  Copyright 12.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.data.UserEntry;
import org.loklak.tools.Cache;

public class ProxyServlet extends HttpServlet {

    private static final long serialVersionUID = -9112326722297824443L;

    private final static Cache<String, byte[]> cache = new Cache<>(1000);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        process(request, response, post);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        post.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, post);
    }
    
    // http://localhost:9000/api/proxy.png?screen_name=loklak_app&url=https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_bigger.png
    
    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
        if (!post.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost"); return;}
        
        // parse arguments
        String url = post.get("url", "");
        String screen_name = post.get("screen_name", "");
        
        if (url.length() == 0 || screen_name.length() == 0) {response.sendError(503, "attributes url and screen_name must be submitted"); return;}

        // read the buffer
        UserEntry user = null;
        byte[] buffer = cache.get(url);
        if (buffer == null && isProfileImage(url)) {
            // try to read it from the user profiles
            user = DAO.searchLocalUser(screen_name);
            if (user != null) {
                buffer = user.getProfileImage();
            }
        }
        
        if (buffer == null) {            
            try {
                ClientConnection connection = new ClientConnection(url);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                buffer = new byte[2048];
                int count;
                try {
                    while ((count = connection.inputStream.read(buffer)) > 0) baos.write(buffer, 0, count);
                } catch (IOException e) {}
                connection.close();
                buffer = baos.toByteArray();
                
                // write the buffer
                if (user != null && user.getType().length() > 0) {
                    user.setProfileImage(buffer);
                    DAO.writeUser(user, user.getType());
                } else {
                    cache.put(url, buffer);
                }
            } catch (IOException e) {
                response.sendError(503, "resource not available"); return;
            }
        }

        if (url.endsWith(".png")) post.setResponse(response, "image/png");
        else if (url.endsWith(".gif")) post.setResponse(response, "image/gif");
        else if (url.endsWith(".jpg")) post.setResponse(response, "image/jpeg");
        else post.setResponse(response, "application/octet-stream");

        ServletOutputStream sos = response.getOutputStream();
        sos.write(buffer);
    }
    
    private boolean isProfileImage(String url) {
        return url.indexOf("pbs.twimg.com/profile_images") > 0 && url.endsWith("_bigger.png");
    }
}
