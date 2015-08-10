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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.api.server.RemoteAccess;
import org.loklak.api.server.RemoteAccess.FileTypeEncoding;
import org.loklak.data.DAO;
import org.loklak.visualization.graphics.PrintTool;
import org.loklak.visualization.graphics.RasterPlotter;
import org.loklak.visualization.graphics.RasterPlotter.DrawMode;

public class MarkdownServlet extends HttpServlet {

    private static final long serialVersionUID = -9112326727290824443L;

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
    
    // http://localhost:9000/vis/markdown.png?text=hello%20world%0Dhello%20universe&color_text=000000&color_background=ffffff&padding=3
    // http://localhost:9000/vis/markdown.png?text=loklak%20has%20now%20an%20amazing%20new%20feature!%0D%0Dthe%20server%20is%20able%20to%20render%20large%20amounts%20of%20text%20lines%0Dinto%20a%20single%20image!!!%0D%0Dsuch%20an%20image%20can%20then%20be%20attached%20to%20a%20tweet%20as%20image%0Dand%20therefore%20is%20able%20to%20transport%20much%20more%0Dthan%20the%20limit%20of%20140%20characters!%0D%0Dif%20you%20want%20to%20see%20what%20loklak%20is,%20check%20out:%0D%0Dloklak.org&color_text=000000&color_background=ffffff&padding=3

    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {
        // parse arguments
        String text = post.get("text", "");
        DAO.log("MARKDOWN-TEXT: " + URLEncoder.encode(text, "UTF-8"));
        int padding = post.get("padding", 3);
        boolean uppercase = post.get("uppercase", true);
        long color_text = Long.parseLong(post.get("color_text", "ffffff"), 16);
        long color_background = Long.parseLong(post.get("color_background", "000000"), 16);
        String drawmodes = post.get("drawmode", color_background > 0x888888 ? DrawMode.MODE_SUB.name() : DrawMode.MODE_ADD.name());
        DrawMode drawmode = DrawMode.valueOf(drawmodes);
        if (drawmode == DrawMode.MODE_SUB) color_text = RasterPlotter.invertColor(color_text);
        FileTypeEncoding fileType = RemoteAccess.getFileType(request);
        
        // compute image
        BufferedReader rdr = new BufferedReader(new StringReader(text));
        List<String> lines = new ArrayList<String>();
        for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
            String[] sublines = line.split("\n");
            for (String subline: sublines) {
                DAO.log("MARKDOWN-LINE: " + subline);
                lines.add(subline);
            }
        }
        rdr.close();
        int charwidth = 6;
        int width = 0; for (String line: lines) width = Math.max(width, line.length());
        width = charwidth * width + 2 * padding - 1;
        
        int lineheight = 7;
        int yoffset = 0;
        int height = width / 2;
        while (lineheight <= 12) {
            height = lines.size() * lineheight + 2 * padding - 1;
            // a perfect size has the format 2:1, that fits into the preview window.
            // We should not allow that the left or right border is cut away; resize if necessary
            if (width <= 2 * height) break;
            // add height, the full height is width / 2
            yoffset = (width / 2 - height) / 2;
            height = width / 2; 
            lineheight++;
        }        
        
        RasterPlotter matrix = new RasterPlotter(width, height, drawmode, color_background);
        matrix.setColor(color_text);
        for (int line = 0; line < lines.size(); line++) {
            String l = lines.get(line);
            PrintTool.print(matrix, padding - 1, yoffset + padding + 4 + line * lineheight, 0, uppercase ? l.toUpperCase() : l, -1, 100);
        }
        
        // write branding
        PrintTool.print(matrix, matrix.getWidth() - 6, matrix.getHeight() - 6, 0, "MADE WITH HTTP://LOKLAK.ORG", 1, 20);
        
        // write image
        response.addHeader("Access-Control-Allow-Origin", "*");
        RemoteAccess.writeImage(fileType, response, post, matrix);
    }
}
