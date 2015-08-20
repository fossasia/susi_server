//OS.java
//-------------------------------------------
//(C) by Michael Peter Christen; mc@yacy.net
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004
//last major change: 11.03.2004
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

// this class was taken from the YaCy project

package org.loklak.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.loklak.data.DAO;

public final class OS {

    // constants for system identification
    public enum System {
        MacOSX,  // all Mac OS X
        Unix,    // all Unix/Linux type systems
        Windows, // all Windows 95/98/NT/2K/XP
        Unknown; // any other system
    }

    // system-identification statics
    public static final System  systemOS;
    public static final boolean isMacArchitecture;
    public static final boolean isUnixFS;
    public static final boolean canExecUnix;
    public static final boolean isWindows;
    public static final boolean isWin32;

    // calculated system constants
    public static int maxPathLength = 65535;

    // Macintosh-specific statics
    public  static final Map<String, String> macFSTypeCache = new HashMap<String, String>();
    public  static final Map<String, String> macFSCreatorCache = new HashMap<String, String>();

    // static initialization
    static {
        // check operation system type
        final Properties sysprop = java.lang.System.getProperties();
        final String sysname = sysprop.getProperty("os.name","").toLowerCase();
        if (sysname.startsWith("mac os x")) systemOS = System.MacOSX;
        else if (sysname.startsWith("windows")) systemOS = System.Windows;
        else if ((sysname.startsWith("linux")) || (sysname.startsWith("unix"))) systemOS = System.Unix;
        else systemOS = System.Unknown;

        isMacArchitecture = systemOS == System.MacOSX;
        isUnixFS = systemOS == System.MacOSX || systemOS == System.Unix;
        canExecUnix = isUnixFS || systemOS != System.Windows;
        isWindows = systemOS == System.Windows;
        isWin32 = isWindows && java.lang.System.getProperty("os.arch", "").contains("x86");

        // set up maximum path length according to system
        if (isWindows) maxPathLength = 255; else maxPathLength = 65535;
    }

    private static long copy(final InputStream source, final OutputStream dest, final long count)
            throws IOException {
        assert count < 0 || count > 0 : "precondition violated: count == " + count + " (nothing to copy)";
        if ( count == 0 ) {
            // no bytes to copy
            return 0;
        }

        final byte[] buffer = new byte[2048];
        int chunkSize = (int) ((count > 0) ? Math.min(count, 2048) : 2048);

        int c;
        long total = 0;
        while ( (c = source.read(buffer, 0, chunkSize)) > 0 ) {
            dest.write(buffer, 0, c);
            dest.flush();
            total += c;

            if ( count > 0 && count == total) {
                break;
            }

        }
        dest.flush();

        return total;
    }

    private static void copy(final InputStream source, final File dest, final long count) throws IOException {
        final String path = dest.getParent();
        if ( path != null && path.length() > 0 ) {
            new File(path).mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dest);
            copy(source, fos, count);
        } finally {
            if ( fos != null ) {
                try {
                    fos.close();
                } catch (final Exception e ) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void copy(final InputStream source, final File dest) throws IOException {
        copy(source, dest, -1);
    }


    private static void copy(final byte[] source, final File dest) throws IOException {
        copy(new ByteArrayInputStream(source), dest);
    }

    private static void deployScript(final File scriptFile, final String theScript) throws IOException {
        copy(UTF8.getBytes(theScript), scriptFile);
        if(!isWindows){ // set executable
            try {
                Runtime.getRuntime().exec("chmod 755 " + scriptFile.getAbsolutePath().replaceAll(" ", "\\ ")).waitFor();
            } catch (final InterruptedException e) {
                DAO.log("DEPLOY of script file failed. file = " + scriptFile.getAbsolutePath());
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * use a hack to get the current process PID
     * @return the PID of the current java process or -1 if the PID cannot be obtained
     */
    public static int getPID() {
        final String pids = ManagementFactory.getRuntimeMXBean().getName();
        final int p = pids.indexOf('@');
        return p >= 0 ? parseIntDecSubstring(pids, 0, p) : -1;
    }

    private static final int parseIntDecSubstring(String s, int startPos, final int endPos) throws NumberFormatException {
        if (s == null || endPos > s.length() || endPos <= startPos) throw new NumberFormatException(s);

        int result = 0;
        boolean negative = false;
        int i = startPos;
        int limit = -Integer.MAX_VALUE;
        final int multmin;
        int digit;
        char c;

        char firstChar = s.charAt(i);
        if (firstChar < '0') {
            if (firstChar == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
            } else if (firstChar != '+') throw new NumberFormatException(s);
            i++;
            if (endPos == i) throw new NumberFormatException(s);
        }
        multmin = limit / 10;
        while (i < endPos) {
            c = s.charAt(i++);
            if (c == ' ') break;
            digit = c - '0';
            if (digit < 0  || digit > 9 || result < multmin) throw new NumberFormatException(s);
            result *= 10;
            //result = (result << 3) + (result << 1);
            if (result < limit + digit) throw new NumberFormatException(s);
            result -= digit;
        }
        return negative ? result : -result;
    }

    private static final String LF_STRING = UTF8.String(new byte[]{10});

    public static void execAsynchronous(final File scriptFile) throws IOException {
        // runs a script as separate thread
        String starterFileExtension = null;
        String script = null;
        if(isWindows){
            starterFileExtension = ".starter.bat";
            // use /K to debug, /C for release
            script = "start /MIN CMD /C \"" + scriptFile.getAbsolutePath() + "\"";
        } else { // unix/linux
            starterFileExtension = ".starter.sh";
            script = "#!/bin/sh" + LF_STRING + scriptFile.getAbsolutePath().replaceAll(" ", "\\ ") + " &" + LF_STRING;
        }
        final File starterFile = new File(scriptFile.getAbsolutePath().replaceAll(" ", "\\ ") + starterFileExtension);
        deployScript(starterFile, script);
        try {
            Runtime.getRuntime().exec(starterFile.getAbsolutePath().replaceAll(" ", "\\ ")).waitFor();
        } catch (final InterruptedException e) {
            throw new IOException(e.getMessage());
        }
        starterFile.delete();
    }

    public static List<String> execSynchronous(final String command) throws IOException {
        // runs a unix/linux command and returns output as Vector of Strings
        // this method blocks until the command is executed
        final Process p = Runtime.getRuntime().exec(command);
        return execSynchronousProcess(p);
    }

    private static List<String> execSynchronousProcess(Process p) throws IOException {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final List<String> output = new ArrayList<String>();
        while ((line = in.readLine()) != null) output.add(line);
        in.close();
        in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = in.readLine()) != null) output.add(line);
        in.close();
        return output;
    }

}
