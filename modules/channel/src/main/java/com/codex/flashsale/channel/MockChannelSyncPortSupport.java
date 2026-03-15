package com.codex.flashsale.channel;

import com.codex.flashsale.channel.sync.ChannelSyncCommand;
import com.codex.flashsale.channel.sync.ChannelSyncPort;
import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

abstract class MockChannelSyncPortSupport implements ChannelSyncPort {

    private static final ConcurrentHashMap<String, AtomicInteger> TRANSIENT_FAILURES = new ConcurrentHashMap<>();

    @Override
    public void publish(ChannelSyncCommand command) {
        if (command.sku() == null) {
            return;
        }
        if (command.sku().contains("PERMANENT")) {
            throw new PermanentChannelSyncException("Mock permanent sync failure for " + command.sku());
        }
        if (command.sku().contains("TRANSIENT")) {
            String failureKey = channel().name() + ":" + command.outboxEventId();
            int attempt = TRANSIENT_FAILURES.computeIfAbsent(failureKey, key -> new AtomicInteger()).incrementAndGet();
            if (attempt == 1) {
                throw new TransientChannelSyncException("Mock transient sync failure for " + command.sku());
            }
        }
    }
}
