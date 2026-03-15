package com.codex.flashsale.alerts;

import com.codex.flashsale.api.OpsAlertStatus;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "alert_delivery_state")
public class AlertDeliveryState extends AuditTimestamps {

    @Id
    @Column(name = "alert_code", nullable = false, length = 64)
    private String alertCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_observed_status", nullable = false, length = 32)
    private OpsAlertStatus lastObservedStatus;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_notified_status", length = 32)
    private OpsAlertStatus lastNotifiedStatus;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    protected AlertDeliveryState() {
    }

    public AlertDeliveryState(String alertCode) {
        this.alertCode = alertCode;
    }

    public void observe(OpsAlertStatus status, Instant observedAt) {
        this.lastObservedStatus = status;
        this.lastObservedAt = observedAt;
    }

    public void markSent(OpsAlertStatus status, Instant sentAt) {
        this.lastNotifiedStatus = status;
        this.lastSentAt = sentAt;
        this.lastError = null;
        this.consecutiveFailures = 0;
    }

    public void markFailed(String error) {
        this.lastError = truncate(error);
        this.consecutiveFailures += 1;
    }

    public String getAlertCode() {
        return alertCode;
    }

    public OpsAlertStatus getLastObservedStatus() {
        return lastObservedStatus;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public OpsAlertStatus getLastNotifiedStatus() {
        return lastNotifiedStatus;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 512) {
            return value;
        }
        return value.substring(0, 512);
    }
}
