package org.loklak.tools.storage;

/**
 * Created by devenv on 12/9/16.
 */
public enum Mode {
        COMPRESSED, // dump files are compressed but cannot be re-written. All data is cached in RAM.
        REWRITABLE // dump files are not compressed but can be re-written. Data is only indexed in RAM and retrieved from file.
}
