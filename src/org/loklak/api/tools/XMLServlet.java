/**
 *  XML Servlet
 *  Copyright 06.06.2016 by Sudheesh Singanamalla, @sudheesh001
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

package org.loklak.api.tools;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.json.XML;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;

public class XMLServlet extends HttpServlet {

    private static final long serialVersionUID = 8578478303032749879L;
	
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);

        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        // evaluate get parameters
        String data = post.get("data", "");
        try {
        	String jsonData = XML.toJSONObject(data).toString();
        	JSONObject json = new JSONObject(jsonData);
        	PrintWriter sos = response.getWriter();
        	sos.print(json.toString(2));
        	sos.println();
        }
        catch (IOException e) {
        	Log.getLog().warn(e);
        	JSONObject json = new JSONObject(true);
        	json.put("error", "Malformed XML. Please check XML Again");
        	json.put("type", "Error");
        	PrintWriter sos = response.getWriter();
        	sos.print(json.toString(2));
        	sos.println();
        }

        post.finalize();
    }

}
