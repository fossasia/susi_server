/**
 *  DumpImporter
 *  Copyright 06.01.2016 by Michael Peter Christen, @0rb1t3r
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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.tools.storage.JsonFactory;
import org.loklak.tools.storage.JsonReader;
import org.loklak.tools.storage.JsonStreamReader;

public class DumpImporter extends Thread {

    private boolean shallRun = true, isBusy = false;
    
    /**
     * ask the thread to shut down
     */
    public void shutdown() {
        this.shallRun = false;
        this.interrupt();
        Log.getLog().info("catched DumpImporter termination signal");
    }

    public boolean isBusy() {
        return this.isBusy;
    }
    
    @Override
    public void run() {
        
        // work loop
        loop: while (this.shallRun) try {

            this.isBusy = false;
            
            // scan dump input directory to import files
            Collection<File> import_dumps = DAO.message_dump.getImportDumps();

            // check if we can do anything
            if (import_dumps == null || import_dumps.size() == 0 || !DAO.clusterReady()) {
                try {Thread.sleep(10000);} catch (InterruptedException e) {}
                continue loop;
            }
            this.isBusy = true;

            // take only one file and process this file
            File import_dump = import_dumps.iterator().next();
            final JsonReader dumpReader = DAO.message_dump.getDumpReader(import_dump);
            final AtomicLong newTweets = new AtomicLong(0);
            Log.getLog().info("started import of dump file " + import_dump.getAbsolutePath());
            
            // we start concurrent indexing threads to process the json objects
            Thread[] indexerThreads = new Thread[dumpReader.getConcurrency()];
            for (int i = 0; i < dumpReader.getConcurrency(); i++) {
                indexerThreads[i] = new Thread() {
                    public void run() {
                        JsonFactory tweet;
                        try {
                            while ((tweet = dumpReader.take()) != JsonStreamReader.POISON_JSON_MAP) {
                                try {
                                    Map<String, Object> json = tweet.getJson();
                                    @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) json.remove("user");
                                    if (user == null) continue;
                                    UserEntry u = new UserEntry(user);
                                    MessageEntry t = new MessageEntry(json);
                                    // record user into search index
                                    DAO.users.writeEntry(u.getScreenName(), t.getSourceType().name(), u, true);
                                    DAO.messages.writeEntry(t.getIdStr(), t.getSourceType().name(), t, true);
                                    newTweets.incrementAndGet();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (LoklakServer.queuedIndexing.isBusy()) try {Thread.sleep(200);} catch (InterruptedException e) {}
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                indexerThreads[i].start();
            }
            
            // wait for termination of the indexing threads and do logging meanwhile
            boolean running = true;
            while (running) {
                long startTime = System.currentTimeMillis();
                long startCount = newTweets.get();
                running = false;
                for (int i = 0; i < dumpReader.getConcurrency(); i++) {
                    if (indexerThreads[i].isAlive()) running = true;
                }
                try {Thread.sleep(10000);} catch (InterruptedException e) {}
                long runtime = System.currentTimeMillis() - startTime;
                long count = newTweets.get() - startCount;
                Log.getLog().info("imported " + newTweets.get() + " tweets at " + (count * 1000 / runtime) + " tweets per second from " + import_dump.getName());
            }
            
            // finally flush the caches
            try {DAO.users.bulkCacheFlush();} catch (IOException e) {}
            try {DAO.messages.bulkCacheFlush();} catch (IOException e) {}

            // catch up the number of processed tweets
            Log.getLog().info("finished import of dump file " + import_dump.getAbsolutePath() + ", " + newTweets.get() + " new tweets");
            
            // shift the dump file to prevent that it is imported again
            DAO.message_dump.shiftProcessedDump(import_dump.getName());
            this.isBusy = false;
                    
        } catch (Throwable e) {
            Log.getLog().warn("DumpImporter THREAD", e);
        }

        Log.getLog().info("DumpImporter terminated");
    }
    
}
