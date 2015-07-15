package org.loklak.api.server.helper;

public class PushReport {
    private int newCount, knownCount;

    public PushReport() {
        newCount = 0;
        knownCount = 0;
    }

    public int getNewCount() {
        return newCount;
    }

    public void incrementNewCount() {
        this.newCount++;
    }

    public int getKnownCount() {
        return knownCount;
    }

    public void incrementKnownCount() {
        this.knownCount++;
    }

    public PushReport combine(PushReport that) {
        PushReport newReport = new PushReport();
        newReport.newCount = this.getNewCount() + that.getNewCount();
        newReport.knownCount = this.getKnownCount() + that.getKnownCount();
        return newReport;
    }
}
