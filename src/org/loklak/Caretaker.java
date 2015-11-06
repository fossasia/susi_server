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

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.api.client.HelloClient;
import org.loklak.api.client.PushClient;
import org.loklak.api.server.SuggestServlet;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;
import org.loklak.data.Timeline;
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
        while (this.shallRun) {
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
            
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(4000);} catch (InterruptedException e) {}
            
            // peer-to-peer operation
            Timeline tl = DAO.takeTimelineMin(Timeline.Order.CREATED_AT, 100, 1000, 1);
            if (!this.shallRun) break;
            if (tl != null && tl.size() > 0 && remote.length > 0) {
                // transmit the timeline
                try {Thread.sleep(2000);} catch (InterruptedException e) {}
                boolean success = PushClient.push(remote, tl);
                if (success) {
                    DAO.log("success pushing " + tl.size() + " messages to backend in 1st attempt");
                }
                if (!success) {
                    // we should try again.. but not an infinite number because then
                    // our timeline in RAM would fill up our RAM creating a memory leak
                    retrylook: for (int retry = 0; retry < 3; retry++) {
                        // give back-end time to recover
                        try {Thread.sleep(3000 + retry * 3000);} catch (InterruptedException e) {}
                        if (PushClient.push(remote, tl)) {
                            DAO.log("success pushing " + tl.size() + " messages to backend in " + (retry + 2) + ". attempt");
                            break retrylook;
                        }
                    }
                    DAO.log("failed pushing " + tl.size() + " messages to backend");
                }
            }
            
            // scan dump input directory to import files
            try {
                DAO.importAccountDumps();
                DAO.importMessageDumps();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            
            // run some crawl steps
            for (int i = 0; i < 10; i++) {
                if (Crawler.process() == 0) break; // this may produce tweets for the timeline push
            }
            
            // run automatic searches
            if (DAO.getConfig("retrieval.queries.enabled", false)) {
                // execute some queries again: look out in the suggest database for queries with outdated due-time in field retrieval_next
                List<QueryEntry> queryList = DAO.SearchLocalQueries("", 10, "retrieval_next", "date", SortOrder.ASC, null, new Date(), "retrieval_next");
                for (QueryEntry qe: queryList) {
                    if (!acceptQuery4Retrieval(qe.getQuery())) {
                        DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
                        continue;
                    }
                    Timeline t = DAO.scrapeTwitter(null, qe.getQuery(), Timeline.Order.CREATED_AT, qe.getTimezoneOffset(), false, 10000, true);
                    DAO.log("automatic retrieval of " + t.size() + " new messages for q = \"" + qe.getQuery() + "\"");
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
    
}
