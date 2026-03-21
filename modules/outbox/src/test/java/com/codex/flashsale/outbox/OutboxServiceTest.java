package com.codex.flashsale.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codex.flashsale.common.time.TimeProvider;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.mockito.ArgumentCaptor;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = now::get;
        outboxService = new OutboxService(
                repository,
                objectMapper,
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
    void shouldRetryFailedPublishThenMarkPublished() throws Exception {
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

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, org.mockito.Mockito.times(2))
                .send(eq("inventory-flashsale.events"), eq("res-1"), messageCaptor.capture());
        JsonNode envelope = objectMapper.readTree(messageCaptor.getAllValues().getLast());
        assertThat(envelope.path("eventType").asText()).isEqualTo("inventory.reservation.created");
        assertThat(envelope.path("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.path("occurredAt").asText()).isNotBlank();
        assertThat(envelope.path("payload").path("ok").asBoolean()).isTrue();
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
