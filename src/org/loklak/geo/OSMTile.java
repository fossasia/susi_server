/**
 *  OSMTile
 *  Copyright 2008 by Michael Peter Christen
 *  First released 12.02.2008 at http://yacy.net
 *
 *  $LastChangedDate$
 *  $LastChangedRevision$
 *  $LastChangedBy$
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

package org.loklak.geo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.loklak.visualization.graphics.RasterPlotter;


public class OSMTile {

    // helper methods to load map images from openstreetmap.org

    /**
     * generate a image according to a given coordinate of a middle tile
     * and a width and height of tile numbers. The tile number width and height must
     * always be impair since the given tile must be always in the middle
     * @param t the middle tile
     * @param width number of tiles
     * @param height number of tiles
     * @return the image
     */
    public static RasterPlotter getCombinedTiles(final TileCoordinates t, int width, int height) {
        final int w = (width - 1) / 2;
        width = w * 2 + 1;
        final int h = (height - 1) / 2;
        height = h * 2 + 1;
        final RasterPlotter m = new RasterPlotter(256 * width, 256 * height, RasterPlotter.DrawMode.MODE_REPLACE, "FFFFFF");
        final List<Place> tileLoader = new ArrayList<Place>();
        Place place;
        // start tile loading concurrently
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                place = new Place(m, t.xtile - w + i, t.ytile - h + j, 256 * i, 256 * j, t.zoom);
                place.start();
                tileLoader.add(place);
                if (t.zoom >= 17) try {Thread.sleep(100);} catch (final InterruptedException e) {} // be nice with tile server for uncached tiles
            }
        }
        // wait until all tiles are loaded
        for (final Place p: tileLoader) try { p.join(); } catch (final InterruptedException e) {}
        return m;
    }

    static class Place extends Thread {
        RasterPlotter m;
        int xt, yt, xc, yc, z;
        public Place(final RasterPlotter m, final int xt, final int yt, final int xc, final int yc, final int z) {
            this.m = m; this.xt = xt; this.yt = yt; this.xc = xc; this.yc = yc; this.z = z;
        }
        @Override
        public void run() {
            final TileCoordinates t = new TileCoordinates(this.xt, this.yt, this.z);
            BufferedImage bi = null;
            for (int i = 0; i < 5; i++) {
                bi = getSingleTile(t, i);
                if (bi != null) {
                    this.m.insertBitmap(bi, this.xc, this.yc);
                    return;
                }
                // don't DoS OSM when trying again
                try {Thread.sleep(300 + 100 * i);} catch (final InterruptedException e) {}
            }
        }
    }

    public static BufferedImage getSingleTile(final TileCoordinates tile, final int retry) {
        URL tileURL;
        try {
            tileURL = new URL(tile.url(retry));
        } catch (MalformedURLException e1) {
            return null;
        }
        
        // download resource using the crawler and keep resource in memory if possible
        InputStream is;
        try {
            is = tileURL.openStream();
        } catch (IOException e1) {
            return null;
        }
        byte[] buffer = new byte[2048];
        int c;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while ((c = is.read(buffer)) > 0) baos.write(buffer, 0, c);
        } catch (IOException e) {}
        byte[] tileb = baos.toByteArray();
        
        try {
            ImageIO.setUseCache(false); // do not write a cache to disc; keep in RAM
            return ImageIO.read(new ByteArrayInputStream(tileb));
        } catch (final EOFException e) {
            return null;
        } catch (final IOException e) {
            return null;
        }
    }

    public static class TileCoordinates {

        public int xtile, ytile, zoom, n;
        public double north_lat, south_lat, east_lon, west_lon;   
          
        public TileCoordinates(final double lat_deg, final double lon_deg, final int zoom) {
            // see http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
            // X goes from 0 (left edge is 180 °W) to 2^zoom − 1 (right edge is 180 °E)
            // Y goes from 0 (top edge is 85.0511 °N) to 2^zoom − 1 (bottom edge is 85.0511 °S) in a Mercator projection
            // the number 85.0511 is the result of arctan(sinh(π)). By using this bound, the entire map becomes a (very large) square.

            // Lon./lat. to tile numbers
            // n = 2 ^ zoom
            // xtile = n * ((lon_deg + 180) / 360)
            // ytile = n * (1 - (log(tan(lat_rad) + sec(lat_rad)) / π)) / 2

            // Tile numbers to lon./lat.
            // n = 2 ^ zoom
            // lon_deg = xtile / n * 360.0 - 180.0
            // lat_rad = arctan(sinh(π * (1 - 2 * ytile / n)))
            // lat_deg = lat_rad * 180.0 / π
            
            this.zoom = zoom;
            this.n = 1 << zoom;
            this.xtile = (int) Math.floor((lon_deg + 180) / 360 * this.n);
            double lat_rad = lat_deg * RasterPlotter.PI180;
            this.ytile = (int) Math.floor((1 - Math.log(Math.tan(lat_rad) + 1 / Math.cos(lat_rad)) / Math.PI) / 2 * this.n);
            tile2boundingBox();
        }
        
        public TileCoordinates(final int xtile, final int ytile, final int zoom) {
            this.zoom = zoom;
            this.xtile = xtile;
            this.ytile = ytile;
            tile2boundingBox();
        }
        
        private void tile2boundingBox() {
            this.north_lat = tile2lat(this.ytile);
            this.south_lat = tile2lat(this.ytile + 1);
            this.west_lon = tile2lon(this.xtile);
            this.east_lon = tile2lon(this.xtile + 1);
        }
         
        private double tile2lon(int x) {
            return x * 360.0d / this.n - 180.0d;
        }
         
        private double tile2lat(int y) {
            //return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y) / this.n)));
            return Math.toDegrees(Math.atan(Math.sinh(Math.PI - 2.0 * Math.PI * y / this.n)));
        }

        public String url(final int retry) {
            // see http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
            final int hash = (this.xtile + 7 * this.ytile + 13 * this.zoom + retry) % 4;
            final String host = (hash == 3) ? "tile.openstreetmap.org" : ((char) ('a' + hash)) + ".tile.openstreetmap.org";
            final String url = "http://" + host + "/" + this.zoom + "/" + this.xtile + "/" + this.ytile + ".png";
            //System.out.println("OSM URL = " + url);
            return url;
        }

    }
}
