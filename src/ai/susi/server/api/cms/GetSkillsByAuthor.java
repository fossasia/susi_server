package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


/**
 * Created by chetankaushik on 12/08/17.
 * API Endpoint to get all skills by an author.
 * http://127.0.0.1:4000/cms/getSkillsByAuthor.json?author=chetan%20kaushik
 */
public class GetSkillsByAuthor extends AbstractAPIHandler implements APIHandler {

    JSONObject result ;
    private static final long serialVersionUID = 3446536703362688060L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillsByAuthor.json";
    }

    public void listFilesForFolder(String author,final File folder) {
        String Author = author;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(Author, fileEntry);
            } else {
                String s  = fileEntry.getPath();
                if(s.contains(".txt")) {
                    Scanner scanner = null;
                    try {
                        scanner = new Scanner(fileEntry);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    assert scanner != null;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().toLowerCase();
                        if (line.contains(Author.toLowerCase())){
                            System.out.println(fileEntry.getPath());
                            result.put(result.length()+"",fileEntry.getPath());
                        }
                    }
                }
            }
        }
    }
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        result= new JSONObject();
        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }

        String author = call.get("author",null);

        if(author == null){
            JSONObject json =  new JSONObject();
            json.put("accepted",false);
            json.put("message","Author name not given");
            return new ServiceResponse(json);
        }
        final File folder = new File(DAO.model_watch_dir.getPath());
        listFilesForFolder(author.toLowerCase(),folder);
        return new ServiceResponse(result);

    }

}
