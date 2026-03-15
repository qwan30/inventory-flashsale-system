package com.codex.flashsale.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "mock", matchIfMissing = true)
public class MockShopeeChannelSyncPort extends MockChannelSyncPortSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.SHOPEE;
    }
}
