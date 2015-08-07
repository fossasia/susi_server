package org.loklak.api.server.push;

import org.loklak.data.ImportProfileEntry;

public class PushReport {
    private int recordCount, newCount, knownCount, errorCount;
    private ImportProfileEntry importProfile;

    public PushReport() {
        recordCount = newCount = knownCount = errorCount = 0;
        importProfile = null;
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

    public ImportProfileEntry getImportProfile() {
        return importProfile;
    }

    public void setImportProfile(ImportProfileEntry importProfile) {
        this.importProfile = importProfile;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void incrementRecordCount() {
        this.recordCount++;
    }
}
