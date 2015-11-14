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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.api.client.HelloClient;
import org.loklak.api.client.PushClient;
import org.loklak.api.server.SuggestServlet;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.QueryEntry;
import org.loklak.data.Timeline;
import org.loklak.data.UserEntry;
import org.loklak.harvester.TwitterAPI;
import org.loklak.tools.DateParser;
import org.loklak.tools.OS;

import twitter4j.TwitterException;

/**
 * The caretaker class is a concurrent thread which does peer-to-peer operations
 * and data transmission asynchronously.
 */
public class Caretaker extends Thread {

    private boolean shallRun = true;
    
    public final static long startupTime = System.currentTimeMillis();
    public final static long upgradeWait = DateParser.DAY_MILLIS; // 1 day
    public       static long upgradeTime = startupTime + upgradeWait;

    public static BlockingQueue<Timeline> pushToBackendTimeline = new LinkedBlockingQueue<Timeline>();
    public static BlockingQueue<Timeline> receivedFromPushTimeline = new LinkedBlockingQueue<Timeline>();
    
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
        HelloClient.propagate(remote, (int) DAO.getConfig("port.http", 9000), (int) DAO.getConfig("port.https", 9443), (String) DAO.getConfig("peername", "anonymous"));
        
        // work loop
        while (this.shallRun) try {
            if (System.currentTimeMillis() > upgradeTime) {
                // increase the upgrade time to prevent that the peer runs amok (re-tries the attempt all the time) when upgrade fails for any reason
                upgradeTime = upgradeTime + upgradeWait;
                
                // do an upgrade
                DAO.log("UPGRADE: starting an upgrade");
                upgrade();
                DAO.log("UPGRADE: started an upgrade");
            }
            
            // clear caches
            if (SuggestServlet.cache.size() > 100) SuggestServlet.cache.clear();
            
            // dump timelines submitted by the peers
            long dumpstart = System.currentTimeMillis();
            int[] newandknown = scheduledTimelineStorage();
            long dumpfinish = System.currentTimeMillis();
            if (newandknown[0] > 0 || newandknown[1] > 0) {
                DAO.log("dumped timelines from push api: " + newandknown[0] + " new, " + newandknown[1] + " known, storage time: " + (dumpfinish - dumpstart) + " ms");
            }
            if (dumpfinish - dumpstart < 4000) {
                // sleep a bit to prevent that the DoS limit fires at backend server
                try {Thread.sleep(4000 - (dumpfinish - dumpstart));} catch (InterruptedException e) {}
            }
            
            // peer-to-peer operation
            Timeline tl = takeTimelineMin(pushToBackendTimeline, Timeline.Order.CREATED_AT, 100, 1000, 1);
            if (!this.shallRun) break;
            if (tl != null && tl.size() > 0 && remote.length > 0) {
                // transmit the timeline
                long start = System.currentTimeMillis();
                boolean success = PushClient.push(remote, tl);
                if (success) {
                    DAO.log("success pushing " + tl.size() + " messages to backend in 1st attempt in " + (System.currentTimeMillis() - start) + " ms");
                }
                if (!success) {
                    // we should try again.. but not an infinite number because then
                    // our timeline in RAM would fill up our RAM creating a memory leak
                    retrylook: for (int retry = 0; retry < 5; retry++) {
                        // give back-end time to recover
                        try {Thread.sleep(3000 + retry * 3000);} catch (InterruptedException e) {}
                        start = System.currentTimeMillis();
                        if (PushClient.push(remote, tl)) {
                            DAO.log("success pushing " + tl.size() + " messages to backend in " + (retry + 2) + ". attempt in " + (System.currentTimeMillis() - start) + " ms");
                            success = true;
                            break retrylook;
                        }
                    }
                    if (!success) DAO.log("failed pushing " + tl.size() + " messages to backend");
                }
            }
            
            // scan dump input directory to import files
            try {
                DAO.importAccountDumps();
                DAO.importMessageDumps();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            
            // run some harvesting steps
            if (DAO.getConfig("retrieval.forbackend.enabled", false) && (DAO.getConfig("backend", "").length() > 0)) {
                for (int i = 0; i < 5; i++) {
                    int count = Harvester.harvest();
                    if (count == -1) break;
                }
            }
            
            // run some crawl steps
            for (int i = 0; i < 10; i++) {
                if (Crawler.process() == 0) break; // this may produce tweets for the timeline push
            }
            
            // run searches
            if (DAO.getConfig("retrieval.queries.enabled", false)) {
                // execute some queries again: look out in the suggest database for queries with outdated due-time in field retrieval_next
                List<QueryEntry> queryList = DAO.SearchLocalQueries("", 10, "retrieval_next", "date", SortOrder.ASC, null, new Date(), "retrieval_next");
                for (QueryEntry qe: queryList) {
                    if (!acceptQuery4Retrieval(qe.getQuery())) {
                        DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
                        continue;
                    }
                    Timeline t = DAO.scrapeTwitter(null, qe.getQuery(), Timeline.Order.CREATED_AT, qe.getTimezoneOffset(), false, 10000, true);
                    DAO.log("retrieval of " + t.size() + " new messages for q = \"" + qe.getQuery() + "\"");
                    DAO.announceNewUserId(t);
                    try {Thread.sleep(1000);} catch (InterruptedException e) {} // prevent remote DoS protection handling
                }
            }
            
            // retrieve user data
            Set<Number> ids = DAO.getNewUserIdsChunk();
            if (ids != null && DAO.getConfig("retrieval.user.enabled", false) && TwitterAPI.getAppTwitterFactory() != null) {
                try {
                    TwitterAPI.getScreenName(ids, 10000, false);
                } catch (IOException | TwitterException e) {
                    for (Number n: ids) DAO.announceNewUserId(n); // push back unread values
                    if (e instanceof TwitterException) try {Thread.sleep(10000);} catch (InterruptedException ee) {}
                }
            }
            
            // heal the latency to give peers with out-dated information a new chance
            DAO.healLatency(0.95f);
        } catch (Throwable e) {
            Log.getLog().warn("CARETAKER THREAD", e);
        }

        Log.getLog().info("caretaker terminated");
    }

    public static boolean acceptQuery4Retrieval(String q) {
        return q.length() > 1 && q.length() <=16 && q.indexOf(':') < 0;
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
            DAO.log("UPGRADE failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int[] scheduledTimelineStorage() {
        Timeline tl;
        int newMessages = 0, knownMessages = 0;
        while (!receivedFromPushTimeline.isEmpty() && (tl = receivedFromPushTimeline.poll()) != null) {
            for (MessageEntry me: tl) {
                me.enrich(); // we enrich here again because the remote peer may have done this with an outdated version or not at all
                boolean stored = DAO.writeMessage(me, tl.getUser(me), true, true, true);
                if (stored) newMessages++; else knownMessages++;
            }
        }
        return new int[]{newMessages, knownMessages};
    }

    public static void storeTimelineScheduler(Timeline tl) {
        try {
            receivedFromPushTimeline.put(tl);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static void transmitTimelineToBackend(Timeline tl) {
        if (DAO.getConfig("backend", new String[0], ",").length > 0) pushToBackendTimeline.add(tl);
    }
    
    public static void transmitMessage(final MessageEntry tweet, final UserEntry user) {
        if (DAO.getConfig("backend", new String[0], ",").length <= 0) return;
        Timeline tl = pushToBackendTimeline.poll();
        if (tl == null) tl = new Timeline(Timeline.Order.CREATED_AT);
        tl.add(tweet, user);
        pushToBackendTimeline.add(tl);
    }

    public static Timeline takeTimelineMin(final BlockingQueue<Timeline> dumptl, final Timeline.Order order, final int minsize, final int maxsize, final long maxwait) {
        Timeline tl = takeTimelineMax(dumptl, order, minsize, maxwait);
        if (tl.size() >= minsize) {
            // split that and return the maxsize
            Timeline tlr = tl.reduceToMaxsize(minsize);
            dumptl.add(tlr); // push back the remaining
            return tl;
        }
        // push back that timeline and return nothing
        dumptl.add(tl);
        return new Timeline(order);
    }

    private static Timeline takeTimelineMax(final BlockingQueue<Timeline> dumptl, final Timeline.Order order, final int maxsize, final long maxwait) {
        Timeline tl = new Timeline(order);
        try {
            Timeline tl0 = dumptl.poll(maxwait, TimeUnit.MILLISECONDS);
            if (tl0 == null) return tl;
            tl.putAll(tl0);
            while (tl0.size() < maxsize && dumptl.size() > 0 && dumptl.peek().size() + tl0.size() <= maxsize) {
                tl0 = dumptl.take();
                if (tl0 == null) return tl;
                tl.putAll(tl0);
            }
            return tl;
        } catch (InterruptedException e) {
            return tl;
        }
    }
    
}
