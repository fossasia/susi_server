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

package ai.susi.server.api.susi;

import ai.susi.DAO;
import ai.susi.mind.SusiMemory.TokenMapList;
import ai.susi.server.FileHandler;
import ai.susi.server.Query;
import ai.susi.server.RemoteAccess;
import ai.susi.tools.UTF8;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

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
        List<TokenMapList> tokenstats = DAO.susi.unanswered2tokenizedstats();
        tokenstats.forEach(tml -> {
            LinkedHashMap<String, Integer> m = tml.getMap();
            buffer.append("TOKEN \"");
            buffer.append(tml.getToken());
            buffer.append("\" (");
            buffer.append(tml.getCounter());
            buffer.append(")\n");
            m.forEach((k,v) -> {
                buffer.append(k);
                buffer.append(" (");
                buffer.append(v);
                buffer.append(")\n");
            });
            buffer.append("\n");
            
        });
        
        /*
        LinkedHashMap<String, Integer> unanswered = MapTools.sortByValue(DAO.susi.getUnanswered());
        for (Map.Entry<String, Integer> entry: unanswered.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append(" (");
            buffer.append(entry.getValue());
            buffer.append(")\n");
        }
        */
        
        FileHandler.setCaching(response, 60);
        post.setResponse(response, "text/plain");
        response.getOutputStream().write(UTF8.getBytes(buffer.toString()));
        post.finalize();
    }
    
}
