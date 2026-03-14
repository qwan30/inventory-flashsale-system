package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;

public interface ChannelSyncPort {

    SalesChannel channel();

    void publish(ChannelSyncCommand command);
}
