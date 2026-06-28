package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import java.util.Optional;

abstract class PersistedChannelInboundAdapterSupport implements ChannelInboundPort {

    private final ChannelInventorySnapshotRepository repository;
    private final SalesChannel channel;

    protected PersistedChannelInboundAdapterSupport(
            ChannelInventorySnapshotRepository repository,
            SalesChannel channel
    ) {
        this.repository = repository;
        this.channel = channel;
    }

    @Override
    public SalesChannel channel() {
        return channel;
    }

    @Override
    public Optional<ChannelInventorySnapshotView> fetchInventorySnapshot(String sku) {
        return repository.findByChannelAndSku(channel, sku).map(ChannelInventorySnapshot::toView);
    }
}
