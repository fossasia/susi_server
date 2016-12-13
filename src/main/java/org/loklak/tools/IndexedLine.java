package org.loklak.tools;

/**
 * Created by devenv on 12/9/16.
 */
public class IndexedLine {
    private final long pos;
    private final byte[] text;
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
