package com.codex.flashsale.application;

import java.time.Instant;

public record ChannelReplaySummary(
        String action,
        String resourceId,
        String outcome,
        Instant createdAt,
        String details
) {
}
