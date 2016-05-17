/**
 *  AbstractAPIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@SuppressWarnings("serial")
public abstract class AbstractAPIHandler extends HttpServlet implements APIHandler {

    private String[] serverProtocolHostStub = null;

    public AbstractAPIHandler() {
        this.serverProtocolHostStub = null;
    }
    
    public AbstractAPIHandler(String[] serverProtocolHostStub) {
        this.serverProtocolHostStub = serverProtocolHostStub;
    }
    
    @Override
    public JSONObject Service(JSONObject call) {
        return null;
    }
    @Override
    public String[] getServerProtocolHostStub() {
        return this.serverProtocolHostStub;
    }

    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(HTTPAccount customer) {
        return null;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    abstract public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

}
