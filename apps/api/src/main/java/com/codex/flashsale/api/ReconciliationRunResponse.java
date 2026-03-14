package com.codex.flashsale.api;

import java.time.Instant;

public record ReconciliationRunResponse(
        String runId,
        int scannedSkuCount,
        int scannedSnapshotCount,
        int openDriftCount,
        Instant completedAt
) {
}
