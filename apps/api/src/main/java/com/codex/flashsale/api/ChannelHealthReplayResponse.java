package com.codex.flashsale.api;

import java.time.Instant;

public record ChannelHealthReplayResponse(
        String action,
        String resourceId,
        String outcome,
        Instant createdAt,
        String details
) {
}
