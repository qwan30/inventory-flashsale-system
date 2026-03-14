package com.codex.flashsale.application;

import com.codex.flashsale.api.InventoryDriftSnapshotResponse;
import com.codex.flashsale.api.OutboxBacklogResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationDrift;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationRun;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshotView;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryService;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpsApplicationService {

    private final OutboxService outboxService;
    private final InventoryService inventoryService;
    private final ChannelSyncService channelSyncService;
    private final ChannelReconciliationService channelReconciliationService;

    public OpsApplicationService(
            OutboxService outboxService,
            InventoryService inventoryService,
            ChannelSyncService channelSyncService,
            ChannelReconciliationService channelReconciliationService
    ) {
        this.outboxService = outboxService;
        this.inventoryService = inventoryService;
        this.channelSyncService = channelSyncService;
        this.channelReconciliationService = channelReconciliationService;
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
        InventoryReconciliationRun run = channelReconciliationService.startRun();
        List<InventoryItem> inventoryItems = inventoryService.findAllInventoryItems();
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
                            snapshot.soldQty()
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
        return new ReconciliationRunResponse(
                completedRun.getId(),
                completedRun.getScannedSkuCount(),
                completedRun.getScannedSnapshotCount(),
                completedRun.getOpenDriftCount(),
                completedRun.getCompletedAt()
        );
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

    private boolean isDrift(InventoryItem inventoryItem, ChannelInventorySnapshotView snapshot) {
        return inventoryItem.getAvailableQty() != snapshot.availableQty()
                || inventoryItem.getReservedQty() != snapshot.reservedQty()
                || inventoryItem.getSoldQty() != snapshot.soldQty();
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
