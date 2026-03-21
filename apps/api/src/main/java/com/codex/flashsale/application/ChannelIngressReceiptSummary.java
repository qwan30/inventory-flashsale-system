package com.codex.flashsale.application;

import java.time.Instant;

public record ChannelIngressReceiptSummary(
        String type,
        String externalReceiptId,
        String outcome,
        Instant processedAt
) {
}
