/**
 *  Sitemap
 *  Copyright 15.06.2016 by Shiven Mian, @shivenmian
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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;

public class Sitemap extends HttpServlet{

	private static final long serialVersionUID = -8475570405765656976L;
	private final String sitemaphead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n";
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Query post = RemoteAccess.evaluate(request);
		//String siteurl = request.getRequestURL().toString();
		//String baseurl = siteurl.substring(0, siteurl.length() - request.getRequestURI().length()) + request.getContextPath() + "/";
		String baseurl = "http://loklak.org/";
		JSONObject TopMenuJsonObject = new TopMenuService().serviceImpl(post, null, null);
		JSONArray sitesarr = TopMenuJsonObject.getJSONArray("items");
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(sitemaphead + "\n");
		for(int i = 0; i < sitesarr.length(); i++){
			JSONObject sitesobj = sitesarr.getJSONObject(i);
			Iterator<String> sites = sitesobj.keys();
			sos.print("<url>\n<loc>" + baseurl + sitesobj.getString(sites.next().toString()) + "/</loc>\n" + "<changefreq>weekly</changefreq>\n</url>\n");
		}
		sos.print("</urlset>");
		sos.println();
		post.finalize();
	}
}
