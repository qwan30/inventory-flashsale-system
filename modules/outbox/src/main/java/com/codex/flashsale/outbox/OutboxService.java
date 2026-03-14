package com.codex.flashsale.outbox;

import com.codex.flashsale.common.time.TimeProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TimeProvider timeProvider;
    private final String topic;
    private final int publishBatchSize;

    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            TimeProvider timeProvider,
            @Value("${app.kafka.topic:inventory-flashsale.events}") String topic,
            @Value("${app.outbox.publish-batch-size:50}") int publishBatchSize
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.timeProvider = timeProvider;
        this.topic = topic;
        this.publishBatchSize = publishBatchSize;
    }

    public OutboxEvent record(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            return repository.save(new OutboxEvent(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    serializedPayload
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }

    public int publishPendingEvents() {
        List<OutboxEvent> pendingEvents = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, publishBatchSize)
        );
        pendingEvents.forEach(this::publishEvent);
        return pendingEvents.size();
    }

    public OutboxEvent save(OutboxEvent event) {
        return repository.saveAndFlush(event);
    }

    private void publishEvent(OutboxEvent event) {
        try {
            OutboxEnvelope envelope = new OutboxEnvelope(
                    event.getId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getCreatedAt(),
                    event.getPayload()
            );
            String message = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topic, event.getAggregateId(), message).get(5, TimeUnit.SECONDS);
            event.markPublished(timeProvider.now());
        } catch (Exception exception) {
            event.markFailed(exception.getMessage());
        }
        repository.saveAndFlush(event);
    }
}

