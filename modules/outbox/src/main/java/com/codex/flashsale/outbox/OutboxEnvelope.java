package com.codex.flashsale.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record OutboxEnvelope(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        JsonNode payload
) {
}
