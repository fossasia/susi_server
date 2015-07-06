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
import java.util.Date;
import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.search.sort.SortOrder;
import org.loklak.api.client.HelloClient;
import org.loklak.api.client.PushClient;
import org.loklak.data.DAO;
import org.loklak.data.QueryEntry;
import org.loklak.data.Timeline;

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
        HelloClient.propagate(remote, (int) DAO.getConfig("port.http", 9000), (int) DAO.getConfig("port.https", 9443), (String) DAO.getConfig("peername", "anonymous"));

        try {Thread.sleep(10000);} catch (InterruptedException e) {} // wait a bit to give elasticsearch a start-up time
        while (this.shallRun) {
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(5000);} catch (InterruptedException e) {}
            
            // peer-to-peer operation
            Timeline tl = DAO.takeTimeline(Timeline.Order.CREATED_AT, 500, 3000);
            if (!this.shallRun) break;
            if (tl != null && tl.size() > 0 && remote.length > 0) {
                // transmit the timeline
                boolean success = PushClient.push(remote, tl);
                if (!success) {
                    // we should try again.. but not an infinite number because then
                    // our timeline in RAM would fill up our RAM creating a memory leak
                    retrylook: for (int retry = 0; retry < 3; retry++) {
                        // give back-end time to recover
                        if (PushClient.push(remote, tl)) {
                            DAO.log("success pushing to backend in " + retry + " attempt");
                            break retrylook;
                        }
                        try {Thread.sleep(3000 + retry * 3000);} catch (InterruptedException e) {}
                    }
                    DAO.log("failed pushing " + tl.size() + " messages to backend");
                }
            }
            
            // scan dump input directory to import files
            File[] importList = DAO.getTweetImportDumps();
            for (File importFile: importList) {
                String name = importFile.getName();
                DAO.log("importing tweet dump " + name);
                int imported = DAO.importDump(importFile);
                DAO.shiftProcessedTweetDump(name);
                DAO.log("imported tweet dump " + name + ", " + imported + " new messages");
            }
            
            // scan spacial data import directory
            importList = DAO.getGeoJsonImportDumps();
            for (File importFile: importList) {
                String name = importFile.getName();
                DAO.log("importing geoJson " + name);
                int imported = DAO.importGeoJson(importFile);
                DAO.shiftProcessedGeoJsonDump(name);
                DAO.log("imported geoJson " + name + ", " + imported + " new messages");
            }
            
            // run some crawl steps
            for (int i = 0; i < 10; i++) {
                if (Crawler.process() == 0) break; // this may produce tweets for the timeline push
            }
            
            if (DAO.getConfig("retrieval.enabled", false)) {
                // execute some queries again: look out in the suggest database for queries with outdated due-time in field retrieval_next
                List<QueryEntry> queryList = DAO.SearchLocalQueries("", 10, "retrieval_next", SortOrder.ASC, null, new Date(), "retrieval_next");
                for (QueryEntry qe: queryList) {
                    if (!acceptQuery4Retrieval(qe.getQuery())) {
                        DAO.deleteQuery(qe.getQuery(), qe.getSourceType());
                        continue;
                    }
                    Timeline[] t = DAO.scrapeTwitter(qe.getQuery(), Timeline.Order.CREATED_AT, qe.getTimezoneOffset(), false);
                    DAO.log("automatic retrieval of " + t[0].size() + " messages, " + t[1].size() + " new for q = \"" + qe.getQuery() + "\"");
                    try {Thread.sleep(1000);} catch (InterruptedException e) {} // prevent remote DoS protection handling
                }
            }
        }

        Log.getLog().info("caretaker terminated");
    }

    public static boolean acceptQuery4Retrieval(String q) {
        return q.length() > 1 && q.length() <=16 && q.indexOf(':') < 0;
    }
    
}
