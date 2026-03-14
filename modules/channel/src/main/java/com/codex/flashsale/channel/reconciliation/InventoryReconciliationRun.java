package com.codex.flashsale.channel.reconciliation;

import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_reconciliation_run")
public class InventoryReconciliationRun extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "scanned_sku_count", nullable = false)
    private int scannedSkuCount;

    @Column(name = "scanned_snapshot_count", nullable = false)
    private int scannedSnapshotCount;

    @Column(name = "open_drift_count", nullable = false)
    private int openDriftCount;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected InventoryReconciliationRun() {
    }

    public InventoryReconciliationRun(String id) {
        this.id = id;
        this.scannedSkuCount = 0;
        this.scannedSnapshotCount = 0;
        this.openDriftCount = 0;
    }

    public void complete(int scannedSkuCount, int scannedSnapshotCount, int openDriftCount, Instant completedAt) {
        this.scannedSkuCount = scannedSkuCount;
        this.scannedSnapshotCount = scannedSnapshotCount;
        this.openDriftCount = openDriftCount;
        this.completedAt = completedAt;
    }

    public String getId() {
        return id;
    }

    public int getScannedSkuCount() {
        return scannedSkuCount;
    }

    public int getScannedSnapshotCount() {
        return scannedSnapshotCount;
    }

    public int getOpenDriftCount() {
        return openDriftCount;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
