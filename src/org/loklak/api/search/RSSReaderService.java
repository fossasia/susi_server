/**
 *  RSS Reader
 *  Copyright 23.06.2016 by Jigyasa Grover, @jig08
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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.susi.SusiThought;
import org.loklak.tools.storage.JSONObjectWithDefault;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RSSReaderService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 1463185662941444503L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/api/rssreader.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights, final JSONObjectWithDefault permissions) throws APIException {
		String url = post.get("url", "");
		return readRSS(url);
    }
		
    public static SusiThought readRSS(String url) {
    	
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = null;
        
		try {
            ClientConnection connection = new ClientConnection(url);
		    XmlReader xmlreader = new XmlReader(connection.inputStream);
			feed = input.build(xmlreader);
		} catch (Exception e) {
			e.printStackTrace();
		}

		@SuppressWarnings("unused")
		int totalEntries = 0;
		int i = 0;

		JSONArray jsonArray = new JSONArray();

		// Reading RSS Feed from the URL
		for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("title", entry.getTitle().toString());
			jsonObject.put("link", entry.getLink().toString());
			jsonObject.put("uri", entry.getUri().toString());
			jsonObject.put("guid", Integer.toString(entry.hashCode()));
			if (entry.getPublishedDate() != null) jsonObject.put("pubDate", entry.getPublishedDate().toString());
			if (entry.getUpdatedDate() != null) jsonObject.put("updateDate", entry.getUpdatedDate().toString());
			if (entry.getDescription() != null) jsonObject.put("description", entry.getDescription().toString());

			jsonArray.put(i, jsonObject);

			i++;
		}

		totalEntries = i;

		SusiThought json = new SusiThought();
		json.setData(jsonArray);
		return json;
	}
}
