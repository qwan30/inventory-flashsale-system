package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelSyncAttemptRepository extends JpaRepository<ChannelSyncAttempt, String> {

    List<ChannelSyncAttempt> findByStatusOrderByCreatedAtAsc(ChannelSyncStatus status, Pageable pageable);

    List<ChannelSyncAttempt> findByStatusAndFailureTypeAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
            ChannelSyncStatus status,
            ChannelSyncFailureType failureType,
            Instant nextAttemptAt,
            Pageable pageable
    );

    long countByStatus(ChannelSyncStatus status);

    boolean existsByOutboxEventIdAndChannel(String outboxEventId, SalesChannel channel);

    long countByStatusAndFailureTypeAndNextAttemptAtLessThanEqual(
            ChannelSyncStatus status,
            ChannelSyncFailureType failureType,
            Instant nextAttemptAt
    );
}
