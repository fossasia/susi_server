/**
 *  GetImageService
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

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONObject;
import org.apache.commons.io.FileUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import java.nio.file.Files;
import ai.susi.server.Query;
import ai.susi.server.RemoteAccess;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
/**
 This Servlet gives a API Endpoint to return image
 To get a public skill's image
 http://localhost:4000/cms/getImage.png?model=general&language=en&group=Food%20and%20Drink&image=images/cooking_guide.png
    
 To get a private skill's image
 http://localhost:4000/cms/getImage.png?access_token=k30RWlpTKvKM3TUiwi7E5a2bgtjJQa&language=en&group=Knowledge&image=images/susi%20icon.png

 To get image stored in local storage
 http://localhost:4000/cms/getImage.png?image=963e84467b92c0916b27d157b1d45328/1529692996805_susi icon.png
 */
 public class GetImageServlet extends HttpServlet {

    private static final long serialVersionUID = 628253297031919192L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Query post = RemoteAccess.evaluate(request);
        File imageFile = null;
        String file = "";

        if (post.get("group","") != "" && post.get("language","") != "" && post.get("image","") != "") {
            String group = post.get("group","");
            String language = post.get("language","");
            String image = post.get("image","");
            file = image;
            // for public skill
            if (post.get("model","") != "") {
                String model = post.get("model","");
                imageFile = new File(DAO.model_watch_dir  + File.separator + model + File.separator + group + File.separator + language + File.separator + image);
            } 
            // for private skill
            if (post.get("access_token","") != "") {
                String userId = "";
                ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, post.get("access_token",""));
                Authentication authentication = DAO.getAuthentication(credential);
                // check if access_token is valid
                if (authentication.getIdentity() != null) {
                    ClientIdentity identity = authentication.getIdentity();
                    userId = identity.getUuid();
                }
                imageFile = new File(DAO.private_skill_watch_dir  + File.separator + userId + File.separator + group + File.separator + language + File.separator + image);
            }
        }
        else if (post.get("image","") != "") {
        // for custom user's images
            String image_path = post.get("image","");
            file = image_path.substring(image_path.indexOf("/")+1);
            imageFile = new File(DAO.data_dir  + File.separator + "image_uploads" + File.separator+ image_path);
        }

        if (imageFile == null || !imageFile.exists()) {response.sendError(503, "image does not exist"); return;}
        
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] b = new byte[2048];
        InputStream is = new BufferedInputStream(new FileInputStream(imageFile));
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
