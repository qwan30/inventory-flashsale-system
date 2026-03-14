package com.codex.flashsale.api;

import com.codex.flashsale.outbox.OutboxStatus;
import java.time.Instant;

public record OutboxRetryResponse(
        String eventId,
        OutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError
) {
}
