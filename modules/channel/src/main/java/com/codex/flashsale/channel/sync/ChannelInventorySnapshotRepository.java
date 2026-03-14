package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelInventorySnapshotRepository extends JpaRepository<ChannelInventorySnapshot, String> {

    Optional<ChannelInventorySnapshot> findByChannelAndSku(SalesChannel channel, String sku);

    long countBySyncedAtBefore(Instant syncedAt);
}
