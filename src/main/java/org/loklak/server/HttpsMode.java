package org.loklak.server;

import java.util.Objects;

public enum HttpsMode {
	OFF(0),
    ON(1),
    REDIRECT(2),
    ONLY(3);

    private final Integer mode;

    HttpsMode(int mode) {
        this.mode = mode;
    }

    public boolean equals(HttpsMode other) {return Objects.equals(this.mode, other.mode);}
    public boolean isSmallerThan(HttpsMode other) {return this.mode < other.mode;}
    public boolean isSmallerOrEqualTo(HttpsMode other) {return this.mode <= other.mode;}
    public boolean isGreaterThan(HttpsMode other) {return this.mode > other.mode;}
    public boolean isGreaterOrEqualTo(HttpsMode other) {return this.mode >= other.mode;}
}
