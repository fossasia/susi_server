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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import ai.susi.tools.TimeoutMatcher;
import org.joda.time.DateTime;

import java.io.File;
import java.util.Collection;


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
        DAO.log("catched caretaker termination signal");
    }

    public void deleteOldFiles() {
        Collection<File> filesToDelete = FileUtils.listFiles(new File(DAO.deleted_skill_dir.getPath()),
                new AgeFileFilter(DateTime.now().withTimeAtStartOfDay().minusDays(30).toDate()),
                TrueFileFilter.TRUE);    // include sub dirs
        for (File file : filesToDelete) {
            boolean success = FileUtils.deleteQuietly(file);
            if (!success) {
                System.out.print("Deleted skill older than 30 days.");
            }
        }
    }
    @Override
    public void run() {
        
        boolean busy = false;
        // work loop
        beat: while (this.shallRun) try {
            deleteOldFiles();
            // sleep a bit to prevent that the DoS limit fires at backend server
            try {Thread.sleep(busy ? 1000 : 5000);} catch (InterruptedException e) {}
            TimeoutMatcher.terminateAll();

            if (!this.shallRun) break beat;
            busy = false;

        } catch (Throwable e) {
            DAO.severe("CARETAKER THREAD", e);
        }

        DAO.log("caretaker terminated");
    }
    
}
