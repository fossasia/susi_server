package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by dravit on 12/6/17.
 * Servlet to upload file to data/settings folder
 */
public class UploadDataSettings extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/data/settings/upload";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject();
        result.put("success","true");
        File data = DAO.data_dir;
        String filePath = data.getPath() + "/settings/";
        String fileName = (String) post.getRequest().getParameter("file");
        if (fileName == null || fileName.equals(""))
            try {
                throw new ServletException("Invalid or non-existent file parameter in SendWord servlet.");
            } catch (ServletException e) {
                e.printStackTrace();
            }
        // add the .doc suffix if it doesn't already exist
        if (fileName.indexOf(".json") == -1)
            fileName = fileName + ".json";

        String wordDir = getServletContext().getInitParameter("word-dir");
        if (wordDir == null || wordDir.equals(""))
            try {
                throw new ServletException("Invalid or non-existent wordDir context-param.");
            } catch (ServletException e) {
                result.put("success","false");
                e.printStackTrace();
            }
        ServletOutputStream stream = null;
        BufferedInputStream buf = null;
        try {
            stream = response.getOutputStream();
            File file = new File(filePath +  fileName);
            response.setContentType("application/json");
            response.addHeader("Content-Disposition", "attachment; filename="
                    + fileName);
            response.setContentLength((int) file.length());
            FileInputStream input = new FileInputStream(file);
            buf = new BufferedInputStream(input);
            int readBytes = 0;
            while ((readBytes = buf.read()) != -1)
                stream.write(readBytes);
        } catch (IOException ioe) {
            result.put("success","false");
            try {
                throw new ServletException(ioe.getMessage());
            } catch (ServletException e) {
                e.printStackTrace();
            }
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    result.put("success","false");
                    e.printStackTrace();
                }
            if (buf != null)
                try {
                    buf.close();
                } catch (IOException e) {
                    result.put("success","false");
                    e.printStackTrace();
                }
        }
        return new ServiceResponse(result);
    }
}
