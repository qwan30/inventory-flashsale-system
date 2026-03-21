package com.codex.flashsale.channel.reconciliation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface InventoryReconciliationRunRepository extends JpaRepository<InventoryReconciliationRun, String> {

    long countByStatus(ReconciliationRunStatus status);

    Optional<InventoryReconciliationRun> findTopByOrderByCreatedAtDesc();

    Optional<InventoryReconciliationRun> findTopByTriggerTypeOrderByCreatedAtDesc(ReconciliationTriggerType triggerType);

    List<InventoryReconciliationRun> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
