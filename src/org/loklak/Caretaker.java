/**
 *  Caretaker
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

package org.loklak;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.tools.OS;


/**
 * The caretaker class is a concurrent thread which does peer-to-peer operations
 * and data transmission asynchronously.
 */
public class Caretaker extends Thread {
    
    private boolean shallRun = true;
    
    public  final static long startupTime = System.currentTimeMillis();
    private final static long upgradeWait = 60000; //DateParser.DAY_MILLIS; // 1 day
    public        static long upgradeTime = startupTime + upgradeWait;
    
    /**
     * ask the thread to shut down
     */
    public void shutdown() {
        this.shallRun = false;
        this.interrupt();
        Log.getLog().info("catched caretaker termination signal");
    }
    
    @Override
    public void run() {
        
        boolean busy = false;
        // work loop
        beat: while (this.shallRun) try {
            // check upgrade time
            if (System.currentTimeMillis() > upgradeTime) {
                // increase the upgrade time to prevent that the peer runs amok (re-tries the attempt all the time) when upgrade fails for any reason
                upgradeTime = upgradeTime + upgradeWait;
                
                // do an upgrade
                DAO.log("UPGRADE: starting an upgrade");
                upgrade();
                DAO.log("UPGRADE: started an upgrade");
            }
            
            
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(busy ? 1000 : 5000);} catch (InterruptedException e) {}
            if (!this.shallRun) break beat;
            busy = false;
            
            
            // heal the latency to give peers with out-dated information a new chance
            DAO.healLatency(0.95f);
            
        } catch (Throwable e) {
            Log.getLog().warn("CARETAKER THREAD", e);
        }

        Log.getLog().info("caretaker terminated");
    }

    public static boolean acceptQuery4Retrieval(String q) {
        return q.length() > 1 && q.length() <=16 && (q.indexOf(':') < 0 || q.startsWith("from:"));
    }
    
    /**
     * loklak upgrades itself if this is called
     */
    public static void upgrade() {
        final File upgradeScript = new File(DAO.bin_dir.getAbsolutePath().replaceAll(" ", "\\ "), "upgrade.sh");
      
        try {
            List<String> rsp = OS.execSynchronous(upgradeScript.getAbsolutePath());
            for (String s: rsp) DAO.log("UPGRADE: " + s);
        } catch (IOException e) {
        	Log.getLog().warn("UPGRADE failed: " + e.getMessage(), e);
        }
    }
    
}
