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

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.api.server.RemoteAccess;
import org.loklak.api.server.RemoteAccess.FileTypeEncoding;
import org.loklak.geo.OSMTile;
import org.loklak.visualization.graphics.PrintTool;
import org.loklak.visualization.graphics.RasterPlotter;
import org.loklak.visualization.graphics.RasterPlotter.DrawMode;

public class MapServlet extends HttpServlet {

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
    
    // http://localhost:9000/vis/map.png?text=Test&mlat=1.28373&mlon=103.84379&zoom=18

    protected void process(HttpServletRequest request, HttpServletResponse response, RemoteAccess.Post post) throws ServletException, IOException {
        // parse arguments
        String text = post.get("text", "");
        boolean uppercase = post.get("uppercase", true);
        FileTypeEncoding fileType = RemoteAccess.getFileType(request);
        
        int zoom = post.get("zoom", 19);
        double lat = post.get("mlat", 50.11362d);
        double lon = post.get("mlon", 8.67919d);
        int width = post.get("width", 512);
        int height = post.get("height", 256);
        int tiles_horizontal = (width - 1) / 256 + 1; if (((tiles_horizontal / 2 ) * 2) == tiles_horizontal) tiles_horizontal++;
        int tiles_vertical = (height - 1) / 256 + 1; if (((tiles_vertical / 2 ) * 2) == tiles_vertical) tiles_vertical++;
        // one tile has the size 256x256

        // compute image
        final OSMTile.tileCoordinates coord = new OSMTile.tileCoordinates(lat, lon, zoom);
        RasterPlotter map = OSMTile.getCombinedTiles(coord, tiles_horizontal, tiles_vertical);
        // cut away parts of the map if less was wanted
        
        if (map.getHeight() > height || map.getWidth() > width) {
            BufferedImage bi = map.getImage();
            int xoff = (map.getWidth() - width) / 2;
            int yoff = (map.getHeight() - height) / 2;
            bi = bi.getSubimage(xoff, yoff, width, height);
            map = new RasterPlotter(width, height, RasterPlotter.DrawMode.MODE_REPLACE, "FFFFFF");
            map.insertBitmap(bi, 0, 0);
        }
        
        map.setDrawMode(DrawMode.MODE_SUB);
        map.setColor(0xffffff);
        if (text.length() > 0) PrintTool.print(map, 6, 12, 0, uppercase ? text.toUpperCase() : text, -1, 100);
        PrintTool.print(map, map.getWidth() - 6, map.getHeight() - 6, 0, "MADE WITH LOKLAK.ORG", 1, 50);
        /*
         * copyright notice on OSM Tiles
         * According to http://www.openstreetmap.org/copyright/ the (C) of the map tiles is (CC BY-SA)
         * while the OpenStreetMap raw data is licensed with (ODbL) http://opendatacommons.org/licenses/odbl/ 
         * Map tiles shall be underlined with the statement "(C) OpenStreetMap contributors". In our 5-dot character
         * set the lowercase letters do not look good, so we use uppercase only.
         * The (C) symbol is not available in our font, so we use the letters (C) instead.
         */
        PrintTool.print(map, 6, map.getHeight() - 6, 0, "(C) OPENSTREETMAP CONTRIBUTORS", -1, 100);
        
        // write image
        RemoteAccess.writeImage(fileType, response, post, map);
    }
}
