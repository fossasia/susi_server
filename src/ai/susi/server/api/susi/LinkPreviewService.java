/**
 *  LinkPreviewService
 *  Copyright 24/07/17 by Chetan Kaushik, @dynamitechetan
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

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.broadbear.link.preview.SourceContent;
import org.broadbear.link.preview.TextCrawler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by chetankaushik on 25/07/17.
 */
public class LinkPreviewService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 1463185662941444503L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/linkPreview.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject jsonObject = new JSONObject();
        String url = post.get("url", "");
        if(url==null || url.isEmpty()){
            jsonObject.put("message","URL Not given");
            jsonObject.put("accepted",false);
            return new ServiceResponse(jsonObject);
        }
        SourceContent sourceContent = 	TextCrawler.scrape(url,3);
        if (sourceContent.getImages() != null) jsonObject.put("image", sourceContent.getImages().get(0));
        if (sourceContent.getDescription() != null) jsonObject.put("descriptionShort", sourceContent.getDescription());
        if(sourceContent.getTitle()!=null)jsonObject.put("title", sourceContent.getTitle());
        jsonObject.put("accepted",true);
        return new ServiceResponse(jsonObject);
    }

}
