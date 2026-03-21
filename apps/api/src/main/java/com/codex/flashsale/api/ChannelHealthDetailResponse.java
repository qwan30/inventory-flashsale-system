package com.codex.flashsale.api;

public record ChannelHealthDetailResponse(
        String channel,
        String status,
        String connectorMode,
        boolean configValid,
        long syncBacklogCount,
        long staleSnapshotCount,
        long openDriftCount,
        ChannelSyncFailureDetailResponse latestSyncFailure,
        ChannelHealthIngressReceiptResponse latestIngressReceipt,
        ChannelHealthReplayResponse latestReplay,
        ReconciliationRunResponse recentReconciliationRun
) {
}
