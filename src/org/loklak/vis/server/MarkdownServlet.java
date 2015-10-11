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
    // http://localhost:9000/vis/markdown.png?text=line+one%0Aline+two%0Aline+three%0A%C2%A0%0Avery+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line+very+long+line
    // http://localhost:9000/vis/markdown.png?text=%23+This+is+a+Test-Text+for+large+attachment+texts.%0A%C2%A0%0A%23+The+largest+heading+%28an+%3Ch1%3E+tag%29%0A%23%23+The+second+largest+heading+%28an+%3Ch2%3E+tag%29%0A%E2%80%A6%0A%23%23%23%23%23%23+The+6th+largest+heading+%28an+%3Ch6%3E+tag%29%0A%C2%A0%0AIn+the+words+of+Abraham+Lincoln%3A%0A%C2%A0%0A%3E+Pardon+my+french%0A%C2%A0%0A*This+text+will+be+italic*%0A**This+text+will+be+bold**%0A%C2%A0%0A*+Item%0A*+Item%0A*+Item%0A%C2%A0%0A1.+Item+1%0A2.+Item+2%0A3.+Item+3%0A%C2%A0%0AHere%27s+an+idea%3A+why+don%27t+we+take+%60SuperiorProject%60+and+turn+it+into+%60**Reasonable**Project%60.%0A%C2%A0%0ACheck+out+this+neat+program+I+wrote%3A%0A%C2%A0%0A%60%60%60%0Ax+%3D+0%0Ax+%3D+2+%2B+2%0Awhat+is+x%0A%60%60%60%0A%C2%A0%0AIf+you+like+this%2C+check+the+loklak+project+at%0A++http%3A%2F%2Floklak.org+%28for+the+server%29+and%0A++http%3A%2F%2Floklak.net+%28for+the+webclient%29%0A++%0ATo+get+latest+news%2C+check+%400rb1t3r
    
    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {
        // parse arguments
        String text = post.get("text", "");
        DAO.log("MARKDOWN-TEXT: " + URLEncoder.encode(text, "UTF-8"));
        int padding = post.get("padding", 3);
        boolean uppercase = post.get("uppercase", true);
        long color_background = Long.parseLong(post.get("color_background", "000000"), 16);
        DrawMode drawmode = color_background > 0x888888 ? DrawMode.MODE_SUB : DrawMode.MODE_ADD;
        long color_text = Long.parseLong(post.get("color_text", drawmode == DrawMode.MODE_SUB ? "000000" : "ffffff"), 16);
        long color_code = Long.parseLong(post.get("color_code", drawmode == DrawMode.MODE_SUB ? "66ffaa" : "00ff00"), 16);
        //long color_bold = Long.parseLong(post.get("color_bold", drawmode == DrawMode.MODE_SUB ? "ff66aa" : "ff0000"), 16);
        long color_headline = Long.parseLong(post.get("color_headline", drawmode == DrawMode.MODE_SUB ? "44aaee" : "00aaff"), 16);
        if (drawmode == DrawMode.MODE_SUB) color_text = RasterPlotter.invertColor(color_text);
        FileTypeEncoding fileType = RemoteAccess.getFileType(request);
        
        // read and measure the text
        BufferedReader rdr = new BufferedReader(new StringReader(text));
        StringBuilder sb = new StringBuilder();
        int width = 0;
        int linecount = 0;
        for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
            String[] sublines = line.split("\n");
            for (String subline: sublines) {
                //DAO.log("MARKDOWN-LINE: " + subline);
                width = Math.max(width, subline.length());
                linecount += subline.length() / 80 + 1;
                sb.append(subline).append('\n');
            }
        }
        rdr.close();
        int charwidth = 6;
        width = charwidth * 80 + 2 * padding - 1;
        
        // compute optimum image size
        int lineheight = 7;
        int yoffset = 0;
        int height = width / 2;
        while (lineheight <= 12) {
            height = linecount * lineheight + 2 * padding - 1;
            // a perfect size has the format 2:1, that fits into the preview window.
            // We should not allow that the left or right border is cut away; resize if necessary
            if (width <= 2 * height) break;
            // add height, the full height is width / 2
            yoffset = (width / 2 - height) / 2;
            height = width / 2; 
            lineheight++;
        }
        
        // print to the image
        RasterPlotter matrix = new RasterPlotter(width, height, drawmode, color_background);
        matrix.setColor(color_text);
        int x = padding - 1;
        int y = yoffset + padding + 4;
        int column = 0;
        int hashcount = 0;
        int default_intensity = 100;
        boolean isInlineFormatted = false, isFormatted = false, isBold = false, isItalic = false;
        for (int pos = 0; pos < sb.length(); pos++) {
            char c = sb.charAt(pos);
            int nextspace = sb.indexOf(" ", pos + 1);
            if (c == '\n' || (c == ' ' && column + nextspace - pos >= 80)) {
                x = padding - 1;
                y += lineheight;
                column = 0;
                hashcount = 0;
                if (!isFormatted) {
                    matrix.setColor(color_text);
                }
                isBold = false;
                isItalic = false;
                continue;
            }
            if (!isFormatted && !isInlineFormatted && column == 0 && c == '#') {
                // count the hashes at the beginning of the line
                hashcount = Math.min(6, hashcount + 1); // there may be at most 6
                matrix.setColor(color_headline);
                continue;
            }
            if (!isFormatted && column == 0 && c == ' ' && hashcount > 0) {
                // ignore first space after hash
                continue;
            }
            if (!isFormatted && !isInlineFormatted && c == '*' && pos > 0 && sb.charAt(pos - 1) == '*' && pos < sb.length() - 1 && sb.charAt(pos + 1) != ' ') {
                isBold = true;
                continue;
            }
            if (!isFormatted && !isInlineFormatted && c == '*' && pos > 0 && sb.charAt(pos + 1) == '*' && pos > 0 && sb.charAt(pos - 1) != ' ') {
                isBold = false;
                continue;
            }
            if (!isFormatted && !isInlineFormatted && c == '*' && pos < sb.length() - 1 && sb.charAt(pos + 1) != ' ') {
                isItalic = true;
                continue;
            }
            if (!isFormatted && !isInlineFormatted && c == '*' && pos > 0 && sb.charAt(pos - 1) != ' ') {
                isItalic = false;
                continue;
            }
            if (!isFormatted && c == '`' && pos < sb.length() - 2 && sb.charAt(pos + 1) == '`' && sb.charAt(pos + 2) == '`') {
                matrix.setColor(color_code);
                isFormatted = true;
                continue;
            }
            if (isFormatted && c == '`' && pos < sb.length() - 2 && sb.charAt(pos + 1) == '`' && sb.charAt(pos + 2) == '`') {
                matrix.setColor(color_text);
                isFormatted = false;
                continue;
            }
            if (!isFormatted && c == '`' && pos < sb.length() - 1 && sb.charAt(pos + 1) != ' ') {
                matrix.setColor(color_code);
                isInlineFormatted = true;
                continue;
            }
            if (!isFormatted && c == '`' && pos > 0 && sb.charAt(pos - 1) != ' ') {
                matrix.setColor(color_text);
                isInlineFormatted = false;
                continue;
            }
            if (c == '`' || (!isFormatted && !isInlineFormatted && c == '*')) {
                continue;
            }

            PrintTool.print(matrix, x, y, 0, uppercase || hashcount > 0 ? Character.toUpperCase(c) : c, isItalic, default_intensity);
            if (isBold) PrintTool.print(matrix, x+1, y, 0, uppercase || hashcount > 0 ? Character.toUpperCase(c) : c, isItalic, default_intensity);
            x += 6;
            column++;
        }
        
        // write branding
        PrintTool.print(matrix, matrix.getWidth() - 6, matrix.getHeight() - 6, 0, "MADE WITH HTTP://LOKLAK.NET", 1, false, 50);
        
        // write image
        response.addHeader("Access-Control-Allow-Origin", "*");
        RemoteAccess.writeImage(fileType, response, post, matrix);
        post.finalize();
    }
}
