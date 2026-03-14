package com.codex.flashsale.api;

public record InventoryResponse(
        String sku,
        int availableQty,
        int reservedQty,
        int soldQty,
        long version
) {
}

