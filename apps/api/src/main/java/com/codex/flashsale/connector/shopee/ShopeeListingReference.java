package com.codex.flashsale.connector.shopee;

public record ShopeeListingReference(
        String sku,
        long itemId,
        Long modelId,
        String locationId
) {
}
