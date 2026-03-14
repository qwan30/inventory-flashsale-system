package com.codex.flashsale.outbox;

import java.time.Instant;

public record OutboxEnvelope(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        Instant createdAt,
        String payload
) {
}

