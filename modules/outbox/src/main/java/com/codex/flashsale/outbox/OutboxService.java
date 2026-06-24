package com.codex.flashsale.outbox;

import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.exception.NotFoundException;
import com.codex.flashsale.common.time.TimeProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service implementing the Transactional Outbox Pattern.
 * Writes event records to the outbox database table in the same database transaction
 * as the domain state changes. An asynchronous scheduler subsequently publishes these events
 * to Kafka to ensure at-least-once message delivery without locking business transactions.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TimeProvider timeProvider;
    private final String topic;
    private final int publishBatchSize;
    private final Duration retryDelay;
    private final int maxAttempts;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter retryScheduledCounter;
    private final Timer publishLatency;

    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            TimeProvider timeProvider,
            @Value("${app.kafka.topic:inventory-flashsale.events}") String topic,
            @Value("${app.outbox.publish-batch-size:50}") int publishBatchSize,
            @Value("${app.outbox.retry-delay:10s}") Duration retryDelay,
            @Value("${app.outbox.max-attempts:5}") int maxAttempts,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.timeProvider = timeProvider;
        this.topic = topic;
        this.publishBatchSize = publishBatchSize;
        this.retryDelay = retryDelay;
        this.maxAttempts = maxAttempts;
        this.publishSuccessCounter = meterRegistry.counter("outbox.publish.success");
        this.publishFailureCounter = meterRegistry.counter("outbox.publish.failure");
        this.retryScheduledCounter = meterRegistry.counter("outbox.retry.scheduled");
        this.publishLatency = meterRegistry.timer("outbox.publish.latency");
        Gauge.builder("outbox.backlog.pending", repository, value -> value.countByStatus(OutboxStatus.PENDING))
                .register(meterRegistry);
        Gauge.builder("outbox.backlog.failed", repository, value -> value.countByStatus(OutboxStatus.FAILED))
                .register(meterRegistry);
    }

    /**
     * Records a new outbox event using the default version.
     */
    public OutboxEvent record(String aggregateType, String aggregateId, String eventType, Object payload) {
        return record(aggregateType, aggregateId, eventType, OutboxEvent.DEFAULT_EVENT_VERSION, payload);
    }

    /**
     * Serializes the event payload and saves the outbox record to the database.
     * MUST run in the same active transaction as the primary aggregate update.
     */
    public OutboxEvent record(
            String aggregateType,
            String aggregateId,
            String eventType,
            int eventVersion,
            Object payload
    ) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            return repository.save(new OutboxEvent(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    eventVersion,
                    serializedPayload
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }

    /**
     * Publishes pending outbox events to Kafka in sequential batches.
     * Called periodically by the OutboxPublisherScheduler.
     * 
     * @return the number of published events
     */
    public int publishPendingEvents() {
        List<OutboxEvent> pendingEvents = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, publishBatchSize)
        );
        pendingEvents.forEach(this::publishEvent);
        return pendingEvents.size();
    }

    public int retryFailedEvents() {
        Instant now = timeProvider.now();
        List<OutboxEvent> retryableEvents = repository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
                OutboxStatus.FAILED,
                now,
                PageRequest.of(0, publishBatchSize)
        );
        if (retryableEvents.isEmpty()) {
            return 0;
        }
        retryableEvents.forEach(OutboxEvent::resetForRetry);
        repository.saveAllAndFlush(retryableEvents);
        retryScheduledCounter.increment(retryableEvents.size());
        return retryableEvents.size();
    }

    public OutboxEvent retryEvent(String eventId) {
        OutboxEvent event = repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("OUTBOX_EVENT_NOT_FOUND", "Outbox event not found: " + eventId));
        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            throw new ConflictException("OUTBOX_EVENT_ALREADY_PUBLISHED", "Published outbox event cannot be retried");
        }
        if (event.getStatus() == OutboxStatus.FAILED) {
            event.resetForRetry();
        }
        return repository.saveAndFlush(event);
    }

    public long countPendingBacklog() {
        return repository.countByStatus(OutboxStatus.PENDING);
    }

    public long countFailedBacklog() {
        return repository.countByStatus(OutboxStatus.FAILED);
    }

    public long countRetryableFailedBacklog() {
        return repository.countByStatusAndNextAttemptAtLessThanEqual(OutboxStatus.FAILED, timeProvider.now());
    }

    public List<OutboxEvent> listEventsByStatus(OutboxStatus status, int limit) {
        return repository.findByStatusOrderByUpdatedAtDescCreatedAtDesc(status, PageRequest.of(0, limit));
    }

    public OutboxEvent save(OutboxEvent event) {
        return repository.saveAndFlush(event);
    }

    private void publishEvent(OutboxEvent event) {
        Timer.Sample sample = Timer.start();
        try {
            OutboxEnvelope envelope = new OutboxEnvelope(
                    event.getId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getEventVersion(),
                    event.getCreatedAt(),
                    objectMapper.readTree(event.getPayload())
            );
            String message = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topic, event.getAggregateId(), message).get(5, TimeUnit.SECONDS);
            event.markPublished(timeProvider.now());
            publishSuccessCounter.increment();
        } catch (Exception exception) {
            event.markFailed(exception.getMessage(), timeProvider.now(), retryDelay, maxAttempts);
            publishFailureCounter.increment();
        } finally {
            sample.stop(publishLatency);
        }
        repository.saveAndFlush(event);
    }
}
