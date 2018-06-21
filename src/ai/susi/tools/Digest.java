/**
 *  Digest
 *  (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 *  first published 28.12.2008 on http://yacy.net
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

package ai.susi.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Digest {

	public static Queue<MessageDigest> digestPool = new ConcurrentLinkedQueue<MessageDigest>();
    private static Map<String, byte[]> md5Cache = new ConcurrentHashMap<>();

    /**
     * clean the md5 cache
     */
    public static void cleanup() {
    	md5Cache.clear();
    }

    public static String encodeMD5Hex(final String key) {
        // generate a hex representation from the md5 of a string
        return encodeHex(encodeMD5Raw(key));
    }

    public static String encodeMD5Hex(final byte[] b) {
        // generate a hex representation from the md5 of a byte-array
        return encodeHex(encodeMD5Raw(b));
    }

    private static String encodeHex(final byte[] in) {
        if (in == null) return "";
        final StringBuilder result = new StringBuilder(in.length * 2);
        for (final byte element : in) {
            if ((0Xff & element) < 16) result.append('0');
            result.append(Integer.toHexString(0Xff & element));
        }
        return result.toString();
    }

    private static byte[] encodeMD5Raw(final String key) {

        byte[] h = md5Cache.get(key);
        if (h != null) return h;

    	MessageDigest digest = digestPool.poll();
    	if (digest == null) {
    	    // if there are no digest objects left, create some on the fly
    	    // this is not the most effective way but if we wouldn't do that the encoder would block
    	    try {
                digest = MessageDigest.getInstance("MD5");
                digest.reset();
            } catch (final NoSuchAlgorithmException e) {
            }
    	} else {
    	    digest.reset(); // they should all be reseted but anyway; this is safe
    	}
        byte[] keyBytes;
        keyBytes = key.getBytes(StandardCharsets.UTF_8);
        digest.update(keyBytes);
        final byte[] result = digest.digest();
        digest.reset(); // to be prepared for next
        digestPool.add(digest);
        //System.out.println("Digest Pool size = " + digestPool.size());

        // update the cache
        md5Cache.put(key, result); // prevent expensive MD5 computation and encoding
        return result;
    }

    private static byte[] encodeMD5Raw(final byte[] b) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            final InputStream  in = new ByteArrayInputStream(b);
            final byte[] buf = new byte[2048];
            int n;
            while ((n = in.read(buf)) > 0) digest.update(buf, 0, n);
            in.close();
            // now compute the hex-representation of the md5 digest
            return digest.digest();
        } catch (final java.security.NoSuchAlgorithmException e) {
            System.out.println("Internal Error at md5:" + e.getMessage());
        } catch (final java.io.IOException e) {
            System.out.println("byte[] error: " + e.getMessage());
        }
        return null;
    }

}
