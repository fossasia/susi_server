package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authentication;
import ai.susi.server.Authorization;
import ai.susi.server.ClientCredential;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by @hkedia321 on 8/6/18.
 * This Service uploads an image on the server
 * Can be tested on :-
 * http://127.0.0.1:4000/cms/uploadImage.json
 */

@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB
        maxFileSize=1024*1024*50,       // 50 MB
        maxRequestSize=1024*1024*100)       // 100 MB
public class UploadImageService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 2461878195432910492L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/uploadImage.json";
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Access-Control-Allow-Origin", "*"); // enable CORS
        String userEmail = null;
        JSONObject json = new JSONObject();
        Part imagePart = req.getPart("image");
        if (req.getParameter("access_token") != null) {
            if (imagePart == null) {
                json.put("accepted", false);
                json.put("message", "Image not given");
            } else {
                InputStream imagePartContent = imagePart.getInputStream();

                String image_name = req.getParameter("image_name");

                if (req.getParameter("access_token") != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
                    ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, req.getParameter("access_token"));
                    Authentication authentication = DAO.getAuthentication(credential);

                    // check if access_token is valid
                    if (authentication.getIdentity() != null) {
                        ClientIdentity identity = authentication.getIdentity();
                        userEmail = identity.getName();
                    }
                }
                String imagePath = DAO.susi_data_watch_dir + File.separator + "botbuilder" + File.separator + userEmail + File.separator + "images";
                if (image_name == null) {
                    // Checking for
                    json.put("accepted", false);
                    json.put("message", "The Image name are not given or Image with same name is already present ");

                } else {
                    // Checking for file existence
                    json.put("accepted", false);

                    // Reading content for image
                    Image image = ImageIO.read(imagePartContent);
                    BufferedImage bi = this.toBufferedImage(image);
                    // Checks if images directory exists or not. If not then create one
                    if (!Files.exists(Paths.get(imagePath))) new File(imagePath).mkdirs();
                    File p = new File(imagePath + File.separator + image_name);
                    if (p.exists()) p.delete();
                    ImageIO.write(bi, "jpg", new File(imagePath + File.separator + image_name));

                    //Add to git
                    try (Git git = DAO.getGit()) {
                        git.add().addFilepattern(".").call();

                            // commit the changes
                        DAO.pushCommit(git, "Created image " + image_name, userEmail);
                        json.put("accepted", true);

                    } catch (IOException | GitAPIException e) {
                        e.printStackTrace();
                        json.put("message", "error: " + e.getMessage());

                    }
                }
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
        else{
            json.put("message","Access token are not given");
            json.put("accepted",false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }
    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

    // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

    // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

    // Return the buffered image
        return bimage;
    }
    /**
     * Utility method to get file name from HTTP header content-disposition
     */
    private static String getFileName(Part part) {
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

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        return new ServiceResponse("");
    }
}





