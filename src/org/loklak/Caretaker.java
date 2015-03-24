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

import org.eclipse.jetty.util.log.Log;
import org.loklak.api.client.HelloClient;
import org.loklak.api.client.PushClient;

/**
 * The caretaker class is a concurrent thread which does peer-to-peer operations
 * and data transmission asynchronously.
 */
public class Caretaker extends Thread {

    private boolean shallRun = true;
    
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
        // send a message to other peers that I am alive
        String[] remote = DAO.getConfig("backend", new String[0], ",");
        HelloClient.propagate(remote, (int) DAO.getConfig("port.http", 9100), (int) DAO.getConfig("port.https", 9443), (String) DAO.getConfig("peername", "anonymous"));
        
        while (this.shallRun) {
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(3000);} catch (InterruptedException e) {}
            
            // peer-to-peer operation
            Timeline tl = DAO.takeTimeline(500, 3000);
            if (!this.shallRun) break;
            if (tl != null && tl.size() > 0 && remote.length > 0) {
                // transmit the timeline
                boolean success = PushClient.push(remote, tl);
                if (!success) {
                    // we should try again.. but not an infinite number because then
                    // our timeline in RAM would fill up our RAM creating a memory leak
                    retrylook: for (int retry = 0; retry < 3; retry++) {
                        // give back-end time to recover
                        try {Thread.sleep(3000 + retry * 3);} catch (InterruptedException e) {}
                        if (PushClient.push(remote, tl)) {
                            DAO.log("success pushing to backend in " + retry + " attempt");
                            break retrylook;
                        }
                    }
                    DAO.log("failed pushing " + tl.size() + " messages to backend");
                }
            }
            
            // scan dump input directory to import files
            File[] importList = DAO.getImportDumps();
            for (File importFile: importList) {
                String name = importFile.getName();
                if (name.startsWith(DAO.MESSAGE_DUMP_FILE_PREFIX)) {
                    DAO.log("importing file " + name);
                    int imported = DAO.importDump(importFile);
                    DAO.shiftProcessedDump(name);
                    DAO.log("imported file " + name + ", " + imported + " new messages");
                }
            }
            
            // run some crawl steps
            for (int i = 0; i < 10; i++) {
                if (Crawler.process() == 0) break; // this may produce tweets for the timeline push
            }
        }

        Log.getLog().info("caretaker terminated");
    }

}
