package org.loklak.api.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Date;

/**
 *  DumpDownloadServlet
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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





import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.DAO;
import org.loklak.tools.UTF8;

public class DumpDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 2839106194602799989L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        long now = System.currentTimeMillis();
        
        if (path.length() <= 1) {
            // send directory as html

            response.setDateHeader("Last-Modified", now);
            response.setDateHeader("Expires", now + 10000);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), UTF8.charset);
            BufferedWriter writer = new BufferedWriter(osw);
            writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n");
            writer.write("<html>\n");
            writer.write(" <head>\n");
            writer.write("  <title>Index of /dump</title>\n");
            writer.write(" </head>\n");
            writer.write(" <body>\n");
            writer.write("<h1>Index of /dump</h1>\n");
            writer.write("<pre>      Name \n");
            for (File dump: DAO.getTweetOwnDumps()) {
                String name = dump.getName();
                String space = "";
                for (int i = name.length(); i < 36; i++) space += " ";
                long length = dump.length();
                String size = length < 1024 ? Long.toString(length) : length < 1024 * 1024 ? Long.toString(length / 1024) + "K" : Long.toString(length / 1024 / 1024) + "M";
                while (size.length() < 5) size = " " + size;
                int d = name.lastIndexOf('.');
                if (d < 0) writer.write("[   ]"); else {
                    String ext = name.substring(d + 1);
                    if (ext.length() > 3) ext = ext.substring(0, 3);
                    writer.write('[');
                    writer.write(ext);
                    for (int i = 0; i < 3 - ext.length(); i++) writer.write(' ');
                    writer.write(']');
                }
                writer.write(" <a href=\"" + name + "\">"+ name +"</a>" + space + new Date(dump.lastModified()).toString() + "  " + size + "\n");
            }
            writer.write("<hr></pre>\n");
            writer.write("<address>this is the message dump download directory</address>\n");
            writer.write("<address>- import these dumps by placing them into your data/dump/import/ directory</address>\n");
            writer.write("<address>- imported dumps will be moved to data/dump/imported/</address>\n");
            writer.write("</body></html>\n");
            writer.flush();
            DAO.log(path);
            return;
        }

        // download a dump file
        if (path.startsWith("/")) path = path.substring(1);
        Collection<File> ownDumps = DAO.getTweetOwnDumps();
        File dump = ownDumps.size() == 0 ? null : new File(ownDumps.iterator().next().getParentFile(), path);
        if (dump == null || !dump.exists()) {
            response.sendError(404, request.getContextPath() + " not available");
        }

        response.setDateHeader("Last-Modified", dump.lastModified());
        response.setDateHeader("Expires", now + 600000);
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        FileInputStream fis = new FileInputStream(dump);
        byte[] buffer = new byte[1024];
        int c;
        while ((c = fis.read(buffer)) > 0) response.getOutputStream().write(buffer, 0, c);
        fis.close();
        
        DAO.log(path);
        return;
    }
}