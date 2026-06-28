package com.codex.flashsale.channel.reconciliation;

import com.codex.flashsale.common.domain.SalesChannel;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codex.flashsale.common.domain.SalesChannel;

public interface InventoryReconciliationDriftRepository extends JpaRepository<InventoryReconciliationDrift, String> {

    List<InventoryReconciliationDrift> findByStatusOrderByCreatedAtDesc(ReconciliationDriftStatus status);

    Optional<InventoryReconciliationDrift> findByStatusAndChannelAndSku(
            ReconciliationDriftStatus status,
            SalesChannel channel,
            String sku
    );

    long countByStatus(ReconciliationDriftStatus status);

    long countByStatusAndChannel(ReconciliationDriftStatus status, SalesChannel channel);
}
