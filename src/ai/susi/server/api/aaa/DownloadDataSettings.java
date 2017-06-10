package ai.susi.server.api.aaa;

import ai.susi.DAO;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by dravit on 10/6/17.
 */
public class DownloadDataSettings extends HttpServlet{

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

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

            byte[] zip = zipFiles(settings, files);


            ServletOutputStream sos = response.getOutputStream();
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=settings.zip");

            sos.write(zip);
            sos.flush();
        }
        catch (Exception e){

        }

    }

    private byte[] zipFiles(File settingsDir, String[] files) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        byte bytes[] = new byte[2048];

        for (String fileName : files){
            FileInputStream fis = new FileInputStream(settingsDir.getPath() +
                    DownloadDataSettings.FILE_SEPARATOR + fileName);
            BufferedInputStream bis = new BufferedInputStream(fis);

            zos.putNextEntry(new ZipEntry(fileName));

            int bytesRead;
            while ((bytesRead = bis.read(bytes)) != -1) {
                zos.write(bytes, 0, bytesRead);
            }
            zos.closeEntry();
            bis.close();
            fis.close();
        }
        zos.flush();
        baos.flush();
        zos.close();
        baos.close();

        return baos.toByteArray();
    }
}
