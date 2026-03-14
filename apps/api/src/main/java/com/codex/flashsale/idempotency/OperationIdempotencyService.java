package com.codex.flashsale.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OperationIdempotencyService {

    private final OperationIdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public OperationIdempotencyService(
            OperationIdempotencyRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public boolean hasRecord(
            OperationIdempotencyType operationType,
            String resourceId,
            String operationValue
    ) {
        return repository.findByOperationTypeAndResourceIdAndOperationValue(operationType, resourceId, operationValue).isPresent();
    }

    public <T> Optional<T> findRecordedResponse(
            OperationIdempotencyType operationType,
            String resourceId,
            String idempotencyKey,
            Class<T> responseType
    ) {
        return repository.findByOperationTypeAndResourceIdAndIdempotencyKey(operationType, resourceId, idempotencyKey)
                .map(record -> deserialize(record.getResponsePayload(), responseType));
    }

    public void record(
            OperationIdempotencyType operationType,
            String resourceId,
            String operationValue,
            String idempotencyKey,
            Object response
    ) {
        repository.saveAndFlush(new OperationIdempotencyRecord(
                UUID.randomUUID().toString(),
                operationType,
                resourceId,
                operationValue,
                idempotencyKey,
                serialize(response)
        ));
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize idempotent response", exception);
        }
    }

    private <T> T deserialize(String payload, Class<T> responseType) {
        try {
            return objectMapper.readValue(payload, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize idempotent response", exception);
        }
    }
}
