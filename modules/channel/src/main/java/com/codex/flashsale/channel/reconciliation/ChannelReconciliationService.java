package com.codex.flashsale.channel.reconciliation;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.NotFoundException;
import com.codex.flashsale.common.time.TimeProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChannelReconciliationService {

    private final InventoryReconciliationRunRepository runRepository;
    private final InventoryReconciliationDriftRepository driftRepository;
    private final TimeProvider timeProvider;

    public ChannelReconciliationService(
            InventoryReconciliationRunRepository runRepository,
            InventoryReconciliationDriftRepository driftRepository,
            TimeProvider timeProvider
    ) {
        this.runRepository = runRepository;
        this.driftRepository = driftRepository;
        this.timeProvider = timeProvider;
    }

    public InventoryReconciliationRun startRun(ReconciliationTriggerType triggerType) {
        return runRepository.saveAndFlush(new InventoryReconciliationRun(UUID.randomUUID().toString(), triggerType));
    }

    public InventoryReconciliationRun completeRun(
            String runId,
            int scannedSkuCount,
            int scannedSnapshotCount,
            int openDriftCount
    ) {
        InventoryReconciliationRun run = getRequiredRun(runId);
        run.complete(scannedSkuCount, scannedSnapshotCount, openDriftCount, timeProvider.now());
        return runRepository.saveAndFlush(run);
    }

    public InventoryReconciliationRun failRun(String runId, String failureMessage) {
        InventoryReconciliationRun run = getRequiredRun(runId);
        run.fail(failureMessage, timeProvider.now());
        return runRepository.saveAndFlush(run);
    }

    public InventoryReconciliationDrift recordDrift(
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
        InventoryReconciliationDrift drift = driftRepository.findByStatusAndChannelAndSku(
                ReconciliationDriftStatus.OPEN,
                channel,
                sku
        ).orElseGet(() -> new InventoryReconciliationDrift(
                UUID.randomUUID().toString(),
                runId,
                channel,
                sku,
                centralAvailableQty,
                centralReservedQty,
                centralSoldQty,
                observedAvailableQty,
                observedReservedQty,
                observedSoldQty
        ));
        drift.refresh(
                runId,
                centralAvailableQty,
                centralReservedQty,
                centralSoldQty,
                observedAvailableQty,
                observedReservedQty,
                observedSoldQty
        );
        return driftRepository.saveAndFlush(drift);
    }

    public List<InventoryReconciliationDrift> listOpenDrifts() {
        return driftRepository.findByStatusOrderByCreatedAtDesc(ReconciliationDriftStatus.OPEN);
    }

    public long countOpenDrifts() {
        return driftRepository.countByStatus(ReconciliationDriftStatus.OPEN);
    }

    public java.util.Optional<InventoryReconciliationRun> findLatestRun(ReconciliationTriggerType triggerType) {
        return runRepository.findTopByTriggerTypeOrderByCreatedAtDesc(triggerType);
    }

    public InventoryReconciliationDrift resolveDrift(String driftId, String resolutionNote) {
        InventoryReconciliationDrift drift = getRequiredDrift(driftId);
        drift.resolve(resolutionNote, timeProvider.now());
        return driftRepository.saveAndFlush(drift);
    }

    private InventoryReconciliationRun getRequiredRun(String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("RECONCILIATION_RUN_NOT_FOUND", "Reconciliation run not found: " + runId));
    }

    private InventoryReconciliationDrift getRequiredDrift(String driftId) {
        return driftRepository.findById(driftId)
                .orElseThrow(() -> new NotFoundException("RECONCILIATION_DRIFT_NOT_FOUND", "Reconciliation drift not found: " + driftId));
    }
}
