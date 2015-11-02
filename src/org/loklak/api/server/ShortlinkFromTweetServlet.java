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

package org.loklak.api.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.http.RemoteAccess;

public class ShortlinkFromTweetServlet extends HttpServlet {

    private static final long serialVersionUID = 5632263908L;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final RemoteAccess.Post post = RemoteAccess.evaluate(request);
        String id = post.get("id", "");
        if (id.length() == 0) {response.sendError(503, "bad request (id missing)"); return;}
        
        // search for tweet with id
        MessageEntry message = DAO.readMessage(id);
        if (message == null) {response.sendError(503, "bad request (message with id=" + id + " unknown)"); return;}
        
        // read link in message
        String[] links = message.getLinks();
        if (links.length != 1) {response.sendError(503, "bad request (message with id=" + id + " must have exactly one link)"); return;}
        
        response.sendRedirect(links[0]);
    }
}