package com.codex.flashsale.channel;

import com.codex.flashsale.common.domain.SalesChannel;

import org.springframework.stereotype.Component;

@Component
public class MockAppChannelSyncPort extends MockChannelSyncPortSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.APP;
    }
}
