package org.loklak.api.server.push;

public class PushReport {
    private int recordCount, newCount, knownCount, errorCount;

    public PushReport() {
        recordCount = newCount = knownCount = errorCount = 0;
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

    public void incrementErrorCount() {
        this.errorCount++;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public PushReport combine(PushReport that) {
        PushReport newReport = new PushReport();
        newReport.newCount = this.getNewCount() + that.getNewCount();
        newReport.knownCount = this.getKnownCount() + that.getKnownCount();
        newReport.errorCount = this.getErrorCount() + that.getErrorCount();
        return newReport;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void incrementRecordCount() {
        this.recordCount++;
    }
}
