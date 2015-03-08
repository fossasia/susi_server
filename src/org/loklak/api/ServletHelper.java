/**
 *  ServletHelper
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

/**
 * Helper class to provide get and post form value maps
 */
public class ServletHelper {

    public static Map<String, String> getQueryMap(String query)   {  
        if (query == null) return null;
        String[] params = query.split("&");  
        Map<String, String> map = new HashMap<String, String>();  
        for (String param : params) {
            int p = param.indexOf('=');
            if (p >= 0)
                try {map.put(param.substring(0, p), URLDecoder.decode(param.substring(p + 1), "UTF-8"));} catch (UnsupportedEncodingException e) {}
        }  
        return map;  
    }  
    
    public static Map<String, String> getPostMap(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            final char[] buffer = new char[1024];
            for (Part part: request.getParts()) {
                String name = part.getName();
                InputStream is = part.getInputStream();
                final StringBuilder out = new StringBuilder();
                final Reader in = new InputStreamReader(is, "UTF-8");
                int c;
                try {while ((c = in.read(buffer, 0, buffer.length)) > 0) {
                    out.append(buffer, 0, c);
                }} finally {is.close();}
                map.put(name, out.toString());
            }
        } catch (IOException e) {
        } catch (ServletException e) {
        }
        return map;
    }
    
}
