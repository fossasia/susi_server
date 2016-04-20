/**
 *  JsonRepository
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.tools.Compression;
import org.loklak.tools.UTF8;


public class JsonRepository {

    // special keys which can be added to the data set to track changes
    public final static byte[] OPERATION_KEY = "$P".getBytes();
    public final static byte[] MOD_DATE_KEY  = "$D".getBytes();
    public final static byte[] REFERRER_KEY  = "$U".getBytes();
    public final static byte[][] META_KEYS = new byte[][]{OPERATION_KEY, MOD_DATE_KEY, REFERRER_KEY};
    
    public static final Mode COMPRESSED_MODE = Mode.COMPRESSED;
    public static final Mode REWRITABLE_MODE = Mode.REWRITABLE;


    private final static SimpleDateFormat dateFomatMonthly = new SimpleDateFormat("yyyyMM", Locale.US);
    private final static SimpleDateFormat dateFomatDaily = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final static SimpleDateFormat dateFomatHourly = new SimpleDateFormat("yyyyMMddHH", Locale.US);
    private final static SimpleDateFormat dateFomatMinutely = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
    
    static {
        dateFomatMonthly.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateFomatDaily.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateFomatHourly.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    public static enum Mode {
        COMPRESSED, // dump files are compressed but cannot be re-written. All data is cached in RAM.
        REWRITABLE; // dump files are not compressed but can be re-written. Data is only indexed in RAM and retrieved from file.
    }
   
    final File dump_dir, dump_dir_own, dump_dir_import, dump_dir_imported, dump_dir_buffer;
    final String dump_file_prefix;
    final JsonRandomAccessFile json_log;
    final Mode mode;
    final int concurrency;
    final Map<String, JsonRandomAccessFile> buffers;
    
    public JsonRepository(File dump_dir, String dump_file_prefix, String readme, final Mode mode, final boolean dailyDump, final int concurrency) throws IOException {
        this.dump_dir = dump_dir;
        this.dump_file_prefix = dump_file_prefix;
        this.dump_dir_own = new File(this.dump_dir, "own");
        this.dump_dir_import = new File(this.dump_dir, "import");
        this.dump_dir_imported = new File(this.dump_dir, "imported");
        this.dump_dir_buffer = new File(this.dump_dir, "buffer");
        this.dump_dir.mkdirs();
        this.dump_dir_own.mkdirs();
        this.dump_dir_import.mkdirs();
        this.dump_dir_imported.mkdirs();
        this.dump_dir_buffer.mkdirs();
        this.mode = mode;
        this.concurrency = concurrency;
        if (readme != null) {
            File message_dump_dir_readme = new File(this.dump_dir, "readme.txt");
            if (!message_dump_dir_readme.exists()) {
                BufferedWriter w = new BufferedWriter(new FileWriter(message_dump_dir_readme));
                w.write(readme);
                w.close();
            }
        }
        this.json_log = new JsonRandomAccessFile(getCurrentDump(dump_dir_own, this.dump_file_prefix, mode, dailyDump), this.concurrency);
        this.buffers = new TreeMap<>();
    }
    
    public File getDumpDir() {
        return this.dump_dir;
    }
    
    public Mode getMode() {
        return this.mode;
    }

    private static String dateSuffix(final boolean dailyDump, final Date d) {
        return (dailyDump ? dateFomatDaily : dateFomatMonthly).format(d);
    }
    
    private static File getCurrentDump(File path, String prefix, final Mode mode, final boolean dailyDump) {
        String currentDatePart = dateSuffix(dailyDump, new Date());
        
        // if there is already a dump, use it
        String[] existingDumps = path.list();
        if (existingDumps != null) {
            for (String d: existingDumps) {
                // first check if the file is the current file: we never compress that to enable a write to the end of the file
                if (d.startsWith(prefix + currentDatePart) && d.endsWith(".txt")) {
                    continue;
                }
                
                // according to the write mode, we either compress or uncompress the file on-the-fly
                if (mode == COMPRESSED_MODE) {
                    // all files should be compressed to enable small file sizes, but contents must be in RAM after reading
                    if (d.startsWith(prefix) && d.endsWith(".txt")) {
                        final File source = new File(path, d);
                        final File dest = new File(path, d + ".gz");
                        if (dest.exists()) dest.delete();
                        new Thread() {
                            public void run() {
                                try {
                                    DAO.log("starting gzip of " + source);
                                    Compression.gzip(source, dest, true);
                                    DAO.log("finished gzip of " + source);
                                } catch (IOException e) {
                                    DAO.log("gzip of " + source + " failed: " + e.getMessage());
                                }
                            }
                        }.start();
                    }
                } else {
                    // all files should be uncompressed to enable random-access mode
                    if (d.startsWith(prefix) && d.endsWith(".gz")) {
                        final File source = new File(path, d);
                        final File dest = new File(path, d.substring(0,  d.length() - 3));
                        if (dest.exists()) dest.delete();
                        try {
                            Compression.gunzip(source, dest, true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            // mark the file as invalid
                            if (dest.exists()) dest.delete();
                            final File invalid = new File(path, d + ".invalid");
                            source.renameTo(invalid);
                        }
                    }
                }
            }
            // the latest file with the current date is the required one (and it should not be compressed)
            for (String d: existingDumps) {
                if (d.startsWith(prefix + currentDatePart) && d.endsWith(".txt")) {
                    return new File(path, d);
                }
            }
        }
        // no current file was found: create a new one, use a random number.
        // The random is used to make it possible to join many different dumps from different instances
        // without renaming them
        String random = (Long.toString(Math.abs(new Random(System.currentTimeMillis()).nextLong())) + "00000000").substring(0, 8);
        return new File(path, prefix + currentDatePart + "_" + random + ".txt");
    }

    public JsonFactory write(Map<String, Object> map) throws IOException {
        String line = new JSONObject(map).toString(); // new ObjectMapper().writer().writeValueAsString(map);
        JsonFactory jf = null;
        byte[] b = UTF8.getBytes(line);
        long seekpos = this.json_log.appendLine(b);
        jf = this.json_log.getJsonFactory(seekpos, b.length);
        return jf;
    }
    
    public JsonFactory write(Map<String, Object> map, char opkey) throws IOException {
        String line = new JSONObject(map).toString(); // new ObjectMapper().writer().writeValueAsString(map);
        JsonFactory jf = null;
        StringBuilder sb = new StringBuilder();
        sb.append('{').append('\"').append(UTF8.String(OPERATION_KEY)).append('\"').append(':').append('\"').append(opkey).append('\"').append(',');
        sb.append(line.substring(1));
        byte[] b = UTF8.getBytes(sb.toString());
        long seekpos = this.json_log.appendLine(b);
        jf = this.json_log.getJsonFactory(seekpos, b.length);
        return jf;
    }
    
    public void buffer(Date created_at, Map<String, Object> map) throws IOException {
        // compute a buffer name from the created_at date
        String bufferName = dateSuffix(true, created_at);
        
        synchronized (this.buffers) {
            this.buffers.get(bufferName);
        }
    }
    
    public List<Map<String, Object>> getBufferShard() {
        return null;
    }
    
    public int getBufferShardCount() {
        return 0;
    }
    
    public void close() {
        try {this.json_log.close();} catch (IOException e) {}
    }
    
    public SortedSet<File> getOwnDumps(int count) {
        return getDumps(this.dump_dir_own, this.dump_file_prefix, null, count);
    }
    
    public SortedSet<File> getImportDumps(int count) {
        return getDumps(this.dump_dir_import, this.dump_file_prefix, null, count);
    }
    
    public SortedSet<File> getImportedDumps(int count) {
        return getDumps(this.dump_dir_imported, this.dump_file_prefix, null, count);
    }
    
    private static SortedSet<File> tailSet(SortedSet<File> set, int count) {
        if (count >= set.size()) return set;
        TreeSet<File> t = new TreeSet<File>();
        Iterator<File> fi = set.iterator();
        for (int i = 0; i < set.size() - count; i++) fi.next();
        while (fi.hasNext()) t.add(fi.next());
        return t;
    }
    
    private static SortedSet<File> getDumps(final File path, final String prefix, final String suffix, int count) {
        String[] list = path.list();
        TreeSet<File> dumps = new TreeSet<File>(); // sort the names with a tree set
        for (String s: list) {
            if ((prefix == null || s.startsWith(prefix)) &&
                (suffix == null || s.endsWith(suffix))) dumps.add(new File(path, s));
        }
        return tailSet(dumps, count);
    }

    /**
     * move a file from the import directory to the imported directory.
     * @param dumpName only the name, not the full path. The file must be in the import file path
     * @return true if the file was shifted successfully, false if file did not exist or cannot be moved
     */
    public boolean shiftProcessedDump(String dumpName) {
        File f = new File(this.dump_dir_import, dumpName);
        if (!f.exists()) return false;
        File g = new File(this.dump_dir_imported, dumpName);
        if (g.exists()) g.delete();
        return f.renameTo(g);
    }
    
    /**
     * create a concurrent dump reader for the given file. The reader is either a JsonStreamReader if the
     * dump file is gzipped or a JsonRandomAccessFile if the file is a plain txt file. Both reader types
     * must be started as concurrent process which this method does on it's own. The reader process dies
     * automatically when the file is read completely. When the reader thread dies, it pushed several 
     * JsonReader.POISON_JSON_MAP objects to the reading queue, according to the concurrency defined with the
     * initializer of this class.
     * @param dump file
     * @return a concurrent JsonReader with started Thread wrapper
     * @throws IOException
     */
    public JsonReader getDumpReader(File dump) throws IOException {
        if (dump == null || !dump.exists()) throw new IOException("dump file " + dump + " does not exist");
        if (dump.getName().endsWith(".gz")) {
            assert this.mode == COMPRESSED_MODE;
            JsonStreamReader r = new JsonStreamReader(new GZIPInputStream(new FileInputStream(dump)), dump.getAbsolutePath(), this.concurrency);
            final Thread readerThread = new Thread(r);
            readerThread.start();
            return r;
        }
        if (dump.getName().endsWith(".txt")) {
            // no assert for the mode here because both mode would be valid
            final JsonRandomAccessFile r = new JsonRandomAccessFile(dump, this.concurrency);
            final Thread readerThread = new Thread(r);
            readerThread.start();
            return r;
        }
        throw new IOException("wrong file extension: must be txt or gz");
    }
    
}
