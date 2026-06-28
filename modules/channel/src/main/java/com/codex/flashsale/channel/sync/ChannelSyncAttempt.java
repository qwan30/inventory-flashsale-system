package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "channel_sync_attempt")
public class ChannelSyncAttempt extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "outbox_event_id", nullable = false)
    private String outboxEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Column(name = "sku")
    private String sku;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "available_qty")
    private Integer availableQty;

    @Column(name = "reserved_qty")
    private Integer reservedQty;

    @Column(name = "sold_qty")
    private Integer soldQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChannelSyncStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type")
    private ChannelSyncFailureType failureType;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "synced_at")
    private Instant syncedAt;

    protected ChannelSyncAttempt() {
    }

    public ChannelSyncAttempt(
            String id,
            String outboxEventId,
            SalesChannel channel,
            String sku,
            String eventType,
            String payload,
            Integer availableQty,
            Integer reservedQty,
            Integer soldQty
    ) {
        this.id = id;
        this.outboxEventId = outboxEventId;
        this.channel = channel;
        this.sku = sku;
        this.eventType = eventType;
        this.payload = payload;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.soldQty = soldQty;
        this.status = ChannelSyncStatus.PENDING;
        this.attempts = 0;
    }

    public void markSynced(Instant syncedAt) {
        this.status = ChannelSyncStatus.SYNCED;
        this.failureType = null;
        this.lastError = null;
        this.nextAttemptAt = null;
        this.syncedAt = syncedAt;
        this.attempts += 1;
    }

    public void markFailed(
            ChannelSyncFailureType failureType,
            String errorMessage,
            Instant failedAt,
            Duration retryDelay,
            int maxAttempts
    ) {
        this.status = ChannelSyncStatus.FAILED;
        this.failureType = failureType;
        this.attempts += 1;
        this.lastError = truncateErrorMessage(errorMessage);
        if (failureType == ChannelSyncFailureType.PERMANENT || attempts >= maxAttempts) {
            this.nextAttemptAt = null;
            return;
        }
        this.nextAttemptAt = failedAt.plus(retryDelay.multipliedBy(attempts));
    }

    public void resetForRetry() {
        this.status = ChannelSyncStatus.PENDING;
        this.failureType = null;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown channel sync failure";
        }
        return errorMessage.length() <= 512 ? errorMessage : errorMessage.substring(0, 512);
    }

    public ChannelSyncCommand toCommand() {
        return new ChannelSyncCommand(
                outboxEventId,
                channel,
                eventType,
                sku,
                availableQty,
                reservedQty,
                soldQty,
                getCreatedAt(),
                payload
        );
    }

    public String getId() {
        return id;
    }

    public String getOutboxEventId() {
        return outboxEventId;
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public String getSku() {
        return sku;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Integer getAvailableQty() {
        return availableQty;
    }

    public Integer getReservedQty() {
        return reservedQty;
    }

    public Integer getSoldQty() {
        return soldQty;
    }

    public ChannelSyncStatus getStatus() {
        return status;
    }

    public ChannelSyncFailureType getFailureType() {
        return failureType;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
