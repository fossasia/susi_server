/**
 *  Compression
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

package org.loklak.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {

    public static byte[] gzip(byte[] b) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream out = new GZIPOutputStream(baos, 65536){{def.setLevel(Deflater.BEST_COMPRESSION);}};
            out.write(b, 0, b.length);
            out.finish();
            out.close();
            return baos.toByteArray();
        } catch (IOException e) {}
        return null;
    }
    
    public static byte[] gunzip(byte[] b) {
        byte[] buffer = new byte[Math.min(2^20, b.length)];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(b), 65536);
            int l; while ((l = in.read(buffer)) > 0) baos.write(buffer, 0, l);
            in.close();
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {}
        return null;
    }
    
}
