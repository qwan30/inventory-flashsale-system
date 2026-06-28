package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;

import com.codex.flashsale.common.domain.SalesChannel;
import java.time.Instant;

public record ChannelSyncCommand(
        String outboxEventId,
        SalesChannel channel,
        String eventType,
        String sku,
        Integer availableQty,
        Integer reservedQty,
        Integer soldQty,
        Instant occurredAt,
        String payload
) {
}
