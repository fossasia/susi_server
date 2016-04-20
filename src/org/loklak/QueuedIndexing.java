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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.Timeline;
import org.loklak.objects.UserEntry;
import org.loklak.tools.storage.JsonRepository;

public class QueuedIndexing extends Thread {
    
    private final static int MESSAGE_QUEUE_MAXSIZE = 100000;
    private final static int bufferLimit = MESSAGE_QUEUE_MAXSIZE * 3 / 4;
    private static BlockingQueue<MessageWrapper> messageQueue = new ArrayBlockingQueue<MessageWrapper>(MESSAGE_QUEUE_MAXSIZE);
    private static AtomicInteger queueClients = new AtomicInteger(0);

    private boolean shallRun = true, isBusy = false;
    private JsonRepository jsonBufferHandler;
    
    public static int getMessageQueueSize() {
        return messageQueue.size();
    }
    
    public static int getMessageQueueMaxSize() {
        return MESSAGE_QUEUE_MAXSIZE;
    }
    
    public static int getMessageQueueClients() {
        return queueClients.get();
    }
    
    public QueuedIndexing(JsonRepository jsonBufferHandler) {
        this.jsonBufferHandler = jsonBufferHandler;
    }
    
    public static class MessageWrapper {
        public MessageEntry t;
        public UserEntry u;
        public boolean dump, overwriteUser;
        public MessageWrapper(MessageEntry t, UserEntry u, boolean dump, boolean overwriteUser) {
            this.t = t;
            this.u = u;
            this.dump = dump;
            this.overwriteUser = overwriteUser;
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
            
            if (messageQueue.isEmpty() || !DAO.wait_ready(1000)) {
                // in case that the queue is empty, try to fill it with previously pushed content
                //List<Map<String, Object>> shard = this.jsonBufferHandler.getBufferShard();
                // if the shard has content, turn this into messages again
                
                
                // if such content does not exist, simply sleep a while
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
                
                // in case that the message queue is too large, dump the queue into a file here
                // to make room that clients can continue to push without blocking
                /*
                if (messageQueue.size() > bufferLimit) {
                    this.jsonBufferHandler.buffer(mw.t.getCreatedAt(), mw.t.toMap(mw.u, false, Integer.MAX_VALUE, ""));
                    continue pollloop;
                }
                */
                
                // if there is time enough to finish this, contine to write into the index
                mw.t.enrich(); // we enrich here again because the remote peer may have done this with an outdated version or not at all
                boolean stored = DAO.writeMessage(mw.t, mw.u, mw.dump, mw.overwriteUser, true);
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
    
    public static void addScheduler(Timeline tl, final boolean dump, final boolean overwriteUser) {
        queueClients.incrementAndGet();
        for (MessageEntry me: tl) addScheduler(me, tl.getUser(me), dump, overwriteUser);
        queueClients.decrementAndGet();
    }
    
    public static void addScheduler(final MessageEntry t, final UserEntry u, final boolean dump, final boolean overwriteUser) {
        try {
            messageQueue.put(new MessageWrapper(t, u, dump, overwriteUser));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
