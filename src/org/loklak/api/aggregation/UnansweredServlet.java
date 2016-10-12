/**
 *  UnansweredServlet
 *  Copyright 12.10.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.aggregation;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.DAO;
import org.loklak.http.RemoteAccess;
import org.loklak.server.FileHandler;
import org.loklak.server.Query;
import org.loklak.tools.UTF8;

public class UnansweredServlet extends HttpServlet {

    private static final long serialVersionUID = -7095346224124198L;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);

        final StringBuilder buffer = new StringBuilder(1000);
        Set<String> unanswered = DAO.susi.getUnanswered();
        Set<String> sorted = new TreeSet<>();
        sorted.addAll(unanswered);
        appendL(buffer, sorted, 0, 5);
        appendL(buffer, sorted, 5, 20);
        appendL(buffer, sorted, 20, Integer.MAX_VALUE);
        
        FileHandler.setCaching(response, 60);
        post.setResponse(response, "text/plain");
        response.getOutputStream().write(UTF8.getBytes(buffer.toString()));
        post.finalize();
    }
    
    private static void appendL(StringBuilder buffer, Collection<String> c, int minLen, int maxLimit) {
        for (String s: c) if (s.length() >= minLen && s.length() < maxLimit) {
            buffer.append(s);
            buffer.append('\n');
        }
    }
    
}
