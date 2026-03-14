package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.time.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ChannelSyncService {

    private final ChannelSyncAttemptRepository attemptRepository;
    private final ChannelInventorySnapshotRepository snapshotRepository;
    private final ChannelInboundPort channelInboundPort;
    private final Map<SalesChannel, ChannelSyncPort> syncPorts;
    private final TimeProvider timeProvider;
    private final int syncBatchSize;
    private final Duration retryDelay;
    private final int maxAttempts;

    public ChannelSyncService(
            ChannelSyncAttemptRepository attemptRepository,
            ChannelInventorySnapshotRepository snapshotRepository,
            ChannelInboundPort channelInboundPort,
            List<ChannelSyncPort> syncPorts,
            TimeProvider timeProvider,
            @Value("${app.channel.sync-batch-size:50}") int syncBatchSize,
            @Value("${app.channel.retry-delay:15s}") Duration retryDelay,
            @Value("${app.channel.max-attempts:3}") int maxAttempts
    ) {
        this.attemptRepository = attemptRepository;
        this.snapshotRepository = snapshotRepository;
        this.channelInboundPort = channelInboundPort;
        this.syncPorts = new EnumMap<>(SalesChannel.class);
        syncPorts.forEach(syncPort -> this.syncPorts.put(syncPort.channel(), syncPort));
        this.timeProvider = timeProvider;
        this.syncBatchSize = syncBatchSize;
        this.retryDelay = retryDelay;
        this.maxAttempts = maxAttempts;
    }

    public void scheduleSync(
            String outboxEventId,
            String eventType,
            String payload,
            Collection<SalesChannel> channels,
            String sku,
            Integer availableQty,
            Integer reservedQty,
            Integer soldQty
    ) {
        List<ChannelSyncAttempt> attempts = channels.stream()
                .distinct()
                .filter(channel -> !attemptRepository.existsByOutboxEventIdAndChannel(outboxEventId, channel))
                .map(channel -> new ChannelSyncAttempt(
                        UUID.randomUUID().toString(),
                        outboxEventId,
                        channel,
                        sku,
                        eventType,
                        payload,
                        availableQty,
                        reservedQty,
                        soldQty
                ))
                .toList();
        if (!attempts.isEmpty()) {
            attemptRepository.saveAllAndFlush(attempts);
        }
    }

    public int publishPendingAttempts() {
        List<ChannelSyncAttempt> attempts = attemptRepository.findByStatusOrderByCreatedAtAsc(
                ChannelSyncStatus.PENDING,
                PageRequest.of(0, syncBatchSize)
        );
        attempts.forEach(this::publishAttempt);
        return attempts.size();
    }

    public int retryFailedAttempts() {
        Instant now = timeProvider.now();
        List<ChannelSyncAttempt> retryableAttempts =
                attemptRepository.findByStatusAndFailureTypeAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
                        ChannelSyncStatus.FAILED,
                        ChannelSyncFailureType.TRANSIENT,
                        now,
                        PageRequest.of(0, syncBatchSize)
                );
        if (retryableAttempts.isEmpty()) {
            return 0;
        }
        retryableAttempts.forEach(ChannelSyncAttempt::resetForRetry);
        attemptRepository.saveAllAndFlush(retryableAttempts);
        return retryableAttempts.size();
    }

    public Optional<ChannelInventorySnapshotView> fetchSnapshot(SalesChannel channel, String sku) {
        return channelInboundPort.fetchInventorySnapshot(channel, sku);
    }

    public long countPendingAttempts() {
        return attemptRepository.countByStatus(ChannelSyncStatus.PENDING);
    }

    public long countFailedAttempts() {
        return attemptRepository.countByStatus(ChannelSyncStatus.FAILED);
    }

    private void publishAttempt(ChannelSyncAttempt attempt) {
        Instant now = timeProvider.now();
        try {
            ChannelSyncPort syncPort = syncPorts.get(attempt.getChannel());
            if (syncPort == null) {
                throw new PermanentChannelSyncException("No sync port registered for channel " + attempt.getChannel());
            }
            ChannelSyncCommand command = attempt.toCommand();
            syncPort.publish(command);
            attempt.markSynced(now);
            if (command.sku() != null && command.availableQty() != null && command.reservedQty() != null && command.soldQty() != null) {
                upsertSnapshot(command, now);
            }
        } catch (TransientChannelSyncException exception) {
            attempt.markFailed(ChannelSyncFailureType.TRANSIENT, exception.getMessage(), now, retryDelay, maxAttempts);
        } catch (PermanentChannelSyncException exception) {
            attempt.markFailed(ChannelSyncFailureType.PERMANENT, exception.getMessage(), now, retryDelay, maxAttempts);
        }
        attemptRepository.saveAndFlush(attempt);
    }

    private void upsertSnapshot(ChannelSyncCommand command, Instant syncedAt) {
        String snapshotId = ChannelInventorySnapshot.snapshotId(command.channel(), command.sku());
        ChannelInventorySnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseGet(() -> new ChannelInventorySnapshot(
                        snapshotId,
                        command.channel(),
                        command.sku(),
                        command.availableQty(),
                        command.reservedQty(),
                        command.soldQty(),
                        command.outboxEventId(),
                        syncedAt
                ));
        snapshot.apply(command, syncedAt);
        snapshotRepository.saveAndFlush(snapshot);
    }
}
