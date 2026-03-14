package com.codex.flashsale.channel.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReconciliationRunRepository extends JpaRepository<InventoryReconciliationRun, String> {
}
