package com.codex.flashsale.channel.reconciliation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReconciliationDriftRepository extends JpaRepository<InventoryReconciliationDrift, String> {

    List<InventoryReconciliationDrift> findByStatusOrderByCreatedAtDesc(ReconciliationDriftStatus status);
}
