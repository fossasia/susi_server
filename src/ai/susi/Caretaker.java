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

package ai.susi;

import org.eclipse.jetty.util.log.Log;

import ai.susi.tools.TimeoutMatcher;


/**
 * The caretaker class is a concurrent thread which does peer-to-peer operations
 * and data transmission asynchronously.
 */
public class Caretaker extends Thread {
    
    private boolean shallRun = true;
    
    public  final static long startupTime = System.currentTimeMillis();
    
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
            
            
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(busy ? 1000 : 5000);} catch (InterruptedException e) {}
            TimeoutMatcher.terminateAll();
            
            if (!this.shallRun) break beat;
            busy = false;
            
        } catch (Throwable e) {
            Log.getLog().warn("CARETAKER THREAD", e);
        }

        Log.getLog().info("caretaker terminated");
    }
    
}
