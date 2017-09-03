package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by saurabh on 7/6/17.
 * Modified by Chetan Kaushik on 4/8/17
 * This Service creates an skill as per given query.
 * The skill name given in the query should not exist in the SUSI intents Folder
 * Can be tested on :-
 * http://localhost:4000/cms/createSkill.txt?model=general&group=Knowledge&language=en&skill=whois&content=skillData
 */

@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB
        maxFileSize=1024*1024*50,      	// 50 MB
        maxRequestSize=1024*1024*100)   	// 100 MB
public class CreateSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 2461878194569824151L;

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
        return "/cms/createSkill.json";
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Access-Control-Allow-Origin", "*"); // enable CORS
        String userEmail = null;
        JSONObject json = new JSONObject();
        Part file = req.getPart("image");
        if (req.getParameter("access_token") != null) {
            if (file == null) {
                json.put("accepted", false);
                json.put("message", "Image not given");
            } else {
                String filename = getFileName(file);
                InputStream filecontent = file.getInputStream();

                String model_name = req.getParameter("model");
                if (model_name == null) {
                    model_name = "general";
                }
                File model = new File(DAO.model_watch_dir, model_name);
                String group_name = req.getParameter("group");
                if (group_name == null) {
                    group_name = "Knowledge";
                }
                File group = new File(model, group_name);
                String language_name = req.getParameter("language");
                if (language_name == null) {
                    language_name = "en";
                }
                File language = new File(group, language_name);
                String skill_name = req.getParameter("skill");

                File skill = new File(language, skill_name + ".txt");

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

                Path p = Paths.get(language + File.separator + "images/" + image_name);
                if (image_name == null || Files.exists(p)) {
                    // Checking for
                    json.put("accepted", false);
                    json.put("message", "The Image name not given or Image with same name is already present ");
                } else {
                    // Checking for file existence
                    json.put("accepted", false);
                    if (skill.exists()) {
                        json.put("message", "The '" + skill + "' already exists.");
                    } else {
                        // Reading Content for skill
                        String content = req.getParameter("content");
                        if (content == null) {
                            content = "";
                        }
                        // Reading content for image
                        Image image = ImageIO.read(filecontent);
                        BufferedImage bi = this.createResizedCopy(image, 512, 512, true);

                        // Checks if images directory exists or not. If not then create one
                        if (!Files.exists(Paths.get(language.getPath() + File.separator + "images"))) {
                            new File(language.getPath() + File.separator + "images").mkdirs();
                        }
                        ImageIO.write(bi, "jpg", new File(language.getPath() + File.separator + "images/" + image_name));

                        // Writing to Skill Data to File
                        try (FileWriter Skillfile = new FileWriter(skill)) {
                            Skillfile.write(content);
                            String path = skill.getPath().replace(DAO.model_watch_dir.toString(), "models");
                        } catch (IOException e) {
                            e.printStackTrace();
                            json.put("message", "error: " + e.getMessage());
                        }

                        //Add to git
                        try (Git git = DAO.getGit()) {
                            git.add().addFilepattern(".").call();

                            // commit the changes
                            DAO.pushCommit(git, "Created " + skill_name, userEmail);
                            json.put("accepted", true);

                        } catch (IOException | GitAPIException e) {
                            e.printStackTrace();
                            json.put("message", "error: " + e.getMessage());

                        }
                    }
                }
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
        else{
            json.put("message","Access token not given");
            json.put("accepted",false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }

    BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
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

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        return new ServiceResponse("");
    }
}
