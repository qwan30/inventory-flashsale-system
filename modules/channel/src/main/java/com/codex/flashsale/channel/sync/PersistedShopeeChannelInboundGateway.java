package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "mock", matchIfMissing = true)
public class PersistedShopeeChannelInboundGateway extends PersistedChannelInboundAdapterSupport {

    public PersistedShopeeChannelInboundGateway(ChannelInventorySnapshotRepository repository) {
        super(repository, SalesChannel.SHOPEE);
    }
}
