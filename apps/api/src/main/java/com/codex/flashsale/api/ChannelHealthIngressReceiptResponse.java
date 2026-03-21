package com.codex.flashsale.api;

import java.time.Instant;

public record ChannelHealthIngressReceiptResponse(
        String type,
        String externalReceiptId,
        String outcome,
        Instant processedAt
) {
}
