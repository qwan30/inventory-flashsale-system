package com.codex.flashsale.connector.tiktok;

import java.util.Optional;

public interface TikTokChannelClient {

    Optional<TikTokListingView> findListingBySku(String sku);

    TikTokRemoteStockView updateAvailableStock(TikTokListingReference reference, int availableQty);
}
