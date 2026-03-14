package com.codex.flashsale.channel.reconciliation;

import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_reconciliation_run")
public class InventoryReconciliationRun extends AuditTimestamps {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ReconciliationTriggerType triggerType;

    @Column(name = "scanned_sku_count", nullable = false)
    private int scannedSkuCount;

    @Column(name = "scanned_snapshot_count", nullable = false)
    private int scannedSnapshotCount;

    @Column(name = "open_drift_count", nullable = false)
    private int openDriftCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationRunStatus status;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected InventoryReconciliationRun() {
    }

    public InventoryReconciliationRun(String id, ReconciliationTriggerType triggerType) {
        this.id = id;
        this.triggerType = triggerType;
        this.scannedSkuCount = 0;
        this.scannedSnapshotCount = 0;
        this.openDriftCount = 0;
        this.status = ReconciliationRunStatus.RUNNING;
    }

    public void complete(int scannedSkuCount, int scannedSnapshotCount, int openDriftCount, Instant completedAt) {
        this.scannedSkuCount = scannedSkuCount;
        this.scannedSnapshotCount = scannedSnapshotCount;
        this.openDriftCount = openDriftCount;
        this.status = ReconciliationRunStatus.COMPLETED;
        this.failureMessage = null;
        this.completedAt = completedAt;
    }

    public void fail(String failureMessage, Instant completedAt) {
        this.status = ReconciliationRunStatus.FAILED;
        this.failureMessage = truncateFailureMessage(failureMessage);
        this.completedAt = completedAt;
    }

    private String truncateFailureMessage(String failureMessage) {
        if (failureMessage == null || failureMessage.isBlank()) {
            return "Unknown reconciliation failure";
        }
        return failureMessage.length() <= 512 ? failureMessage : failureMessage.substring(0, 512);
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

    public ReconciliationTriggerType getTriggerType() {
        return triggerType;
    }

    public ReconciliationRunStatus getStatus() {
        return status;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
