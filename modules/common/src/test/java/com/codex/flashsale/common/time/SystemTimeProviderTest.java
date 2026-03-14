package com.codex.flashsale.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemTimeProviderTest {

    @Test
    void shouldReturnClockInstant() {
        Instant now = Instant.parse("2026-03-14T15:00:00Z");
        SystemTimeProvider provider = new SystemTimeProvider(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(provider.now()).isEqualTo(now);
    }
}

