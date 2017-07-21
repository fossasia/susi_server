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

package ai.susi.server.api.susi;

import java.util.List;

import org.broadbear.link.preview.SourceContent;
import org.broadbear.link.preview.TextCrawler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiThought;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.ClientConnection;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import javax.servlet.http.HttpServletResponse;

public class RSSReaderService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 1463185662941444503L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/rssreader.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
		String url = post.get("url", "");
		return new ServiceResponse(readRSS(url).toJSON());
    }
		
    @SuppressWarnings("unchecked")
    public static SusiThought readRSS(String url) {
    	
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = null;
        
		try {
            ClientConnection connection = new ClientConnection(url);
		    XmlReader xmlreader = new XmlReader(connection.inputStream);
			feed = input.build(xmlreader);
		} catch (Exception e) {
			e.printStackTrace();
			return new SusiThought(); // fail
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
			SourceContent sourceContent = 	TextCrawler.scrape(entry.getUri(),3);
			if (entry.getPublishedDate() != null) jsonObject.put("pubDate", entry.getPublishedDate().toString());
			if (entry.getUpdatedDate() != null) jsonObject.put("updateDate", entry.getUpdatedDate().toString());
			if (entry.getDescription() != null) jsonObject.put("description", entry.getDescription().getValue().toString());
			if (sourceContent.getImages() != null) jsonObject.put("image", sourceContent.getImages().get(0));
			if (sourceContent.getDescription() != null) jsonObject.put("descriptionShort", sourceContent.getDescription());

			jsonArray.put(i, jsonObject);

			i++;
		}

		totalEntries = i;

		SusiThought json = new SusiThought();
		json.setData(jsonArray);
		return json;
	}
}
