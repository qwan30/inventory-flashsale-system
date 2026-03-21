package com.codex.flashsale.api;

import java.time.Instant;

public record ChannelSyncFailureDetailResponse(
        String attemptId,
        String failureType,
        String error,
        int attempts,
        Instant lastUpdatedAt
) {
}
