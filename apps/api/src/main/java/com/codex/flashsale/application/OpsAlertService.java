package com.codex.flashsale.application;

import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OpsAlertSeverity;
import com.codex.flashsale.api.OpsAlertStatus;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationRun;
import com.codex.flashsale.channel.reconciliation.ReconciliationRunStatus;
import com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import com.codex.flashsale.outbox.OutboxService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpsAlertService {

    private final OutboxService outboxService;
    private final ChannelSyncService channelSyncService;
    private final ChannelReconciliationService channelReconciliationService;
    private final ApplicationProperties applicationProperties;
    private final TimeProvider timeProvider;

    public OpsAlertService(
            OutboxService outboxService,
            ChannelSyncService channelSyncService,
            ChannelReconciliationService channelReconciliationService,
            ApplicationProperties applicationProperties,
            TimeProvider timeProvider,
            MeterRegistry meterRegistry
    ) {
        this.outboxService = outboxService;
        this.channelSyncService = channelSyncService;
        this.channelReconciliationService = channelReconciliationService;
        this.applicationProperties = applicationProperties;
        this.timeProvider = timeProvider;

        Gauge.builder("reconciliation.drift.open", this, value -> value.channelReconciliationService.countOpenDrifts())
                .register(meterRegistry);
        Gauge.builder("channel.snapshot.stale", this, value -> value.countStaleSnapshots())
                .register(meterRegistry);
    }

    public List<OpsAlertResponse> getAlerts() {
        Instant observedAt = timeProvider.now();
        long outboxFailedCount = outboxService.countFailedBacklog();
        long channelSyncFailedCount = channelSyncService.countFailedAttempts();
        long channelSyncRetryableCount = channelSyncService.countRetryableFailedAttempts();
        long openDriftCount = channelReconciliationService.countOpenDrifts();
        long staleSnapshotCount = countStaleSnapshots();
        InventoryReconciliationRun latestScheduledRun = channelReconciliationService.findLatestRun(ReconciliationTriggerType.SCHEDULED)
                .orElse(null);

        return List.of(
                thresholdAlert(
                        "OUTBOX_FAILED_BACKLOG",
                        OpsAlertSeverity.WARN,
                        "Failed outbox backlog breached threshold",
                        outboxFailedCount,
                        applicationProperties.getAlerts().getOutboxFailedThreshold(),
                        observedAt
                ),
                thresholdAlert(
                        "CHANNEL_SYNC_FAILED_BACKLOG",
                        OpsAlertSeverity.WARN,
                        "Failed channel sync backlog breached threshold",
                        channelSyncFailedCount,
                        applicationProperties.getAlerts().getChannelSyncFailedThreshold(),
                        observedAt
                ),
                thresholdAlert(
                        "RECONCILIATION_OPEN_DRIFTS",
                        OpsAlertSeverity.WARN,
                        "Open reconciliation drift count breached threshold",
                        openDriftCount,
                        applicationProperties.getAlerts().getReconciliationOpenDriftThreshold(),
                        observedAt
                ),
                new OpsAlertResponse(
                        "CHANNEL_SYNC_RETRYABLE_BACKLOG",
                        OpsAlertSeverity.INFO,
                        channelSyncRetryableCount > 0 ? OpsAlertStatus.ACTIVE : OpsAlertStatus.INACTIVE,
                        "Retryable channel sync backlog currently waiting for replay",
                        Long.toString(channelSyncRetryableCount),
                        "0",
                        observedAt
                ),
                new OpsAlertResponse(
                        "STALE_CHANNEL_SNAPSHOTS",
                        OpsAlertSeverity.WARN,
                        staleSnapshotCount > 0 ? OpsAlertStatus.ACTIVE : OpsAlertStatus.INACTIVE,
                        "Channel snapshots older than the configured staleness window were detected",
                        Long.toString(staleSnapshotCount),
                        applicationProperties.getAlerts().getChannelSnapshotStaleness().toString(),
                        observedAt
                ),
                latestRunAlert(latestScheduledRun, observedAt)
        );
    }

    private OpsAlertResponse thresholdAlert(
            String code,
            OpsAlertSeverity severity,
            String message,
            long currentValue,
            long threshold,
            Instant observedAt
    ) {
        return new OpsAlertResponse(
                code,
                severity,
                currentValue >= threshold ? OpsAlertStatus.ACTIVE : OpsAlertStatus.INACTIVE,
                message,
                Long.toString(currentValue),
                Long.toString(threshold),
                observedAt
        );
    }

    private OpsAlertResponse latestRunAlert(InventoryReconciliationRun latestScheduledRun, Instant observedAt) {
        if (latestScheduledRun == null) {
            return new OpsAlertResponse(
                    "RECONCILIATION_RUN_FAILURE",
                    OpsAlertSeverity.CRITICAL,
                    OpsAlertStatus.INACTIVE,
                    "No scheduled reconciliation run has failed",
                    "NONE",
                    ReconciliationRunStatus.COMPLETED.name(),
                    observedAt
            );
        }

        boolean active = latestScheduledRun.getStatus() == ReconciliationRunStatus.FAILED;
        String message = active
                ? "Latest scheduled reconciliation run failed: " + latestScheduledRun.getFailureMessage()
                : "Latest scheduled reconciliation run completed successfully";
        Instant runObservedAt = latestScheduledRun.getCompletedAt() != null ? latestScheduledRun.getCompletedAt() : observedAt;
        return new OpsAlertResponse(
                "RECONCILIATION_RUN_FAILURE",
                OpsAlertSeverity.CRITICAL,
                active ? OpsAlertStatus.ACTIVE : OpsAlertStatus.INACTIVE,
                message,
                latestScheduledRun.getStatus().name(),
                ReconciliationRunStatus.COMPLETED.name(),
                runObservedAt
        );
    }

    private long countStaleSnapshots() {
        return channelSyncService.countStaleSnapshots(applicationProperties.getAlerts().getChannelSnapshotStaleness());
    }
}
