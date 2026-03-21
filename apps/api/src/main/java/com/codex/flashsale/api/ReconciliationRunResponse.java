package com.codex.flashsale.api;

import java.time.Instant;

public record ReconciliationRunResponse(
        String runId,
        String triggerType,
        String status,
        int scannedSkuCount,
        int scannedSnapshotCount,
        int openDriftCount,
        String failureMessage,
        Instant createdAt,
        Instant completedAt
) {
}
