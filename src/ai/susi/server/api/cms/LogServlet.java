/**
 *  LogServlet
 *  Copyright 03.08.2018 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.FileHandler;
import ai.susi.server.Query;
import ai.susi.server.RemoteAccess;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LogServlet extends HttpServlet {

    private static final long serialVersionUID = -7095346222464124198L;

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        int count = post.get("count", 1000);

        final StringBuilder buffer = new StringBuilder(1000);

        List<String> lines = DAO.logAppender.getLines(count);
        for (int i = 0; i < lines.size(); i++) buffer.append(lines.get(i));
        
        FileHandler.setCaching(response, 10);
        post.setResponse(response, "text/plain");
        AbstractAPIHandler.setCORS(response);
        response.getOutputStream().write(buffer.toString().getBytes(StandardCharsets.UTF_8));
        post.finalize();
    }
}
