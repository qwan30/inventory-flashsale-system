package com.codex.flashsale.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationIdempotencyRepository extends JpaRepository<OperationIdempotencyRecord, String> {

    Optional<OperationIdempotencyRecord> findByOperationTypeAndResourceIdAndIdempotencyKey(
            OperationIdempotencyType operationType,
            String resourceId,
            String idempotencyKey
    );

    Optional<OperationIdempotencyRecord> findByOperationTypeAndResourceIdAndOperationValue(
            OperationIdempotencyType operationType,
            String resourceId,
            String operationValue
    );
}
