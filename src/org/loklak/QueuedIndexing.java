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
import org.loklak.objects.MessageEntry;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;

public class QueuedIndexing extends Thread {

    private boolean shallRun = true, isBusy = false;
    private static BlockingQueue<MessageWrapper> messageQueue = new ArrayBlockingQueue<MessageWrapper>(100000);
    
    public QueuedIndexing() {
    }
    
    public static class MessageWrapper {
        public MessageEntry t;
        public UserEntry u;
        public boolean dump, overwriteUser, bulk;
        public MessageWrapper(MessageEntry t, UserEntry u, boolean dump, boolean overwriteUser, boolean bulk) {
            this.t = t;
            this.u = u;
            this.dump = dump;
            this.overwriteUser = overwriteUser;
            this.bulk = bulk;
        }
    }
    
    /**
     * ask the thread to shut down
     */
    public void shutdown() {
        this.shallRun = false;
        this.interrupt();
        Log.getLog().info("catched QueuedIndexing termination signal");
    }
    
    public boolean isBusy() {
        return this.isBusy;
    }
    
    @Override
    public void run() {
        
        // work loop
        loop: while (this.shallRun) try {
            this.isBusy = false;
            
            if (messageQueue.isEmpty() || !DAO.isReady()) {
                try {Thread.sleep(10000);} catch (InterruptedException e) {}
                continue loop;
            }

            MessageWrapper mw;
            this.isBusy = true;
            long dumpstart = System.currentTimeMillis();
            int newMessages = 0, knownMessagesCache = 0, knownMessagesIndex = 0;
            pollloop: while ((mw = messageQueue.poll()) != null) {
                if (DAO.messages.existsCache(mw.t.getIdStr())) {
                     knownMessagesCache++;
                     continue pollloop;
                }
                mw.t.enrich(); // we enrich here again because the remote peer may have done this with an outdated version or not at all
                boolean stored = DAO.writeMessage(mw.t, mw.u, mw.dump, mw.overwriteUser, mw.bulk);
                if (stored) newMessages++; else knownMessagesIndex++;
            }
           
            long dumpfinish = System.currentTimeMillis();
            DAO.log("dumped timelines: " + newMessages + " new, " + knownMessagesCache + " known from cache, "  + knownMessagesIndex + " known from index, storage time: " + (dumpfinish - dumpstart) + " ms, remaining messages: " + messageQueue.size());

            this.isBusy = false;
        } catch (Throwable e) {
            Log.getLog().warn("QueuedIndexing THREAD", e);
        }

        Log.getLog().info("QueuedIndexing terminated");
    }
    
    public static void addScheduler(Timeline tl, final boolean dump, final boolean overwriteUser, final boolean bulk) {
        for (MessageEntry me: tl) addScheduler(me, tl.getUser(me), dump, overwriteUser, bulk);
    }
    
    public static void addScheduler(final MessageEntry t, final UserEntry u, final boolean dump, final boolean overwriteUser, final boolean bulk) {
        try {
            messageQueue.put(new MessageWrapper(t, u, dump, overwriteUser, bulk));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
