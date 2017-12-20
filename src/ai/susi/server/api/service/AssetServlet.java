/**
 *  AssetServlet
 *  Copyright 12.06.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.service;

import ai.susi.DAO;
import ai.susi.server.Query;
import ai.susi.server.RemoteAccess;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AssetServlet extends HttpServlet {

    private static final long serialVersionUID = -9112326722297824443L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        
        // parse arguments
        String screen_name = post.get("screen_name", "");
        if (screen_name.length() == 0) {response.sendError(503, "a screen_name must be submitted"); return;}
        String id_str = post.get("id_str", "");
        if (id_str.length() == 0) {response.sendError(503, "an id_str must be submitted"); return;}
        String file = post.get("file", "");
        File assetFile = DAO.getAssetFile(screen_name, id_str, file);
        if (!assetFile.exists()) {response.sendError(503, "asset does not exist"); return;}
        
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] b = new byte[2048];
        InputStream is = new BufferedInputStream(new FileInputStream(assetFile));
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
    }
    
    // http://localhost:9000/aaa/asset?screen_name=loklak_app&id_str=123&file=image.jpg&data=
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
                
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        Map<String, byte[]> m = RemoteAccess.getPostMap(request);
        byte[] data = m.get("data");
        if (data == null || data.length == 0) {response.sendError(400, "your request does not contain a data object."); return;}
        byte[] screen_name_b = m.get("screen_name");
        if (screen_name_b == null || screen_name_b.length == 0) {response.sendError(400, "your request does not contain a screen_name."); return;}
        byte[] id_str_b = m.get("id_str");
        if (id_str_b == null || id_str_b.length == 0) {response.sendError(400, "your request does not contain a id_str."); return;}
        byte[] file_b = m.get("file");
        if (file_b == null || file_b.length == 0) {response.sendError(400, "your request does not contain a file name."); return;}
        String screen_name = screen_name_b == null ? "" : new String(screen_name_b, 0, screen_name_b.length, StandardCharsets.UTF_8);
        final byte[] bytes = id_str_b;
        String id_str = bytes == null ? "" : new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        final byte[] bytes1 = file_b;
        String file = bytes1 == null ? "" : new String(bytes1, 0, bytes1.length, StandardCharsets.UTF_8);

        File f = DAO.getAssetFile(screen_name, id_str, file);
        f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.close();
        
        post.setResponse(response, "application/octet-stream");
        post.finalize();
    }
    
    
}
