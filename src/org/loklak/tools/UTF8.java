/**
 *  UTF8
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

package org.loklak.tools;

import java.nio.charset.Charset;

public class UTF8 {

    public final static Charset charset;
    static {
        charset = Charset.forName("UTF-8");
    }

    public boolean insensitive;

    /**
     * using the string method with the default charset given as argument should prevent using the charset cache
     * in FastCharsetProvider.java:118 which locks all concurrent threads using a UTF8.String() method
     * @param bytes
     * @return
     */
    public final static String String(final byte[] bytes) {
        return bytes == null ? "" : new String(bytes, 0, bytes.length, charset);
    }

    public final static String String(final byte[] bytes, final int offset, final int length) {
        assert bytes != null;
        return new String(bytes, offset, length, charset);
    }

    /**
     * getBytes() as method for String synchronizes during the look-up for the
     * Charset object for the default charset as given with a default charset name.
     * With our call using a given charset object, the call is much easier to perform
     * and it omits the synchronization for the charset lookup.
     *
     * @param s
     * @return
     */
    public final static byte[] getBytes(final String s) {
        if (s == null) return null;
        return s.getBytes(charset);
    }

    public final static byte[] getBytes(final StringBuilder s) {
        if (s == null) return null;
        return s.toString().getBytes(charset);
    }

}
