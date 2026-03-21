package com.codex.flashsale.api;

import com.codex.flashsale.outbox.OutboxStatus;
import java.time.Instant;

public record OutboxEventSummaryResponse(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        int eventVersion,
        OutboxStatus status,
        int attempts,
        String lastError,
        Instant nextAttemptAt,
        Instant createdAt,
        Instant updatedAt
) {
}
