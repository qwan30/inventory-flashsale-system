package com.codex.flashsale.channel.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventoryReconciliationRunRepository extends JpaRepository<InventoryReconciliationRun, String> {

    long countByStatus(ReconciliationRunStatus status);

    Optional<InventoryReconciliationRun> findTopByTriggerTypeOrderByCreatedAtDesc(ReconciliationTriggerType triggerType);
}
