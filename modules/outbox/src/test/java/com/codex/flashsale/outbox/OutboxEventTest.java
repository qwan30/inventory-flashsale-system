package com.codex.flashsale.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void shouldMarkPublished() {
        OutboxEvent event = new OutboxEvent("evt-1", "reservation", "res-1", "inventory.reservation.created", "{}");

        event.markPublished(Instant.parse("2026-03-14T16:00:00Z"));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldScheduleRetryBeforeAttemptLimit() {
        OutboxEvent event = new OutboxEvent("evt-1", "reservation", "res-1", "inventory.reservation.created", "{}");
        Instant failedAt = Instant.parse("2026-03-14T16:00:00Z");

        event.markFailed("broker unavailable", failedAt, Duration.ofSeconds(10), 3);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(failedAt.plusSeconds(10));
    }

    @Test
    void shouldStopRetrySchedulingAtAttemptLimit() {
        OutboxEvent event = new OutboxEvent("evt-1", "reservation", "res-1", "inventory.reservation.created", "{}");
        Instant failedAt = Instant.parse("2026-03-14T16:00:00Z");

        event.markFailed("first failure", failedAt, Duration.ofSeconds(10), 2);
        event.resetForRetry();
        event.markFailed("second failure", failedAt.plusSeconds(10), Duration.ofSeconds(10), 2);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getNextAttemptAt()).isNull();
    }
}
