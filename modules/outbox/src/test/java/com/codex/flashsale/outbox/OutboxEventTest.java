package com.codex.flashsale.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void shouldMarkPublished() {
        OutboxEvent event = new OutboxEvent("evt-1", "reservation", "res-1", "inventory.reservation.created", "{}");

        event.markPublished(Instant.parse("2026-03-14T16:00:00Z"));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getAttempts()).isEqualTo(1);
    }
}
