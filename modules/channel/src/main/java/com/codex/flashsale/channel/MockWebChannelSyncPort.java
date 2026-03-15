package com.codex.flashsale.channel;

import org.springframework.stereotype.Component;

@Component
public class MockWebChannelSyncPort extends MockChannelSyncPortSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.WEB;
    }
}
