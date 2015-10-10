/**
 *  BufferedRandomAccessFile
 *  Copyright 2015 by Michael Peter Christen
 *  First released 30.09.2015
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

package org.loklak.tools;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

/**
 * This class is inspired by https://code.google.com/p/jmzreader/source/browse/tools/braf/trunk/src/main/java/uk/ac/ebi/pride/tools/braf/BufferedRandomAccessFile.java
 * which is in turn an optimized version of the RandomAccessFile class as described by Nick Zhang on JavaWorld.com. The article can be found at http://www.javaworld.com/javaworld/javatips/jw-javatip26.html
 * The getNextLine method was rewritten in such a way that it returns a byte[] rather than a string which was not UTF-8 - friendly in it's original version.
 * The whole class was not concurrency-safe. Synchronization has been added to ensure consistency of buffer and seek position.
 */
public class BufferedRandomAccessFile extends RandomAccessFile {
    
    private byte buffer[];
    private int  buf_end = 0;
    private int  buf_pos = 0;
    private long real_pos = 0;
    
    private final int BUF_SIZE;

    /**
     * Creates a new instance of the BufferedRandomAccessFile.
     * @param filename The path of the file to open.
     * @param mode     Specifies the mode to use ("r", "rw", etc.) See the
     *                 BufferedLineReader documentation for more information.
     * @param bufsize  The buffer size (in bytes) to use.
     * @throws IOException
     */
    public BufferedRandomAccessFile(String filename, String mode, int bufsize) throws IOException {
        super(filename, mode);
        invalidate();
        BUF_SIZE = bufsize;
        buffer = new byte[BUF_SIZE];
    }

    public BufferedRandomAccessFile(File file, String mode, int bufsize) throws IOException {
        this(file.getAbsolutePath(), mode, bufsize);
    }
    
    public BufferedRandomAccessFile(File file, String mode) throws IOException {
        this(file.getAbsolutePath(), mode, 1 << 20);
    }

    /**
     * Reads one byte form the current position
     * @return The read byte or -1 in case the end was reached.
     */
    @Override
    public synchronized final int read() throws IOException {
        if (buf_pos >= buf_end) {
            if (fillBuffer() < 0) return -1;
        }
        return buf_end == 0 ? -1 : buffer[buf_pos++];
    }

    /**
     * Reads the next BUF_SIZE bytes into the internal buffer.
     * @return
     * @throws IOException
     */
    private int fillBuffer() throws IOException {
        int n = super.read(buffer, 0, BUF_SIZE);
        if (n >= 0) {
            real_pos += n;
            buf_end = n;
            buf_pos = 0;
        }
        return n;
    }

    /**
     * Clears the local buffer.
     * @throws IOException
     */
    private void invalidate() throws IOException {
        buf_end = 0;
        buf_pos = 0;
        real_pos = super.getFilePointer();
    }

    /**
     * Reads the set number of bytes into the passed buffer.
     * @param b   The buffer to read the bytes into.
     * @param off Byte offset within the file to start reading from
     * @param len Number of bytes to read into the buffer.
     * @return Number of bytes read.
     */
    @Override
    public synchronized int read(byte b[], int off, int len) throws IOException {
        int leftover = buf_end - buf_pos;
        if (len <= leftover) {
            System.arraycopy(buffer, buf_pos, b, off, len);
            buf_pos += len;
            return len;
        }
        for (int i = 0; i < len; i++) {
            int c = this.read();
            if (c != -1)
                b[off + i] = (byte) c;
            else {
                return i;
            }
        }
        return len;
    }

    /**
     * Returns the current position of the pointer in the file.
     * @return The byte position of the pointer in the file.
     */
    @Override
    public synchronized long getFilePointer() throws IOException {
        return real_pos - buf_end + buf_pos;
    }

    /**
     * Overridden seek method always throws exception: this would not work in concurrent environments.
     * All seek operations must be encapsulated here in synchronized methods.
     */
    @Override
    public synchronized void seek(long pos) throws IOException {
        throw new UnsupportedOperationException("seek cannot be called public to avoid synchronization issues");
    }

    /**
     * Moves the internal pointer to the passed (byte) position in the file.
     * @param pos the byte position to move to.
     */
    private void seekPrivate(long pos) throws IOException {
        int n = (int) (real_pos - pos);
        if (n >= 0 && n <= buf_end) {
            buf_pos = buf_end - n;
        } else {
            super.seek(pos);
            invalidate();
        }
    }
    
    public synchronized void read(final byte[] b, final long pos) throws IOException {
        seekPrivate(pos);
        read(b, 0, b.length);
    }
    
    /**
     * add a line at the end of the file
     * @param b
     * @return the seek position where the line started
     * @throws IOException
     */
    public synchronized long appendLine(final byte[] b) throws IOException {
        long seekpos = this.length();
        this.seekPrivate(seekpos); // go to end of file
        this.write(b);
        this.writeByte((byte) '\n');
        this.invalidate(); // instead of invalidate it could be better to refresh the buffer with the latest byte[]
        return seekpos;
    }
    
    /**
     * Reading of text lines will produce index information along with the parsed text.
     * To get the exact number of bytes, we do not depend on a utf-8 - parsing string but
     * instead the line is read as byte[] to determine the exact length of the line.
     * @return a IndexedLine object with the text and the read index.
     * @throws IOException
     */
    public synchronized IndexedLine readIndexedLine() throws IOException {
        long pos = real_pos - buf_end + buf_pos;
        byte[] text = this.getNextLine();
        return text == null ? null : new IndexedLine(pos, text);
    }

    public static class IndexedLine {
        private long pos;
        private byte[] text;
        public IndexedLine(long pos, byte[] text) {
            this.pos = pos;
            this.text = text;
        }
        public long getPos() {
            return pos;
        }
        public byte[] getText() {
            return text;
        }
        public String toString() {
            return UTF8.String(this.text);
        }
    }
    
    /**
     * Returns the next line from the file. In case no data could be loaded
     * (generally as the end of the file was reached) null is returned.
     *
     * @return The next string on the file or null in case the end of the file
     *         was reached.
     */
    private final byte[] getNextLine() throws IOException {
        if (buf_end - buf_pos <= 0) {
            if (fillBuffer() < 0) return null;
        }
        int lineend = -1;  // final position of the char considering \n

        for (int i = buf_pos; i < buf_end; i++) {
            if (buffer[i] == '\n') {
                lineend = i;
                break;
            }
            // check for only '\r' as line end
            if ((i - buf_pos > 0) && buffer[i - 1] == '\r') {
                lineend = i - 1;
                break;
            }
        }
        if (lineend < 0) {
            ByteBuffer line = new ByteBuffer();
            int c;
            int lastC = 0;
            while (((c = read()) != -1) && (c != '\n') && (lastC != '\r')) {
                line.append((char) c);
                lastC = c;
            }
            if (c == -1 && line.length() == 0) {line.close(); return new byte[0];}
            byte[] b = line.getBytes();
            line.close();
            return b;
        }
        byte[] b = null;
        if (lineend > 0 && buffer[lineend] == '\n' && buffer[lineend - 1] == '\r' && lineend - buf_pos - 1 >= 0) {
            b = new byte[lineend - buf_pos - 1];
            System.arraycopy(buffer, buf_pos, b, 0, lineend - buf_pos - 1);
        } else {
            b = new byte[lineend - buf_pos];
            System.arraycopy(buffer, buf_pos, b, 0, lineend - buf_pos);
        }
        buf_pos = lineend + 1;
        return b;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test.class);
    }

    public static class Test extends TestCase {
    
        public static File getTestFile() {
            return new File("/tmp/test_" + System.currentTimeMillis());
        }
        
        public static String[] getTestLines(int count) {
            Random r = new Random(0);
            String[] lines = new String[count];
            for (int i = 0; i < count; i++) lines[i] = "{\"" + Long.toString(r.nextLong()) + "\":\"X\"}";
            return lines;
        }
        
        public static void writeLines(File f, String[] l) throws IOException {
            if (f.exists()) f.delete();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
            for (String s: l) {writer.write(s); writer.write('\n');}
            writer.close();
        }
        
        private File testFile;
        private String[] testLines;
        
        @Before
        public void setUp() throws Exception {
            this.testFile = getTestFile();
            this.testLines = getTestLines(1000000);
        }
    
        @After
        public void tearDown() throws Exception {
            this.testFile.delete();
        }
        
        public void testSimultanousWriteAndRead() throws IOException {
            if (this.testFile.exists()) this.testFile.delete();
            BufferedRandomAccessFile braf = new BufferedRandomAccessFile(this.testFile, "rw", 5000);
            for (int i = 0; i < this.testLines.length; i++) {
                long pos = braf.getFilePointer();
                braf.appendLine(UTF8.getBytes(this.testLines[i]));
                braf.seekPrivate(pos);
                byte[] b = braf.getNextLine();
                if (!ASCII.String(b).equals(this.testLines[i])) System.out.println(ASCII.String(b) + " != " + this.testLines[i]);
                assertTrue(ASCII.String(b).equals(this.testLines[i]));
            }
            braf.close();
        }
        
        public void testSequentialWriteThenRead() throws IOException {
            writeLines(this.testFile, this.testLines);
            BufferedRandomAccessFile braf = new BufferedRandomAccessFile(this.testFile, "rw", 5000);
            Map<Long, String> m = new HashMap<>();
            // test if sequential read is identical to original
            for (int i = 0; i < this.testLines.length; i++) {
                long pos = braf.getFilePointer();
                byte[] b = braf.getNextLine();
                if (!ASCII.String(b).equals(this.testLines[i])) System.out.println(ASCII.String(b) + " != " + this.testLines[i]);
                assertTrue(ASCII.String(b).equals(this.testLines[i]));
                m.put(pos, this.testLines[i]);
            }
            // test if random read is identical to original
            for (Map.Entry<Long, String> e: m.entrySet()) {
                braf.seekPrivate(e.getKey());
                byte[] b = braf.getNextLine();
                assertTrue(ASCII.String(b).equals(e.getValue()));
            }
            braf.close();
        }
        
    }
    
 }