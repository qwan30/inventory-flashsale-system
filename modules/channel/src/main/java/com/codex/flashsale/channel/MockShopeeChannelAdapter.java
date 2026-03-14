package com.codex.flashsale.channel;

import org.springframework.stereotype.Component;

@Component
public class MockShopeeChannelAdapter extends MockChannelAdapterSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.SHOPEE;
    }
}

