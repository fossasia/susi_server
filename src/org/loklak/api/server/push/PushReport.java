package org.loklak.api.server.push;

import org.loklak.data.ImportProfileEntry;

import java.util.ArrayList;
import java.util.List;

public class PushReport {
    private int recordCount, newCount, knownCount, errorCount;
    private ImportProfileEntry importProfile;

    private List<String> knownMessageIds;
    public PushReport() {
        recordCount = newCount = knownCount = errorCount = 0;
        importProfile = null;
        knownMessageIds = new ArrayList<>();
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

    public void incrementKnownCount(String id) {
        knownMessageIds.add(id);
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

    public List<String> getKnownMessageIds() {
        return knownMessageIds;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void incrementRecordCount() {
        this.recordCount++;
    }

    public void combine(PushReport that) {
        this.recordCount += that.recordCount;
        this.newCount += that.newCount;
        this.knownCount += that.knownCount;
        this.errorCount += that.errorCount;
        // prioritize `that` import profile
        if (that.importProfile != null)
            this.importProfile = that.importProfile;
        if (that.knownMessageIds != null)
            this.knownMessageIds.addAll(that.knownMessageIds);
    }
}
