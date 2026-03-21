package com.codex.flashsale.api;

import java.time.Instant;

public record ChannelHealthResponse(
        String channel,
        String status,
        String connectorMode,
        boolean configValid,
        long syncBacklogCount,
        long staleSnapshotCount,
        long openDriftCount,
        Instant lastReconciliationAt,
        ChannelHealthIngressReceiptResponse latestIngressReceipt,
        ChannelHealthReplayResponse latestReplay
) {
}
