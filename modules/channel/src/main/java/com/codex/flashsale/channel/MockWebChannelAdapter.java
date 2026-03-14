package com.codex.flashsale.channel;

import org.springframework.stereotype.Component;

@Component
public class MockWebChannelAdapter extends MockChannelAdapterSupport {

    @Override
    public SalesChannel channel() {
        return SalesChannel.WEB;
    }
}

