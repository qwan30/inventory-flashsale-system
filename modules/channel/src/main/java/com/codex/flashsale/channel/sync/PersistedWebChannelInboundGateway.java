package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import org.springframework.stereotype.Component;

@Component
public class PersistedWebChannelInboundGateway extends PersistedChannelInboundAdapterSupport {

    public PersistedWebChannelInboundGateway(ChannelInventorySnapshotRepository repository) {
        super(repository, SalesChannel.WEB);
    }
}
