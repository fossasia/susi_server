/**
 * Created by saurabh on 7/6/17.
 * Modified by Chetan Kaushik on 4/8/17
 */

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.SkillTransactions;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import ai.susi.tools.DateParser;
import ai.susi.tools.IO;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;


@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB
        maxFileSize=1024*1024*50,       // 50 MB
        maxRequestSize=1024*1024*100)       // 100 MB

/*
 * This Service creates an skill as per given query.
 * The skill name given in the query should not exist in the SUSI intents Folder
 * Can be tested on :-
 * http://localhost:4000/cms/createSkill.txt?model=general&group=Knowledge&language=en&skill=whois&content=skillData
 */
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
        Part imagePart = req.getPart("image");

        if (req.getParameter("access_token") != null) {
            if(!req.getParameter("skill").isEmpty()) {
                if (imagePart == null) {
                    json.put("accepted", false);
                    json.put("message", "Image not given");
                } else {
                    String userId = null;
                    if (req.getParameter("access_token") != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
                        ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, req.getParameter("access_token"));
                        Authentication authentication = DAO.getAuthentication(credential);
                        // check if access_token is valid
                        if (authentication.getIdentity() != null) {
                            ClientIdentity identity = authentication.getIdentity();
                            userEmail = identity.getName();
                            userId = identity.getUuid();
                        }
                    }
                    if (userId != null) {
                        SkillQuery skillQuery = SkillQuery.getParser().parse(req);
                        String model_name = skillQuery.getModel();
                        String group_name = skillQuery.getGroup();
                        String language_name = skillQuery.getLanguage();
                        String skill_name = skillQuery.getSkill();

                    // if client sends private=1 then it is a private skill
                    String privateSkill = req.getParameter("private");
                    if(privateSkill != null){
                        skillQuery = skillQuery.forPrivate(userId);
                    }
                    InputStream imagePartContent = imagePart.getInputStream();

                    File skill = skillQuery.getSkillFile();

                    String image_name = req.getParameter("image_name");

                    Path imagePath = IO.resolvePath(skillQuery.getLanguagePath(), "images");
                    if (image_name == null) {
                        // Checking for
                        json.put("accepted", false);
                        json.put("message", "The Image name are not given or Image with same name is already present ");

                    } else {
                        // Checking for file existence
                        json.put("accepted", false);
                        if (skill.exists()) {
                            json.put("message", "The '" + skill + "' already exists.");
                        } else {
                            // Reading Content for skill
                            String content = req.getParameter("content");
                            if (content == null) content = "";

                            // Reading content for image
                            Image image = ImageIO.read(imagePartContent);
                            BufferedImage bi = createResizedCopy(image, 512, 512, true);

                            // Checks if images directory exists or not. If not then create one
                            if (!Files.exists(imagePath)) imagePath.toFile().mkdirs();
                            File p = IO.resolvePath(imagePath, image_name).toFile();
                            if (p.exists()) p.delete();
                            ImageIO.write(bi, "jpg", p);

                            // Writing Skills Data in File
                            try (FileWriter Skillfile = new FileWriter(skill)) {
                                Skillfile.write(content);
                                // Set the creationTime in the metadata
                                updateSkillInfo(model_name, group_name, language_name, skill_name);
                            } catch (IOException e) {
                                e.printStackTrace();
                                json.put("message", "error: " + e.getMessage());
                            }

                            //Add to git
                            if (privateSkill != null){
                                storePrivateSkillBot(skill, userId, skill_name, group_name, language_name);
                                SkillTransactions.addAndPushCommit(true, "Created " + skill_name, userEmail);
                                json.put("accepted", true);
                            } else {
                                SkillTransactions.addAndPushCommit(false, "Created " + skill_name, userEmail);
                                json.put("accepted", true);
                            }

                        }
                    }
                }
                else {
                    json.put("message","Access token not valid");
                    json.put("accepted",false);
                }
                }
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(json.toString());
            } else {
                json.put("message", "Skill name is not given");
                json.put("accepted",false);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(json.toString());
            }
        }
        else{
            json.put("message","Access token are not given");
            json.put("accepted",false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }

    private static void updateSkillInfo(String model_name, String group_name, String language_name, String skill_name ) {
        JsonTray skillInfo = DAO.skillInfo;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        DateFormat dateFormatType = DateParser.iso8601Format;
        String skillUpdateTime = dateFormatType.format(new Date());

        if (skillInfo.has(model_name)) {
            modelName = skillInfo.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                        skillName.put("creationTime",skillUpdateTime);
                        skillName.put("lastModifiedTime",skillUpdateTime);
                        return;
                    }
                }
            }
        }
        skillName.put("creationTime",skillUpdateTime);
        skillName.put("lastModifiedTime",skillUpdateTime);
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillInfo.put(model_name, modelName, true);
        return;
    }

    public static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
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
    * Helper method to store the private skill bot of the user in chatbot.json file
    */
    private static void storePrivateSkillBot(File skill, String userId, String skill_name, String group_name, String language_name) {
        JsonTray chatbot = DAO.chatbot;
        JSONObject userName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject designObject = new JSONObject();
        JSONObject configObject = new JSONObject();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (chatbot.has(userId)) {
            userName = chatbot.getJSONObject(userId);
            if (userName.has(group_name)) {
                groupName = userName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                }
            }
        }
        // read design and configuration settings
        try {
            BufferedReader br = new BufferedReader(new FileReader(skill));
            String line = "";
            readloop: while ((line = br.readLine()) != null) {
                String linebeforetrim = line;
                line = line.trim();

                if (line.startsWith("::")) {
                  int thenpos=-1;
                  if (line.startsWith("::bodyBackgroundImage") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("bodyBackgroundImage",value);
                  }
                  else if (line.startsWith("::bodyBackground") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("bodyBackground",value);
                  }
                  else if (line.startsWith("::userMessageBoxBackground") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("userMessageBoxBackground",value);
                  }
                  else if (line.startsWith("::userMessageTextColor") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("userMessageTextColor",value);
                  }
                  else if (line.startsWith("::botMessageBoxBackground") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("botMessageBoxBackground",value);
                  }
                  else if (line.startsWith("::botMessageTextColor") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("botMessageTextColor",value);
                  }
                  else if (line.startsWith("::botIconColor") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("botIconColor",value);
                  }
                  else if (line.startsWith("::botIconImage") && (thenpos = line.indexOf(' ')) > 0) {
                      String value = line.substring(thenpos + 1).trim();
                      if(value.length() > 0)
                          designObject.put("botIconImage",value);
                  }
                  else if (line.startsWith("::allow_bot_only_on_own_sites") && (thenpos = line.indexOf(' ')) > 0) {
                    Boolean value = false;
                    if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                    configObject.put("allow_bot_only_on_own_sites",value);
                }
                else if (line.startsWith("::allowed_sites") && (thenpos = line.indexOf(' ')) > 0) {
                    String value = line.substring(thenpos + 1).trim();
                    if(value.length() > 0)
                        configObject.put("allowed_sites",value);
                }
                else if (line.startsWith("::enable_default_skills") && (thenpos = line.indexOf(' ')) > 0) {
                    Boolean value = true;
                    if(line.substring(thenpos + 1).trim().equalsIgnoreCase("no")) value = false;
                    configObject.put("enable_default_skills",value);
                }
                else if (line.startsWith("::enable_bot_in_my_devices") && (thenpos = line.indexOf(' ')) > 0) {
                    Boolean value = false;
                    if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                    configObject.put("enable_bot_in_my_devices",value);
                }
                else if (line.startsWith("::enable_bot_for_other_users") && (thenpos = line.indexOf(' ')) > 0) {
                    Boolean value = false;
                    if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                    configObject.put("enable_bot_for_other_users",value);
                }
                }
              }
          } catch (IOException e) {
            DAO.log(e.getMessage());
          }

        // save a new bot
        JSONObject botObject = new JSONObject();
        botObject.put("design",designObject);
        botObject.put("configure",configObject);
        botObject.put("timestamp", timestamp.toString());
        languageName.put(skill_name, botObject);
        groupName.put(language_name, languageName);
        userName.put(group_name, groupName);
        chatbot.put(userId, userName, true);
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        return new ServiceResponse("");
    }
}
