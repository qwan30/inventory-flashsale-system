package com.codex.flashsale.api;

public record OutboxBacklogResponse(
        long pendingCount,
        long failedCount,
        long retryableFailedCount
) {
}
