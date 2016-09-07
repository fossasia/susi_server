/**
 *  WeiboUserInfo
 *  Copyright 22.07.2016 by Damini Satya, @daminisatya
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
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WeiboUserInfo extends HttpServlet {
	
	private static final long serialVersionUID = 4653635987712691127L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Query post = RemoteAccess.evaluate(request);

		// manage DoS
		if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
		
		String url = post.get("url", "");
		JSONObject obj = new JSONObject();
		Document doc = Jsoup.connect(url).get();
		Elements infos;
		infos=doc.getElementsByAttributeValue("class", "li_1 clearfix");

		if (infos!=null) {
			Element info;
			String profile;
			for(int i=0;i<infos.size();i++){
				 info=infos.get(i);
				if (info.getElementsByAttributeValueContaining("href", "loc=infblog").size()==0) {
					profile=info.getElementsByAttributeValue("class","pt_detail").first().text().trim();
					obj.put("pro", profile);
					switch(info.getElementsByAttributeValue("class", "pt_title S_txt2").first().text()){
					case "Nickname：":
						obj.put("username", profile);
						break;
					case "Location：":
						obj.put("Address", profile);
						break;
					case "Gender：":
						obj.put("Gender", profile);
						break;
					case "性取向：":
						obj.put("Sexuality", profile.replace("t", "").replace("rn", ""));
						break;
					case "感情状况：":
						obj.put("Relationship", profile.replace("t", "").replace("rn", ""));
						break;
					case "Birthday：":
						obj.put("Birthday", profile);
						break;
					case "血型：":
						obj.put("Blood", profile);
						break;
					case "Domain Name：":
						if(info.getElementsByAttributeValueContaining("href", "loc=infdomain").size()!=0)
						profile=info.select("a").text();
						obj.put("Personaldomain", profile);
						break;
					case "简介：":
						obj.put("Profile", profile);
						break;
					case "Registration：":
						obj.put("Registertime", profile.replace("t", "").replace("rn", ""));
						break;
					case "Email：":
						obj.put("Email", profile);
						break;
					case "QQ：":
						obj.put("Qq", profile);
						break;
					case "大学：":
						obj.put("College", profile.replace("t", "").replace("rn", ""));
						break;
					case "Tags：":
						obj.put("Tag", profile.replace("t", "").replace("rn", ""));
						break;
					}

				}else {
					String blogurl=info.select("a").text();
					obj.put("Blog", blogurl);
				}	
			}
		}

		//print JSON 
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(obj.toString(2));
		sos.println();
	}
}
