package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;

public interface ChannelSyncPort {

    SalesChannel channel();

    void publish(ChannelSyncCommand command);
}
