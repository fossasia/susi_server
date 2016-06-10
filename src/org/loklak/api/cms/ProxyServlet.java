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

package org.loklak.api.cms;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.data.IndexEntry;
import org.loklak.harvester.TwitterAPI;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.UserEntry;
import org.loklak.server.Query;
import org.loklak.tools.CacheMap;

import twitter4j.TwitterException;

public class ProxyServlet extends HttpServlet {

    private static final long serialVersionUID = -9112326722297824443L;

    private final static CacheMap<String, byte[]> cache = new CacheMap<>(1000);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        process(request, response, post);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        post.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, post);
    }
    
    // http://localhost:9000/api/proxy.png?screen_name=loklak_app&url=https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_bigger.png
    
    protected void process(HttpServletRequest request, HttpServletResponse response, Query post) throws ServletException, IOException {
        
        // parse arguments
        String url = post.get("url", "");
        String screen_name = post.get("screen_name", "");
        DAO.log("PROXY: called with screen_name=" + screen_name + ", url=" + url);
        
        if (screen_name.length() == 0 && (url.length() == 0 || screen_name.indexOf("twimg.com") < 0)) {
            response.sendError(503, "either attributes url or screen_name or both must be submitted"); return;
        }
        
        byte[] buffer = url.length() == 0 ? null : cache.get(url);
        if (buffer != null) DAO.log("PROXY: got url=" + url + " content from ram cache!");
        UserEntry user = null;
        
        if (buffer == null && screen_name.length() > 0) {
            if (buffer == null && (url.length() == 0 || isProfileImage(url))) {
                // try to read it from the user profiles
                user = DAO.searchLocalUserByScreenName(screen_name);
                if (user != null) {
                    buffer = user.getProfileImage();
                    if (buffer != null) DAO.log("PROXY: got url=" + url + " content from user profile bas64 cache!");
                    if (url.length() == 0) url = user.getProfileImageUrl();
                    cache.put(user.getProfileImageUrl(), buffer);
                }
            }
        }
        
        if (buffer == null && url.length() > 0) {
            // try to download the image
            buffer = ClientConnection.download(url);
            String newUrl = user == null ? null : user.getProfileImageUrl();
            if (buffer != null) {
                DAO.log("PROXY: downloaded given url=" + url + " successfully!");
            } else if (newUrl != null && !newUrl.equalsIgnoreCase(url)) {
                // if this fails, then check if the stored url is different.
                // That may happen because new user avatar images get new urls
                buffer = ClientConnection.download(newUrl);
                if (buffer != null) DAO.log("PROXY: downloaded url=" + url + " from old user setting successfully!");
            }
            if (buffer == null) {
                // ask the Twitter API for new user data
                try {
                    JSONObject usermap = TwitterAPI.getUser(screen_name, true);
                    newUrl = usermap.has("profile_image_url") ? (String) usermap.get("profile_image_url") : null;
                    if (newUrl != null && newUrl.length() > 0 && !newUrl.startsWith("http:") && usermap.has("profile_image_url_https")) newUrl = (String) usermap.get("profile_image_url_https");
                    if (newUrl != null && newUrl.length() > 0) buffer = ClientConnection.download(newUrl);
                    if (buffer != null) DAO.log("PROXY: downloaded url=" + url + " from recently downloaded user setting successfully!");
                } catch (TwitterException e) {
                    DAO.log("ProxyServlet: call to twitter api failed: " + e.getMessage());
                }
            }
            if (buffer != null) {
                // write the buffer
                if (user != null) {
                    user.setProfileImageUrl(newUrl);
                    user.setProfileImage(buffer);
                    try {
                        // record user into search index
                        DAO.users.writeEntry(new IndexEntry<UserEntry>(user.getScreenName(), user.getType(), user));
                    } catch (IOException e) {
                    	Log.getLog().warn(e);
                    }
                    if (!cache.full()) cache.put(url, buffer);
                } else {
                    cache.put(url, buffer);
                }
            }
        }
        
        if (buffer == null) {
            if (screen_name.length() == 0) {
                response.sendError(503, "url cannot be loaded"); return;
            }
            if (url.length() == 0) {
                response.sendError(503, "user cannot be found"); return;
            }
            response.sendError(503, "url cannot be loaded and user cannot be found"); return;
        }

        if (url.endsWith(".png") || (url.length() == 0 && request.getServletPath().endsWith(".png"))) post.setResponse(response, "image/png");
        else if (url.endsWith(".gif") || (url.length() == 0 && request.getServletPath().endsWith(".gif"))) post.setResponse(response, "image/gif");
        else if (url.endsWith(".jpg") || url.endsWith(".jpeg") || (url.length() == 0 && request.getServletPath().endsWith(".jpg"))) post.setResponse(response, "image/jpeg");
        else post.setResponse(response, "application/octet-stream");

        ServletOutputStream sos = response.getOutputStream();
        sos.write(buffer);
        post.finalize();
    }
    
    private boolean isProfileImage(String url) {
        return url.indexOf("pbs.twimg.com/profile_images") > 0 && url.endsWith("_bigger.png");
    }
}
