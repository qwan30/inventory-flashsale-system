package com.codex.flashsale.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codex.flashsale.common.time.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-03-15T00:00:00Z"));

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = now::get;
        outboxService = new OutboxService(
                repository,
                new ObjectMapper(),
                kafkaTemplate,
                timeProvider,
                "inventory-flashsale.events",
                50,
                Duration.ofSeconds(10),
                3,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void shouldRetryFailedPublishThenMarkPublished() {
        OutboxEvent event = new OutboxEvent("evt-1", "reservation", "res-1", "inventory.reservation.created", "{\"ok\":true}");
        Instant retryAt = now.get().plusSeconds(10);

        when(repository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event), List.of(event));
        when(repository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
                eq(OutboxStatus.FAILED),
                eq(retryAt),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(failedFuture(new RuntimeException("broker down")))
                .thenReturn(successfulFuture());

        int failedPublishCount = outboxService.publishPendingEvents();

        assertThat(failedPublishCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(retryAt);

        now.set(retryAt);

        int retriedCount = outboxService.retryFailedEvents();
        int publishedCount = outboxService.publishPendingEvents();

        assertThat(retriedCount).isEqualTo(1);
        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getNextAttemptAt()).isNull();
        assertThat(event.getPublishedAt()).isEqualTo(retryAt);
    }

    private CompletableFuture<SendResult<String, String>> failedFuture(RuntimeException exception) {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }

    private CompletableFuture<SendResult<String, String>> successfulFuture() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }
}
