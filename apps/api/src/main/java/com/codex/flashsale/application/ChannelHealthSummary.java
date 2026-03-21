package com.codex.flashsale.application;

import com.codex.flashsale.channel.SalesChannel;
import java.time.Instant;

public record ChannelHealthSummary(
        SalesChannel channel,
        ChannelHealthStatus status,
        String connectorMode,
        boolean configValid,
        long syncBacklogCount,
        long staleSnapshotCount,
        long openDriftCount,
        Instant lastReconciliationAt,
        ChannelIngressReceiptSummary latestIngressReceipt,
        ChannelReplaySummary latestReplay
) {
}
