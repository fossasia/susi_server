/**
 *  AccessTracker
 *  Copyright 11.10.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListMap;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonRepository;
import ai.susi.tools.DateParser;

public class AccessTracker extends Thread {

    private final static String START_DATE_KEY    = "start";
    private final static String FINISH_DATE_KEY   = "finish";
    private final static String CLIENT_KEY        = "host"; // host address of the client
    private final static String LOCALHOST_FLAG    = "local"; // boolean from isLocalhost
    private final static String COMMENT_KEY       = "comment"; // to write i.e. termination reason
    private final static String IDLE_TIME_KEY     = "idle";
    private final static String DOS_BLACKOUT_KEY  = "dosb";
    private final static String DOS_REDUCTION_KEY = "dosr";
    private final static String RUNTIME_KEY       = "busy";
    private final static String QUERY_KEY         = "query";
    
    public final static String EVENT_PREFIX = "event_";

    private final static String COMMENT_CLOSED = "closed";
    
    public final static int MAX_FINISHED = 1000;
    
    private JsonRepository history;
    private long track_timeout;
    private long schedule_period;
    private boolean terminate;
    private ConcurrentSkipListMap<Date, Track> pendingQueue, finishedQueue;
    
    
    public AccessTracker(File dump_dir, String dump_file_prefix, long track_timeout, long schedule_period) throws IOException {
        this.history = new JsonRepository(dump_dir, dump_file_prefix, null, JsonRepository.Mode.COMPRESSED, false, 1);
        this.track_timeout = track_timeout;
        this.schedule_period = schedule_period;
        this.terminate = false;
        this.pendingQueue = new ConcurrentSkipListMap<>();
        this.finishedQueue = new ConcurrentSkipListMap<>();
    }
    
    public Collection<Track> getTracks() {
        List<Track> tracks = new ArrayList<>();
        for (Track track: this.pendingQueue.descendingMap().values()) tracks.add(track);
        for (Track track: this.finishedQueue.descendingMap().values()) tracks.add(track);
        return tracks;
    }
    
    public void run() {
        Thread.currentThread().setName("AccessTracker");
        monitor: while (!terminate) {
            // identify oldest track and remove it from the pending tracks if it is over timeout time
            timeoutcheck: while (this.pendingQueue.size() > 0) {
                Map.Entry<Date, Track> t = this.pendingQueue.firstEntry();
                if (t == null) break timeoutcheck;
                boolean timeout = t.getKey().getTime() + AccessTracker.this.track_timeout < System.currentTimeMillis();
                if (timeout || t.getValue().has(FINISH_DATE_KEY)) {
                    try {
                        writeToHistory(t.getValue(), null);
                        this.pendingQueue.remove(t.getKey());
                        this.finishedQueue.put(t.getKey(), t.getValue());
                        while (this.finishedQueue.size() > MAX_FINISHED) this.finishedQueue.remove(this.finishedQueue.firstKey());
                        continue timeoutcheck;
                    } catch (IOException e) {
                    	DAO.severe(e);
                        break monitor;
                    }
                }
                break timeoutcheck;
            }
            
            try {Thread.sleep(this.schedule_period);} catch (InterruptedException e) {if (this.terminate) break monitor;}
        }
        try {
            for (Track track: this.pendingQueue.values()) writeToHistory(track, COMMENT_CLOSED);
        } catch (IOException e) {
        	DAO.severe(e);
        }
    }
    
    private void writeToHistory(Track track, String comment) throws IOException {
        if (comment != null) track.put(COMMENT_KEY, comment);
        this.history.write(track);
    }
    
    public void close() {
        // if the daemon is running, terminate first
        this.terminate = true;
        if (this.isAlive()) try {this.interrupt(); this.join(10000);} catch (InterruptedException e) {}

        // write remaining tracks from pending queue
        try {
            for (Track track: this.pendingQueue.values()) writeToHistory(track, COMMENT_CLOSED);
        } catch (IOException e) {
        	DAO.severe(e);
        }
    }
    
    public class Track extends JSONObject {
        
        public String getClientHost() {
            return clientHost;
        }

        private Date accessTime;
        private String clientHost;
        private long time_since_last_access;
        private boolean isLocalhost;
        private boolean DoS_blackout, DoS_servicereduction;
        
        public Track(String servlet, String clientHost) {
            this.clientHost = clientHost;
            long time = System.currentTimeMillis();
            Date lastDate = null;
            try {lastDate = AccessTracker.this.pendingQueue.lastKey();} catch (NoSuchElementException e) {}
            long last = lastDate == null ? time - 1 : lastDate.getTime();
            if (time <= last) time = last + 1;
            this.accessTime = new Date(time);
            this.put(START_DATE_KEY, DateParser.iso8601MillisFormat.format(accessTime));
            this.put(CLIENT_KEY, clientHost);
            this.isLocalhost = RemoteAccess.isLocalhost(clientHost);
            this.put(LOCALHOST_FLAG, this.isLocalhost);
            AccessTracker.this.pendingQueue.put(accessTime, this);
        }
        
        public Track(String serialized) {
            try {
                JSONObject json = new JSONObject(serialized);
                this.putAll(json);
            } catch (Throwable e) {
                DAO.log("cannot parse \"" + serialized + "\"");
            } 
        }
        
        public Date getDate() {
            return this.accessTime;
        }

        public long getTimeSinceLastAccess() {
            return time_since_last_access;
        }

        public void setTimeSinceLastAccess(long time_since_last_access) {
            this.time_since_last_access = time_since_last_access;
            this.put(IDLE_TIME_KEY, time_since_last_access);
        }

        public boolean isDoSBlackout() {
            return DoS_blackout;
        }

        public void setDoSBlackout(boolean doS_blackout) {
            DoS_blackout = doS_blackout;
            this.put(DOS_BLACKOUT_KEY, doS_blackout);
        }

        public boolean isDoSServicereduction() {
            return DoS_servicereduction;
        }

        public void setDoSServicereduction(boolean doS_servicereduction) {
            DoS_servicereduction = doS_servicereduction;
            this.put(DOS_REDUCTION_KEY, doS_servicereduction);
        }
        
        public boolean isLocalhostAccess() {
            return this.isLocalhost;
        }
        
        public void setQuery(final Map<String, String> qm) {
            if (qm == null) return;
            Map<String, Object> m = new LinkedHashMap<>();
            m.putAll(qm);
            this.put(QUERY_KEY, m);
        }
        
        public String toString() {
            return new JSONObject(this).toString();
        }
        
        public void finalize() {
            Date finishTime = new Date();
            long runtime = finishTime.getTime() - this.accessTime.getTime();
            this.put(RUNTIME_KEY, runtime);
            this.put(FINISH_DATE_KEY, DateParser.iso8601MillisFormat.format(finishTime));
        }
    }

    public Track startTracking(String servlet, String clientHost) {
        return new Track(servlet, clientHost);
    }
    
    
}
