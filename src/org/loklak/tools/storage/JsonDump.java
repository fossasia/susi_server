/**
 *  JsonDump
 *  Copyright 16.07.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.tools.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jetty.util.log.Log;
import org.loklak.data.DAO;
import org.loklak.tools.UTF8;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDump {

    // special keys which can be added to the data set to track changes
    public final static byte[] OPERATION_KEY = "$P".getBytes();
    public final static byte[] MOD_DATE_KEY  = "$D".getBytes();
    public final static byte[] REFERRER_KEY  = "$U".getBytes();
    
   
    final File dump_dir, dump_dir_own, dump_dir_import, dump_dir_imported;
    final String dump_file_prefix;
    final RandomAccessFile json_log;
    
    public JsonDump(File dump_dir, String dump_file_prefix, String readme) throws IOException {
        this.dump_dir = dump_dir;
        this.dump_file_prefix = dump_file_prefix;
        this.dump_dir_own = new File(this.dump_dir, "own");
        this.dump_dir_import = new File(this.dump_dir, "import");
        this.dump_dir_imported = new File(this.dump_dir, "imported");
        this.dump_dir.mkdirs();
        this.dump_dir_own.mkdirs();
        this.dump_dir_import.mkdirs();
        this.dump_dir_imported.mkdirs();
        if (readme != null) {
            File message_dump_dir_readme = new File(this.dump_dir, "readme.txt");
            if (!message_dump_dir_readme.exists()) {
                BufferedWriter w = new BufferedWriter(new FileWriter(message_dump_dir_readme));
                w.write(readme);
                w.close();
            }
        }
        this.json_log = new RandomAccessFile(getCurrentDump(dump_dir_own, this.dump_file_prefix), "rw");
    }
    
    private static File getCurrentDump(File path, String prefix) {
        SimpleDateFormat formatYearMonth = new SimpleDateFormat("yyyyMM", Locale.US);
        formatYearMonth.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentDatePart = formatYearMonth.format(new Date());
        
        // if there is already a dump, use it
        String[] existingDumps = path.list();
        if (existingDumps != null) for (String d: existingDumps) {
            if (d.startsWith(prefix + currentDatePart) && d.endsWith(".txt")) {
                return new File(path, d);
            }
            
            // in case the file is a dump file but ends with '.txt', we compress it here on-the-fly
            if (d.startsWith(prefix) && d.endsWith(".txt")) {
                final File source = new File(path, d);
                final File dest = new File(path, d + ".gz");
                new Thread() {
                    public void run() {
                        byte[] buffer = new byte[2^20];
                        try {
                            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(dest), 65536){{def.setLevel(Deflater.BEST_COMPRESSION);}};
                            FileInputStream in = new FileInputStream(source);
                            int l; while ((l = in.read(buffer)) > 0) out.write(buffer, 0, l);
                            in.close(); out.finish(); out.close();
                            if (dest.exists()) source.delete();
                       } catch (IOException e) {}
                    }
                }.start();
            }
        }
        // create a new one, use a random number. The random is used to make it possible to join many different dumps from different locations without renaming them
        String random = (Long.toString(Math.abs(new Random(System.currentTimeMillis()).nextLong())) + "00000000").substring(0, 8);
        return new File(path, prefix + currentDatePart + "_" + random + ".txt");
    }

    public void write(Map<String, Object> map) throws IOException {
        String line = new ObjectMapper().writer().writeValueAsString(map);
        synchronized (this.json_log) {
            this.json_log.seek(this.json_log.length()); // go to end of file
            this.json_log.write(UTF8.getBytes(line));
            this.json_log.writeByte('\n');
        }
    }
    public void write(Map<String, Object> map, char opkey) throws IOException {
        String line = new ObjectMapper().writer().writeValueAsString(map);
        byte[] lineb = UTF8.getBytes(line);
        synchronized (this.json_log) {
            this.json_log.seek(this.json_log.length()); // go to end of file
            this.json_log.write('{');
            this.json_log.write('\"'); this.json_log.write(OPERATION_KEY); this.json_log.write('\"'); this.json_log.write(':'); this.json_log.write('\"'); this.json_log.write(opkey); this.json_log.write('\"'); this.json_log.write(',');
            this.json_log.write(lineb, 1, lineb.length - 1);
            this.json_log.writeByte('\n');
        }
    }
    
    public void close() {
        try {this.json_log.close();} catch (IOException e) {}
    }
    
    public Collection<File> getOwnDumps() {
        return getDumps(this.dump_dir_own, this.dump_file_prefix, null);
    }
    
    public Collection<File> getImportDumps() {
        return getDumps(this.dump_dir_import, this.dump_file_prefix, null);
    }
    
    public Collection<File> getImportedDumps() {
        return getDumps(this.dump_dir_imported, this.dump_file_prefix, null);
    }
    
    private static Collection<File> getDumps(final File path, final String prefix, final String suffix) {
        String[] list = path.list();
        TreeSet<File> dumps = new TreeSet<File>(); // sort the names with a tree set
        for (String s: list) {
            if ((prefix == null || s.startsWith(prefix)) &&
                (suffix == null || s.endsWith(suffix))) dumps.add(new File(path, s));
        }
        return dumps;
    }

    private boolean shiftProcessedDump(String dumpName) {
        File f = new File(this.dump_dir_import, dumpName);
        if (!f.exists()) return false;
        File g = new File(this.dump_dir_imported, dumpName);
        if (g.exists()) g.delete();
        return f.renameTo(g);
    }

    public void shiftProcessedDumps() {
        for (File f: this.getImportDumps()) shiftProcessedDump(f.getName());
    }

    public ConcurrentReader getOwnDumpReader(int concurrency) {
        Collection<File> dumps = this.getOwnDumps();
        return dumps == null || dumps.size() == 0 ? null : new ConcurrentReader(dumps, concurrency);
    }
    
    public ConcurrentReader getImportDumpReader(int concurrency) {
        Collection<File> dumps = this.getImportDumps();
        return dumps == null || dumps.size() == 0 ? null : new ConcurrentReader(dumps, concurrency);
    }
    
    public final static Map<String, Object> POISON_JSON_MAP = new HashMap<>();
    public static class ConcurrentReader extends Thread {

        private ArrayBlockingQueue<Map<String, Object>> jsonline;
        private Collection<File> dumpFiles;
        private int concurrency;

        private ConcurrentReader(File dumpFile, int concurrency) {
            this.jsonline = new ArrayBlockingQueue<>(1000);
            this.dumpFiles = new ArrayList<>();
            this.dumpFiles.add(dumpFile);
            this.concurrency = concurrency;
        }
        
        private ConcurrentReader(Collection<File> dumpFiles, int concurrency) {
            this.jsonline = new ArrayBlockingQueue<>(1000);
            this.dumpFiles = dumpFiles;
            this.concurrency = concurrency;
        }
        
        public Map<String, Object> take() throws InterruptedException {
            return this.jsonline.take();
        }
        
        public void run() {
            for (File dumpFile: this.dumpFiles) {
                DAO.log("reading dump " + dumpFile.toString());
                try {
                    InputStream is = new FileInputStream(dumpFile);
                    String line;
                    BufferedReader br = null;
                    try {
                        if (dumpFile.getName().endsWith(".gz")) is = new GZIPInputStream(is);
                        br = new BufferedReader(new InputStreamReader(is, UTF8.charset));
                        while((line = br.readLine()) != null) {
                            try {
                                Map<String, Object> json = DAO.jsonMapper.readValue(line, DAO.jsonTypeRef);
                                if (json == null) continue;
                                this.jsonline.put(json);
                            } catch (Throwable e) {
                                Log.getLog().warn("cannot parse line \"" + line + "\"", e);
                            }
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
            }
            for (int i = 0; i < this.concurrency; i++) {
                try {this.jsonline.put(POISON_JSON_MAP);} catch (InterruptedException e) {}
            }
        }
    }
    
}
