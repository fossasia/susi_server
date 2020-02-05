/**
 *  APIHandler
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

package ai.susi.server;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ai.susi.json.JsonObjectWithDefault;

/**
 * Interface for all servlets
 */
public interface APIHandler {


    public abstract UserRole getMinimalUserRole();

    public abstract JSONObject getDefaultPermissions(UserRole baseUserRole);

    /**
     * get the path to the servlet
     * @return the url path of the servlet
     */
    public String getAPIPath();

    /**
     * call the servlet with a query locally without a network connection
     * @return a Service Response
     * @throws IOException
     */
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException;

}