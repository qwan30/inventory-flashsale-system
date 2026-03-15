package com.codex.flashsale.connector.shopee;

public record ShopeeRemoteStockView(
        String sku,
        int availableQty,
        int reservedQty
) {
}
