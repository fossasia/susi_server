/**
 *  UploadSettingsService
 *  Copyright 28/6/17 by Dravit Lochan, @DravitLochan
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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.IO;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by dravit on 28/6/17.
 * http://127.0.0.1:4000/aaa/uploadSettings.json
 */
public class UploadSettingsService extends AbstractAPIHandler implements APIHandler{
    /**
     * 
     */
    private static final long serialVersionUID = -1349057408501566674L;

    @Override
    public String getAPIPath() {
        return "/aaa/uploadSettings.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject(true);
        result.put("accepted", false);
        result.put("message", "Error: Unable to Upload file");

        Path path = IO.resolvePath(DAO.data_dir.toPath(), "settings");

        //Get all the parts from request and write it to the file on server
        try {
            HttpServletRequest request = post.getRequest();
            for (Part part : request.getParts()) {
                String fileName = getFileName(part);
                part.write(IO.resolvePath(path, fileName).toString());
                request.setAttribute("message", fileName + " File uploaded successfully!");
            }
            result.put("accepted", true);
        } catch (IOException | ServletException e) {
            e.printStackTrace();
            result.put("accepted", false);
        }
        return new ServiceResponse(result);
    }

    /**
     * Utility method to get file name from HTTP header content-disposition
     */
    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= "+contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length()-1);
            }
        }
        return "";
    }
}
