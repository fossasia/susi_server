/**
 *  ShortlinkFromTweetServlet
 *  Copyright 2.11.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.search;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.MessageEntry;
import org.loklak.server.Query;

public class ShortlinkFromTweetServlet extends HttpServlet {

    private static final long serialVersionUID = 5632263908L;

    public final static char SHORTLINK_COUNTER_SEPERATOR = '*';
    
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final Query post = RemoteAccess.evaluate(request);
        String id = post.get("id", "");
        if (id.length() == 0) {response.sendError(503, "bad request (id missing)"); return;}
        
        // search for tweet with id
        int nth = 0;
        int p = id.indexOf(SHORTLINK_COUNTER_SEPERATOR);
        if (p >= 0) {
            // cut away the counter and use it to address the nth entry in the links array
            nth = Integer.parseInt(id.substring(p + 1));
            id = id.substring(0, p);
        }
        
        MessageEntry message = DAO.readMessage(id);
        if (message == null) {response.sendError(503, "bad request (message with id=" + id + " unknown)"); return;}
        
        // read link in message
        String[] links = message.getLinks();
        if (nth + 1 > links.length) {response.sendError(503, "bad request (message with id=" + id + " wrong number of links)"); return;}
        
        response.sendRedirect(links[nth]);
    }
}