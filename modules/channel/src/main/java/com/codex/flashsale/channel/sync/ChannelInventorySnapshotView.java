package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import java.time.Instant;

public record ChannelInventorySnapshotView(
        SalesChannel channel,
        String sku,
        int availableQty,
        int reservedQty,
        int soldQty,
        Instant syncedAt,
        String sourceOutboxEventId,
        boolean soldQtyComparable
) {

    public ChannelInventorySnapshotView(
            SalesChannel channel,
            String sku,
            int availableQty,
            int reservedQty,
            int soldQty,
            Instant syncedAt,
            String sourceOutboxEventId
    ) {
        this(channel, sku, availableQty, reservedQty, soldQty, syncedAt, sourceOutboxEventId, true);
    }
}
