/**
 *  Campaigns
 *  Copyright 09.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;

public class Campaigns {
    
    public static enum State implements Comparable<State>, Comparator<State> {
        FUTURE(0),    // campaigns in the future
        ONGOING(1),   // campaigns are ongoing
        WRAPPED(2);   // campaigns are over
        
        int card;
        
        private State(int card) {
            this.card = card;
        }

        public int getCard() {
            return this.card;
        }
        
        @Override
        public int compare(State o1, State o2) {
            return o1.card - o2.card;
        }
        
        public String toFileName() {
            return this.card + "_" + this.name().toLowerCase();
        }
    }
    
    private File storagePath;
    private TreeMap<State, TreeSet<Campaign>> campaigns;
    
    public Campaigns(File storagePath) {
        storagePath.mkdirs();
        this.storagePath = storagePath;
        this.campaigns = new TreeMap<>();
        for (State state: State.values()) {
            File f = new File(storagePath, state.toFileName() + ".txt");
            this.campaigns.put(state, load(f));
        }
    }
    
    public void processCampaigns() {
        long duetime = System.currentTimeMillis();
        Campaign e;
        Iterator<Campaign> i;
        Set<State> needsCommit = new HashSet<>();
        i = this.campaigns.get(State.ONGOING).iterator();
        while (i.hasNext()) {
            e = i.next();
            if (e.getEndTime() >= duetime) {
                i.remove(); this.campaigns.get(State.WRAPPED).add(e);
                needsCommit.add(State.ONGOING); needsCommit.add(State.WRAPPED);
            }
        }
        i = this.campaigns.get(State.FUTURE).iterator();
        while (i.hasNext()) {
            e = i.next();
            if (e.getStartTime() >= duetime) {
                i.remove(); this.campaigns.get(State.ONGOING).add(e);
                needsCommit.add(State.FUTURE); needsCommit.add(State.ONGOING);
            }
        }
        for (State c: needsCommit) {
            save(c);
        }
    }

    public void close() {
        save();
    }
    
    public void save() {
        for (State state: State.values()) {
            save(state);
        }
    }

    public void save(State state) {
        File f = new File(storagePath, state.toFileName() + ".txt");
        save(f, this.campaigns.get(state));
    }
    
    private TreeSet<Campaign> load(File dumpFile) {
        TreeSet<Campaign> campaigns = new TreeSet<>();
        try {
            InputStream is = new FileInputStream(dumpFile);
            String line;
            BufferedReader br = null;
            try {
                if (dumpFile.getName().endsWith(".gz")) is = new GZIPInputStream(is);
                br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                while ((line = br.readLine()) != null) {
                    JSONObject campaign = new JSONObject(line);
                    Campaign e = new Campaign(campaign);
                    campaigns.add(e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null) br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return campaigns;
    }
    
    private void save(File dumpFile, TreeSet<Campaign> campaigns) {
        try {
            OutputStream os = new FileOutputStream(dumpFile);
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                for (Campaign campaign: campaigns) {
                    bw.write(campaign.toString());
                    bw.write('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bw != null) bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
