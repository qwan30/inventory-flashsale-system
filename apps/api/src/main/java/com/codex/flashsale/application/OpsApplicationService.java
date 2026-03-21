package com.codex.flashsale.application;

import com.codex.flashsale.api.ChannelHealthDetailResponse;
import com.codex.flashsale.api.ChannelHealthIngressReceiptResponse;
import com.codex.flashsale.api.ChannelHealthReplayResponse;
import com.codex.flashsale.api.ChannelSyncFailureDetailResponse;
import com.codex.flashsale.api.InventoryDriftSnapshotResponse;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxBacklogResponse;
import com.codex.flashsale.api.OutboxEventSummaryResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.ingress.TikTokIngressReceiptRepository;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationDrift;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationRun;
import com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshotView;
import com.codex.flashsale.channel.sync.ChannelSyncAttemptRepository;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.channel.sync.ChannelSyncStatus;
import com.codex.flashsale.config.ApplicationProperties;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryService;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxStatus;
import com.codex.flashsale.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpsApplicationService {

    private static final int DEFAULT_OUTBOX_LIMIT = 50;
    private static final int MAX_OUTBOX_LIMIT = 200;
    private static final int DEFAULT_RECONCILIATION_RUN_LIMIT = 20;
    private static final int MAX_RECONCILIATION_RUN_LIMIT = 100;

    private final OutboxService outboxService;
    private final InventoryService inventoryService;
    private final ChannelSyncService channelSyncService;
    private final ChannelReconciliationService channelReconciliationService;
    private final OpsAlertService opsAlertService;
    private final ApplicationProperties applicationProperties;
    private final ChannelSyncAttemptRepository channelSyncAttemptRepository;
    private final TikTokIngressReceiptRepository tikTokIngressReceiptRepository;
    private final ChannelReplaySummaryProvider channelReplaySummaryProvider;
    private final Counter reconciliationRunSuccessCounter;
    private final Counter reconciliationRunFailureCounter;
    private final Timer reconciliationRunDuration;

    public OpsApplicationService(
            OutboxService outboxService,
            InventoryService inventoryService,
            ChannelSyncService channelSyncService,
            ChannelReconciliationService channelReconciliationService,
            OpsAlertService opsAlertService,
            ApplicationProperties applicationProperties,
            ChannelSyncAttemptRepository channelSyncAttemptRepository,
            TikTokIngressReceiptRepository tikTokIngressReceiptRepository,
            ChannelReplaySummaryProvider channelReplaySummaryProvider,
            MeterRegistry meterRegistry
    ) {
        this.outboxService = outboxService;
        this.inventoryService = inventoryService;
        this.channelSyncService = channelSyncService;
        this.channelReconciliationService = channelReconciliationService;
        this.opsAlertService = opsAlertService;
        this.applicationProperties = applicationProperties;
        this.channelSyncAttemptRepository = channelSyncAttemptRepository;
        this.tikTokIngressReceiptRepository = tikTokIngressReceiptRepository;
        this.channelReplaySummaryProvider = channelReplaySummaryProvider;
        this.reconciliationRunSuccessCounter = meterRegistry.counter("reconciliation.run.success");
        this.reconciliationRunFailureCounter = meterRegistry.counter("reconciliation.run.failure");
        this.reconciliationRunDuration = meterRegistry.timer("reconciliation.run.duration");
    }

    public OutboxBacklogResponse getOutboxBacklog() {
        return new OutboxBacklogResponse(
                outboxService.countPendingBacklog(),
                outboxService.countFailedBacklog(),
                outboxService.countRetryableFailedBacklog()
        );
    }

    public OutboxRetryResponse retryOutboxEvent(String eventId) {
        OutboxEvent event = outboxService.retryEvent(eventId);
        return new OutboxRetryResponse(
                event.getId(),
                event.getStatus(),
                event.getAttempts(),
                event.getNextAttemptAt(),
                event.getLastError()
        );
    }

    public List<OutboxEventSummaryResponse> listOutboxEvents(OutboxStatus status, Integer limit) {
        int effectiveLimit = clampLimit(limit, DEFAULT_OUTBOX_LIMIT, MAX_OUTBOX_LIMIT);
        return outboxService.listEventsByStatus(status, effectiveLimit).stream()
                .map(this::toOutboxEventSummaryResponse)
                .toList();
    }

    public ReconciliationRunResponse runReconciliation() {
        return runReconciliation(ReconciliationTriggerType.MANUAL);
    }

    public ReconciliationRunResponse runScheduledReconciliation() {
        return runReconciliation(ReconciliationTriggerType.SCHEDULED);
    }

    public List<OpsAlertResponse> getAlerts() {
        return opsAlertService.getAlerts();
    }

    public List<ReconciliationRunResponse> listReconciliationRuns(Integer limit) {
        int effectiveLimit = clampLimit(limit, DEFAULT_RECONCILIATION_RUN_LIMIT, MAX_RECONCILIATION_RUN_LIMIT);
        return channelReconciliationService.listRecentRuns(effectiveLimit).stream()
                .map(this::toReconciliationRunResponse)
                .toList();
    }

    public List<ChannelHealthSummary> listChannelHealthSummaries() {
        Instant lastReconciliationAt = channelReconciliationService.findLatestRun()
                .map(InventoryReconciliationRun::getCreatedAt)
                .orElse(null);
        return List.of(SalesChannel.SHOPEE, SalesChannel.TIKTOK_SHOP).stream()
                .map(channel -> toChannelHealthSummary(channel, lastReconciliationAt))
                .toList();
    }

    public ChannelHealthDetailResponse getChannelHealthDetail(SalesChannel channel) {
        Instant lastReconciliationAt = channelReconciliationService.findLatestRun()
                .map(InventoryReconciliationRun::getCreatedAt)
                .orElse(null);
        ChannelHealthSummary summary = toChannelHealthSummary(channel, lastReconciliationAt);
        long syncBacklogCount = channelSyncService.countBacklogForChannel(channel);
        long staleSnapshotCount = channelSyncService.countStaleSnapshotsForChannel(
                channel,
                applicationProperties.getAlerts().getChannelSnapshotStaleness()
        );
        long openDriftCount = channelReconciliationService.countOpenDrifts(channel);
        ChannelSyncFailureDetailResponse latestFailure = latestChannelSyncFailure(channel);
        ReconciliationRunResponse latestRun = channelReconciliationService.findLatestRun()
                .map(this::toReconciliationRunResponse)
                .orElse(null);

        return new ChannelHealthDetailResponse(
                summary.channel().name(),
                summary.status().name(),
                summary.connectorMode(),
                summary.configValid(),
                syncBacklogCount,
                staleSnapshotCount,
                openDriftCount,
                latestFailure,
                summary.latestIngressReceipt() == null ? null : toIngressResponse(summary.latestIngressReceipt()),
                summary.latestReplay() == null ? null : toReplayResponse(summary.latestReplay()),
                latestRun
        );
    }

    private ChannelSyncFailureDetailResponse latestChannelSyncFailure(SalesChannel channel) {
        return channelSyncAttemptRepository.findTopByChannelAndStatusOrderByUpdatedAtDesc(channel, ChannelSyncStatus.FAILED)
                .map(attempt -> new ChannelSyncFailureDetailResponse(
                        attempt.getId(),
                        attempt.getFailureType() == null ? null : attempt.getFailureType().name(),
                        attempt.getLastError(),
                        attempt.getAttempts(),
                        attempt.getUpdatedAt()
                ))
                .orElse(null);
    }

    private ChannelHealthIngressReceiptResponse toIngressResponse(ChannelIngressReceiptSummary summary) {
        return new ChannelHealthIngressReceiptResponse(
                summary.type(),
                summary.externalReceiptId(),
                summary.outcome(),
                summary.processedAt()
        );
    }

    private ChannelHealthReplayResponse toReplayResponse(ChannelReplaySummary replay) {
        return new ChannelHealthReplayResponse(
                replay.action(),
                replay.resourceId(),
                replay.outcome(),
                replay.createdAt(),
                replay.details()
        );
    }

    private ReconciliationRunResponse runReconciliation(ReconciliationTriggerType triggerType) {
        Timer.Sample sample = Timer.start();
        InventoryReconciliationRun run = channelReconciliationService.startRun(triggerType);
        List<InventoryItem> inventoryItems = inventoryService.findAllInventoryItems();
        try {
            int scannedSnapshots = 0;
            int openDrifts = 0;

            for (InventoryItem inventoryItem : inventoryItems) {
                for (SalesChannel channel : SalesChannel.values()) {
                    ChannelInventorySnapshotView snapshot = channelSyncService.fetchSnapshot(channel, inventoryItem.getSku()).orElse(null);
                    if (snapshot == null) {
                        continue;
                    }
                    scannedSnapshots += 1;
                    if (isDrift(inventoryItem, snapshot)) {
                        channelReconciliationService.recordDrift(
                                run.getId(),
                                channel,
                                inventoryItem.getSku(),
                                inventoryItem.getAvailableQty(),
                                inventoryItem.getReservedQty(),
                                inventoryItem.getSoldQty(),
                                snapshot.availableQty(),
                                snapshot.reservedQty(),
                                observedSoldQty(inventoryItem, snapshot)
                        );
                        openDrifts += 1;
                    }
                }
            }

            InventoryReconciliationRun completedRun = channelReconciliationService.completeRun(
                    run.getId(),
                    inventoryItems.size(),
                    scannedSnapshots,
                    openDrifts
            );
            reconciliationRunSuccessCounter.increment();
            return toReconciliationRunResponse(completedRun);
        } catch (RuntimeException exception) {
            InventoryReconciliationRun failedRun = channelReconciliationService.failRun(run.getId(), exception.getMessage());
            reconciliationRunFailureCounter.increment();
            throw exception;
        } finally {
            sample.stop(reconciliationRunDuration);
        }
    }

    public List<ReconciliationDriftResponse> listOpenReconciliationDrifts() {
        return channelReconciliationService.listOpenDrifts()
                .stream()
                .map(this::toReconciliationDriftResponse)
                .toList();
    }

    public ReconciliationDriftResponse resolveReconciliationDrift(String driftId, String resolutionNote) {
        return toReconciliationDriftResponse(channelReconciliationService.resolveDrift(driftId, resolutionNote));
    }

    private ReconciliationRunResponse toReconciliationRunResponse(InventoryReconciliationRun run) {
        return new ReconciliationRunResponse(
                run.getId(),
                run.getTriggerType().name(),
                run.getStatus().name(),
                run.getScannedSkuCount(),
                run.getScannedSnapshotCount(),
                run.getOpenDriftCount(),
                run.getFailureMessage(),
                run.getCreatedAt(),
                run.getCompletedAt()
        );
    }

    private OutboxEventSummaryResponse toOutboxEventSummaryResponse(OutboxEvent event) {
        return new OutboxEventSummaryResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getEventVersion(),
                event.getStatus(),
                event.getAttempts(),
                event.getLastError(),
                event.getNextAttemptAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private int clampLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, maxLimit);
    }

    private boolean isDrift(InventoryItem inventoryItem, ChannelInventorySnapshotView snapshot) {
        boolean stockDrift = inventoryItem.getAvailableQty() != snapshot.availableQty()
                || inventoryItem.getReservedQty() != snapshot.reservedQty();
        if (stockDrift) {
            return true;
        }
        if (!snapshot.soldQtyComparable()) {
            return false;
        }
        return inventoryItem.getSoldQty() != snapshot.soldQty();
    }

    private int observedSoldQty(InventoryItem inventoryItem, ChannelInventorySnapshotView snapshot) {
        if (!snapshot.soldQtyComparable()) {
            // Shopee live stock APIs do not expose sold quantity.
            return inventoryItem.getSoldQty();
        }
        return snapshot.soldQty();
    }

    private ReconciliationDriftResponse toReconciliationDriftResponse(InventoryReconciliationDrift drift) {
        return new ReconciliationDriftResponse(
                drift.getId(),
                drift.getRunId(),
                drift.getChannel(),
                drift.getSku(),
                new InventoryDriftSnapshotResponse(
                        drift.getCentralAvailableQty(),
                        drift.getCentralReservedQty(),
                        drift.getCentralSoldQty()
                ),
                new InventoryDriftSnapshotResponse(
                        drift.getObservedAvailableQty(),
                        drift.getObservedReservedQty(),
                        drift.getObservedSoldQty()
                ),
                drift.getStatus().name(),
                drift.getResolutionNote(),
                drift.getResolvedAt()
        );
    }

    private ChannelHealthSummary toChannelHealthSummary(SalesChannel channel, Instant lastReconciliationAt) {
        ConnectorConfig connectorConfig = connectorConfig(channel);
        long syncBacklogCount = channelSyncService.countBacklogForChannel(channel);
        long staleSnapshotCount = channelSyncService.countStaleSnapshotsForChannel(
                channel,
                applicationProperties.getAlerts().getChannelSnapshotStaleness()
        );
        long openDriftCount = channelReconciliationService.countOpenDrifts(channel);

        ChannelHealthStatus status = deriveHealthStatus(
                connectorConfig.configValid(),
                syncBacklogCount,
                staleSnapshotCount,
                openDriftCount
        );

        ChannelIngressReceiptSummary latestIngressReceipt = latestIngressReceipt(channel).orElse(null);
        ChannelReplaySummary latestReplay = channelReplaySummaryProvider.findLatest(channel).orElse(null);

        return new ChannelHealthSummary(
                channel,
                status,
                connectorConfig.mode(),
                connectorConfig.configValid(),
                syncBacklogCount,
                staleSnapshotCount,
                openDriftCount,
                lastReconciliationAt,
                latestIngressReceipt,
                latestReplay
        );
    }

    private Optional<ChannelIngressReceiptSummary> latestIngressReceipt(SalesChannel channel) {
        if (channel != SalesChannel.TIKTOK_SHOP) {
            return Optional.empty();
        }
        return tikTokIngressReceiptRepository.findTopByChannelOrderByProcessedAtDesc(channel)
                .map(receipt -> new ChannelIngressReceiptSummary(
                        receipt.getReceiptType(),
                        receipt.getExternalReceiptId(),
                        receipt.getOutcome(),
                        receipt.getProcessedAt()
                ));
    }

    private ChannelHealthStatus deriveHealthStatus(
            boolean configValid,
            long syncBacklogCount,
            long staleSnapshotCount,
            long openDriftCount
    ) {
        if (!configValid) {
            return ChannelHealthStatus.UNAVAILABLE;
        }
        if (syncBacklogCount > 0 || staleSnapshotCount > 0 || openDriftCount > 0) {
            return ChannelHealthStatus.DEGRADED;
        }
        return ChannelHealthStatus.HEALTHY;
    }

    private ConnectorConfig connectorConfig(SalesChannel channel) {
        return switch (channel) {
            case SHOPEE -> shopeeConnectorConfig();
            case TIKTOK_SHOP -> tikTokConnectorConfig();
            default -> new ConnectorConfig("UNKNOWN", false);
        };
    }

    private ConnectorConfig shopeeConnectorConfig() {
        ApplicationProperties.Shopee shopee = applicationProperties.getChannel().getShopee();
        String mode = normalizedMode(shopee.getMode());
        if ("MOCK".equals(mode)) {
            return new ConnectorConfig(mode, true);
        }
        if ("REAL".equals(mode)) {
            boolean valid = nonBlank(shopee.getBaseUrl())
                    && shopee.getPartnerId() != null
                    && nonBlank(shopee.getPartnerKey())
                    && shopee.getShopId() != null
                    && nonBlank(shopee.getAccessToken())
                    && positive(shopee.getConnectTimeout())
                    && positive(shopee.getReadTimeout());
            return new ConnectorConfig(mode, valid);
        }
        return new ConnectorConfig(mode, false);
    }

    private ConnectorConfig tikTokConnectorConfig() {
        ApplicationProperties.TikTok tikTok = applicationProperties.getChannel().getTikTok();
        String mode = normalizedMode(tikTok.getMode());
        if ("MOCK".equals(mode)) {
            return new ConnectorConfig(mode, true);
        }
        if ("REAL".equals(mode)) {
            boolean valid = nonBlank(tikTok.getBaseUrl())
                    && nonBlank(tikTok.getAppKey())
                    && nonBlank(tikTok.getAppSecret())
                    && nonBlank(tikTok.getShopCipher())
                    && nonBlank(tikTok.getAccessToken())
                    && nonBlank(tikTok.getIngressSecret())
                    && positive(tikTok.getConnectTimeout())
                    && positive(tikTok.getReadTimeout());
            return new ConnectorConfig(mode, valid);
        }
        return new ConnectorConfig(mode, false);
    }

    private String normalizedMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return "UNKNOWN";
        }
        return rawMode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private boolean positive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    private record ConnectorConfig(String mode, boolean configValid) {
    }
}
