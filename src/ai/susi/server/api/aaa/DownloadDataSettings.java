package ai.susi.server.api.aaa;

import ai.susi.DAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by dravit on 10/6/17.
 */
public class DownloadDataSettings extends HttpServlet{

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

        try{
            File settings = new File(DAO.data_dir.getPath()+"/settings");
            String path = settings.getPath();
            System.out.println(path);
            String[] files = settings.list();
            System.out.println(files.length);
            
        }
        catch (Exception e){

        }

    }
}
