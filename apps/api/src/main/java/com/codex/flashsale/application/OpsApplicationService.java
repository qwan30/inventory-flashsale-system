package com.codex.flashsale.application;

import com.codex.flashsale.api.InventoryDriftSnapshotResponse;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxBacklogResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationDrift;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationRun;
import com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshotView;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryService;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpsApplicationService {

    private final OutboxService outboxService;
    private final InventoryService inventoryService;
    private final ChannelSyncService channelSyncService;
    private final ChannelReconciliationService channelReconciliationService;
    private final OpsAlertService opsAlertService;
    private final Counter reconciliationRunSuccessCounter;
    private final Counter reconciliationRunFailureCounter;
    private final Timer reconciliationRunDuration;

    public OpsApplicationService(
            OutboxService outboxService,
            InventoryService inventoryService,
            ChannelSyncService channelSyncService,
            ChannelReconciliationService channelReconciliationService,
            OpsAlertService opsAlertService,
            MeterRegistry meterRegistry
    ) {
        this.outboxService = outboxService;
        this.inventoryService = inventoryService;
        this.channelSyncService = channelSyncService;
        this.channelReconciliationService = channelReconciliationService;
        this.opsAlertService = opsAlertService;
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

    public ReconciliationRunResponse runReconciliation() {
        return runReconciliation(ReconciliationTriggerType.MANUAL);
    }

    public ReconciliationRunResponse runScheduledReconciliation() {
        return runReconciliation(ReconciliationTriggerType.SCHEDULED);
    }

    public List<OpsAlertResponse> getAlerts() {
        return opsAlertService.getAlerts();
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
                run.getCompletedAt()
        );
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
}
