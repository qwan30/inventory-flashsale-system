package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PersistedChannelInboundGateway implements ChannelInboundPort {

    private final ChannelInventorySnapshotRepository repository;

    public PersistedChannelInboundGateway(ChannelInventorySnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ChannelInventorySnapshotView> fetchInventorySnapshot(SalesChannel channel, String sku) {
        return repository.findByChannelAndSku(channel, sku).map(ChannelInventorySnapshot::toView);
    }
}
