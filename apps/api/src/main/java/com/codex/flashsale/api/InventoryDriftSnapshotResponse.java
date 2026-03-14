package com.codex.flashsale.api;

public record InventoryDriftSnapshotResponse(
        int availableQty,
        int reservedQty,
        int soldQty
) {
}
