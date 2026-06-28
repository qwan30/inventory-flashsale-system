package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

import org.springframework.stereotype.Component;

@Component
public class MockWebChannelSyncPort extends MockChannelSyncPortSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.WEB;
    }
}
