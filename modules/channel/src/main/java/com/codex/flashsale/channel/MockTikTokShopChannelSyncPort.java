package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "mock", matchIfMissing = true)
public class MockTikTokShopChannelSyncPort extends MockChannelSyncPortSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.TIKTOK_SHOP;
    }
}
