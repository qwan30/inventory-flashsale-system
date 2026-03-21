package com.codex.flashsale.connector.tiktok;

public record TikTokRemoteStockView(
        String sku,
        int availableQty,
        int reservedQty
) {
}
