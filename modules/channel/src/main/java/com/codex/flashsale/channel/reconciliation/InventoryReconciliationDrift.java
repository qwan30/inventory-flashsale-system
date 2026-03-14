package com.codex.flashsale.channel.reconciliation;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_reconciliation_drift")
public class InventoryReconciliationDrift extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "central_available_qty", nullable = false)
    private int centralAvailableQty;

    @Column(name = "central_reserved_qty", nullable = false)
    private int centralReservedQty;

    @Column(name = "central_sold_qty", nullable = false)
    private int centralSoldQty;

    @Column(name = "observed_available_qty", nullable = false)
    private int observedAvailableQty;

    @Column(name = "observed_reserved_qty", nullable = false)
    private int observedReservedQty;

    @Column(name = "observed_sold_qty", nullable = false)
    private int observedSoldQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationDriftStatus status;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected InventoryReconciliationDrift() {
    }

    public InventoryReconciliationDrift(
            String id,
            String runId,
            SalesChannel channel,
            String sku,
            int centralAvailableQty,
            int centralReservedQty,
            int centralSoldQty,
            int observedAvailableQty,
            int observedReservedQty,
            int observedSoldQty
    ) {
        this.id = id;
        this.runId = runId;
        this.channel = channel;
        this.sku = sku;
        this.centralAvailableQty = centralAvailableQty;
        this.centralReservedQty = centralReservedQty;
        this.centralSoldQty = centralSoldQty;
        this.observedAvailableQty = observedAvailableQty;
        this.observedReservedQty = observedReservedQty;
        this.observedSoldQty = observedSoldQty;
        this.status = ReconciliationDriftStatus.OPEN;
    }

    public void resolve(String resolutionNote, Instant resolvedAt) {
        this.status = ReconciliationDriftStatus.RESOLVED;
        this.resolutionNote = resolutionNote;
        this.resolvedAt = resolvedAt;
    }

    public void refresh(
            String runId,
            int centralAvailableQty,
            int centralReservedQty,
            int centralSoldQty,
            int observedAvailableQty,
            int observedReservedQty,
            int observedSoldQty
    ) {
        this.runId = runId;
        this.centralAvailableQty = centralAvailableQty;
        this.centralReservedQty = centralReservedQty;
        this.centralSoldQty = centralSoldQty;
        this.observedAvailableQty = observedAvailableQty;
        this.observedReservedQty = observedReservedQty;
        this.observedSoldQty = observedSoldQty;
        this.status = ReconciliationDriftStatus.OPEN;
        this.resolutionNote = null;
        this.resolvedAt = null;
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public String getSku() {
        return sku;
    }

    public int getCentralAvailableQty() {
        return centralAvailableQty;
    }

    public int getCentralReservedQty() {
        return centralReservedQty;
    }

    public int getCentralSoldQty() {
        return centralSoldQty;
    }

    public int getObservedAvailableQty() {
        return observedAvailableQty;
    }

    public int getObservedReservedQty() {
        return observedReservedQty;
    }

    public int getObservedSoldQty() {
        return observedSoldQty;
    }

    public ReconciliationDriftStatus getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
