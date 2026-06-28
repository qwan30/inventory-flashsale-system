package com.codex.flashsale.api;

import com.codex.flashsale.common.domain.SalesChannel;
import java.time.Instant;

public record ReconciliationDriftResponse(
        String driftId,
        String runId,
        SalesChannel channel,
        String sku,
        InventoryDriftSnapshotResponse centralInventory,
        InventoryDriftSnapshotResponse observedInventory,
        String status,
        String resolutionNote,
        Instant resolvedAt
) {
}
