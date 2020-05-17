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
import ai.susi.server.*;
import ai.susi.server.api.aaa.GetAvatarServlet;
import ai.susi.tools.IO;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/*
 This Servlet gives a API Endpoint to return image
 * Updated by @akshatnitd on 15/8/18.
 To get a public skill's image
 http://localhost:4000/cms/getImage.png?model=general&language=en&group=Food%20and%20Drink&image=images/cooking_guide.png

 To get a private skill's image
 http://localhost:4000/cms/getImage.png?access_token=k30RWlpTKvKM3TUiwi7E5a2bgtjJQa&language=en&group=Knowledge&image=images/susi%20icon.png

 To get image stored in local storage
 http://localhost:4000/cms/getImage.png?image=963e84467b92c0916b27d157b1d45328/1529692996805_susi icon.png
 */
 public class GetImageServlet extends HttpServlet {

    private static final long serialVersionUID = 628253297031919192L;

    public static File getDefaultImage() {
        return Paths.get(DAO.html_dir.getPath(), "images", "default.jpg").toFile();
    }

    public static File getAvatar(String file) {
        File imageFile = IO.resolvePath(Paths.get(DAO.data_dir.getPath(), "avatar_uploads"), file).toFile();
        if (!imageFile.exists()) {
            return getDefaultImage();
        }

        return imageFile;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Query post = RemoteAccess.evaluate(request);
        File imageFile = null;
        String file = "";

        String group = post.get("group","").trim();
        String language = post.get("language","").trim();
        String image = post.get("image","").trim();
        if (!group.isEmpty() && !language.isEmpty() && !image.isEmpty()) {
            file = image;
            // for public skill
            String model = post.get("model", "").trim();
            if (!model.isEmpty()) {
                imageFile = IO.resolvePath(DAO.model_watch_dir.toPath(), model, group, language, image).toFile();
            }
            // for private skill
            if (!post.get("access_token", "").equals("")) {
                String userId = "";
                ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, post.get("access_token",""));
                Authentication authentication = DAO.getAuthentication(credential);
                // check if access_token is valid
                if (authentication.getIdentity() != null) {
                    ClientIdentity identity = authentication.getIdentity();
                    userId = identity.getUuid();
                }
                imageFile = IO.resolvePath(DAO.private_skill_watch_dir.toPath(), userId, group, language, image).toFile();
            }
        }
        else if (!post.get("image", "").equals("")) {
            // for custom user's images
            String image_path = post.get("image","");
            file = image_path.substring(image_path.indexOf("/")+1);
            String showAvatar = post.get("avatar","false");
            if (showAvatar.equals("true")) {
                imageFile = getAvatar(file);
            } else {
                imageFile = IO.resolvePath(Paths.get(DAO.data_dir.getPath(), "image_uploads"), image_path).toFile();
            }
        }

        else if(!post.get("sliderImage", "").equals("")) {
            //For slider image
            String image_path = post.get("sliderImage", "");
            imageFile = IO.resolvePath(Paths.get(DAO.data_dir.getPath(), "slider_uploads"), image_path).toFile();
        }

        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            imageFile = getDefaultImage();
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try {
            data = IO.readFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GetAvatarServlet.setMimeType(request, response, post, file);

        ServletOutputStream sos = response.getOutputStream();
        sos.write(data.toByteArray());
        post.finalize();
    }
}
