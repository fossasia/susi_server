/**
 *  MarkdownServlet
 *  Copyright 04.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.vis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.api.server.RemoteAccess;
import org.loklak.visualization.graphics.PrintTool;
import org.loklak.visualization.graphics.RasterPlotter;
import org.loklak.visualization.graphics.RasterPlotter.DrawMode;

public class MarkdownServlet extends HttpServlet {

    private static final long serialVersionUID = -9112326721290824443L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        process(request, response, post);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        post.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, post);
    }
    
    // http://localhost:9100/vis/markdown.png?text=hello%20world%0Dhello%20universe&color_text=000000&color_background=ffffff&padding=3

    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {
        // parse arguments
        String text = post.get("text", "");
        int padding = post.get("padding", 3);
        boolean uppercase = post.get("uppercase", true);
        long color_text = Long.parseLong(post.get("color_text", "ffffff"), 16);
        long color_background = Long.parseLong(post.get("color_background", "000000"), 16);
        String drawmodes = post.get("drawmode", color_background > 0x888888 ? DrawMode.MODE_SUB.name() : DrawMode.MODE_ADD.name());
        DrawMode drawmode = DrawMode.valueOf(drawmodes);
        if (drawmode == DrawMode.MODE_SUB) color_text = RasterPlotter.invertColor(color_text);
        boolean gifExt = request.getServletPath().endsWith(".gif");
        boolean pngExt = request.getServletPath().endsWith(".png");
        boolean jpgExt = request.getServletPath().endsWith(".jpg");
        
        // compute image
        BufferedReader rdr = new BufferedReader(new StringReader(text));
        List<String> lines = new ArrayList<String>();
        for (String line = rdr.readLine(); line != null; line = rdr.readLine()) lines.add(line);
        rdr.close();
        int width = 0; for (String line: lines) width = Math.max(width, line.length());
        width = 6 * width + 2 * padding - 1;
        int height = lines.size() * 6 + 2 * padding - 1;
        
        RasterPlotter matrix = new RasterPlotter(width, height, drawmode, color_background);
        matrix.setColor(color_text);
        for (int line = 0; line < lines.size(); line++) {
            String l = lines.get(line);
            PrintTool.print(matrix, padding - 1, padding + 4 + line * 6, 0, uppercase ? l.toUpperCase() : l, -1, 100);
        }
        
        // write image
        ServletOutputStream sos = response.getOutputStream();
        if (pngExt) {
            post.setResponse(response, "image/png");
            sos.write(matrix.pngEncode(1));
        }
        if (gifExt) {
            post.setResponse(response, "image/gif");
            ImageIO.write(matrix.getImage(), "gif", sos);
        }
        if (jpgExt) {
            post.setResponse(response, "image/jpeg");
            ImageIO.write(matrix.getImage(), "jpg", sos);
        }
    }
}
