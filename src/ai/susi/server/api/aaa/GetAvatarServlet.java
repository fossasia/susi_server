/**
 *  GetAvatarService
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
import ai.susi.server.*;

import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

/*
 This Servlet gives a API Endpoint to return avatar of a user based on the avatar
 type the user has in the settings. The access token of the user is required.
 http://localhost:4000/getAvatar.png?access_token=963e84467b92c0916b27d157b1d45328
 */

 public class GetAvatarServlet extends HttpServlet {

	private static final long serialVersionUID = 7408901113114682419L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Query post = RemoteAccess.evaluate(request);
        File imageFile = null;
        String file = "";
        String userId = "";
        String avatar_type = "";

        if (post.get("access_token", null) != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
            ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, post.get("access_token", null));
            Authentication authentication = DAO.getAuthentication(credential);
            // check if access_token is valid
            if (authentication.getIdentity() != null) {
                ClientIdentity identity = authentication.getIdentity();
                Accounting accounting = DAO.getAccounting(identity);
                JSONObject accountingObj = accounting.getJSON();
                if (accountingObj.has("settings") &&
                    accountingObj.getJSONObject("settings").has("avatarType")) {
                    avatar_type = accountingObj.getJSONObject("settings").getString("avatarType");
                    userId = identity.getUuid();
                    file = userId + ".jpg";
                } else {
                    avatar_type = "default";
                    file = "default.jpg";
                }
            }
        } else {
            avatar_type = "default";
            file = "default.jpg";
        }

        InputStream is = null;
        byte[] b = new byte[2048];
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        if(avatar_type.equals("gravatar")) {
        	String gravatarUrl = "https://www.gravatar.com/avatar/" + file;
        	URL url = new URL(gravatarUrl);
        	is = url.openStream();
        } else if(avatar_type.equals("server")) {
        	imageFile = new File(DAO.data_dir  + File.separator + "avatar_uploads" + File.separator + file);
            if (imageFile == null || !imageFile.exists()) {
                file = "default.jpg";
                imageFile = new File(DAO.html_dir  + File.separator + "images" + File.separator + file);
            }
            is = new BufferedInputStream(new FileInputStream(imageFile));
        } else {
            file = "default.jpg";
            imageFile = new File(DAO.html_dir  + File.separator + "images" + File.separator + file);
            is = new BufferedInputStream(new FileInputStream(imageFile));
        }

        int c;
        try {
            while ((c = is.read(b)) >  0) {data.write(b, 0, c);}
        } catch (IOException e) {}

        if (file.endsWith(".png") || (file.length() == 0 && request.getServletPath().endsWith(".png"))) post.setResponse(response, "image/png");
        else if (file.endsWith(".gif") || (file.length() == 0 && request.getServletPath().endsWith(".gif"))) post.setResponse(response, "image/gif");
        else if (file.endsWith(".jpg") || file.endsWith(".jpeg") || (file.length() == 0 && request.getServletPath().endsWith(".jpg"))) post.setResponse(response, "image/jpeg");
        else post.setResponse(response, "application/octet-stream");

        ServletOutputStream sos = response.getOutputStream();
        sos.write(data.toByteArray());
        post.finalize();
    }
}
