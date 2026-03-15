package com.codex.flashsale.connector.shopee;

import java.util.Optional;

public interface ShopeeChannelClient {

    Optional<ShopeeListingView> findListingBySku(String sku);

    ShopeeRemoteStockView updateSellerStock(ShopeeListingReference reference, int sellerStock);
}
