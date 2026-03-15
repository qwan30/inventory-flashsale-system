package com.codex.flashsale.connector.shopee;

public record ShopeeListingView(
        ShopeeListingReference reference,
        ShopeeRemoteStockView stock
) {
}
