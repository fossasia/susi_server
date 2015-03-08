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
        String[] remote = DAO.getConfig("backend", "").split(",");
        HelloClient.propagate(remote, (int) DAO.getConfig("port.http", 9100), (int) DAO.getConfig("port.https", 9443));
        
        while (this.shallRun) {
            // peer-to-peer operation
            Timeline tl = DAO.takeTimeline(100, 3000);
            if (!this.shallRun) break;
            if (tl != null && tl.size() > 0) {
                // transmit the timeline
                PushClient.push(remote, tl);
            }
            
            // scan dump input directory to import files
            File[] importList = DAO.getImportDumps();
            for (File importFile: importList) {
                String name = importFile.getName();
                if (name.startsWith(DAO.MESSAGE_DUMP_FILE_PREFIX)) {
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
