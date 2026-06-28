package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    long countByChannelAndStatusIn(SalesChannel channel, Collection<ChannelSyncStatus> statuses);

    boolean existsByOutboxEventIdAndChannel(String outboxEventId, SalesChannel channel);

    long countByStatusAndFailureTypeAndNextAttemptAtLessThanEqual(
            ChannelSyncStatus status,
            ChannelSyncFailureType failureType,
            Instant nextAttemptAt
    );

    Optional<ChannelSyncAttempt> findTopByChannelAndStatusOrderByUpdatedAtDesc(SalesChannel channel, ChannelSyncStatus status);
}
