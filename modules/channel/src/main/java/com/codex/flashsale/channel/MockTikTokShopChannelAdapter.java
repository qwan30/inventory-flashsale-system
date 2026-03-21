package com.codex.flashsale.channel;

import org.springframework.stereotype.Component;

@Component
public class MockTikTokShopChannelAdapter extends MockChannelAdapterSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.TIKTOK_SHOP;
    }
}
