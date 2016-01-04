/**
 *  QueuedIndexing
 *  Copyright 04.01.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.Timeline;

public class QueuedIndexing extends Thread {

    private boolean shallRun = true;
    private static BlockingQueue<Timeline> receivedFromPushTimeline = new ArrayBlockingQueue<Timeline>(1000);
    
    /**
     * ask the thread to shut down
     */
    public void shutdown() {
        this.shallRun = false;
        this.interrupt();
        Log.getLog().info("catched QueuedIndexing termination signal");
    }
    
    @Override
    public void run() {
        
        // work loop
        loop: while (this.shallRun) try {
            
            if (receivedFromPushTimeline.isEmpty() || !DAO.clusterReady()) {
                try {Thread.sleep(10000);} catch (InterruptedException e) {}
                continue loop;
            }
            
            // dump timelines submitted by the peers
            long dumpstart = System.currentTimeMillis();
            int newMessages = 0, knownMessages = 0;
            Timeline tl = receivedFromPushTimeline.poll();
            assert tl != null; // because we tested in the beginning of the loop that it is not empty
            for (MessageEntry me: tl) {
                me.enrich(); // we enrich here again because the remote peer may have done this with an outdated version or not at all
                boolean stored = DAO.writeMessage(me, tl.getUser(me), true, true, true);
                if (stored) newMessages++; else knownMessages++;
            }
            long dumpfinish = System.currentTimeMillis();
            DAO.log("dumped timelines from push api: " + newMessages + " new, " + knownMessages + " known, storage time: " + (dumpfinish - dumpstart) + " ms, remaining timelines: " + receivedFromPushTimeline.size());
        } catch (Throwable e) {
            Log.getLog().warn("CARETAKER THREAD", e);
        }

        Log.getLog().info("QueuedIndexing terminated");
    }
    
    public static int storeTimelineScheduler(Timeline tl) {
        try {
            receivedFromPushTimeline.put(tl);
            return receivedFromPushTimeline.size();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
}
