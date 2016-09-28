/**
 *  SusiLog
 *  Copyright 24.07.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.susi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.tools.UTF8;

/**
 * Susis log is a kind of reflection about the conversation in the past
 */
public class SusiLog {

    private File root;
    private int rc;
    private Map<String, UserRecord> logs;
    
    public SusiLog(File storageLocation, int rememberLatestCount) {
        this.root = storageLocation;
        this.rc = rememberLatestCount;
        this.logs = new ConcurrentHashMap<>();
    }
    
    /**
     * get a list of interaction using the client key
     * @param client
     * @return a list of interactions, latest interaction is first in list
     */
    public ArrayList<SusiInteraction> getInteractions(String client) {
        UserRecord ur = logs.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            logs.put(client, ur);
        }
        return ur.conversation;
    }
    
    public SusiLog addInteraction(String client, SusiInteraction si) {
        UserRecord ur = logs.get(client);
        if (ur == null) {
            ur = new UserRecord(client);
            logs.put(client, ur);
        }
        ur.add(si);
        return this;
    }
    
    public class UserRecord {
        private ArrayList<SusiInteraction> conversation = null; // first entry always has the latest interaction
        private File logdump;
        public UserRecord(String client) {
            this.conversation = new ArrayList<>();
            File logpath = new File(root, client);
            logpath.mkdirs();
            this.logdump = new File(logpath, "log.txt");
            if (this.logdump.exists()) {
                try {
                    this.conversation = readLog(this.logdump, rc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public UserRecord add(SusiInteraction interaction) {
            if (this.conversation == null) return this;
            this.conversation.add(0, interaction);
            if (this.conversation.size() > rc) this.conversation.remove(this.conversation.size() - 1);
            try {
                Files.write(this.logdump.toPath(), UTF8.getBytes(interaction.getJSON().toString(0) + "\n"), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            return this;
        }
    }
    
    public static ArrayList<SusiInteraction> readLog(final File logdump, int count) throws IOException {
        List<String> lines = Files.readAllLines(logdump.toPath());
        ArrayList<SusiInteraction> conversation = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (line.length() == 0) continue;
            SusiInteraction si = new SusiInteraction(new JSONObject(line));
            conversation.add(si);
            if (conversation.size() >= count) break;
        }
        return conversation;
    }
    
    public TreeMap<Long, List<SusiInteraction>> getAllLogs() {
        TreeMap<Long, List<SusiInteraction>> all = new TreeMap<>();
        String[] clients = this.root.list();
        for (String client: clients) {
            File logpath = new File(this.root, client);
            if (logpath.exists()) {
                File logdump = new File(logpath, "log.txt");
                if (logdump.exists()) try {
                    ArrayList<SusiInteraction> conversation = readLog(logdump, Integer.MAX_VALUE);
                    if (conversation.size() > 0) {
                        Date d = conversation.get(0).getQueryDate();
                        all.put(-d.getTime(), conversation);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return all;
    }
    
}
