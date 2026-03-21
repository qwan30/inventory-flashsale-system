package com.codex.flashsale.api;

import java.time.Instant;

public record AdminTikTokIngressReplayRequest(
        String kind,
        String receiptId,
        String sku,
        Integer availableQty,
        Integer reservedQty,
        Integer soldQty,
        String orderId,
        String status,
        Instant observedAt
) {
}
