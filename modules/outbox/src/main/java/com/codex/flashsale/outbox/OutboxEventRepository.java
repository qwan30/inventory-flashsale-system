package com.codex.flashsale.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
            OutboxStatus status,
            Instant nextAttemptAt,
            Pageable pageable
    );

    long countByStatus(OutboxStatus status);

    long countByStatusAndNextAttemptAtLessThanEqual(OutboxStatus status, Instant nextAttemptAt);
}
