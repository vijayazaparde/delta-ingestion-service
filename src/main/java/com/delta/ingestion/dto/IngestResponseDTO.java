package com.delta.ingestion.dto;

public class IngestResponseDTO {

    private int received;
    private int inserted;
    private int skipped;
    private int failed;

    public IngestResponseDTO() {
    }

    public IngestResponseDTO(int received, int inserted, int skipped, int failed) {
        this.received = received;
        this.inserted = inserted;
        this.skipped = skipped;
        this.failed = failed;
    }

    public int getReceived() {
        return received;
    }

    public void setReceived(int received) {
        this.received = received;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }
}
