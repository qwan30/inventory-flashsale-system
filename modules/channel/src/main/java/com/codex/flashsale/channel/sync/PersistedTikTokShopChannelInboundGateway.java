package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "mock", matchIfMissing = true)
public class PersistedTikTokShopChannelInboundGateway extends PersistedChannelInboundAdapterSupport {

    public PersistedTikTokShopChannelInboundGateway(ChannelInventorySnapshotRepository repository) {
        super(repository, SalesChannel.TIKTOK_SHOP);
    }
}
