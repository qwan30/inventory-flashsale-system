package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "channel_inventory_snapshot")
public class ChannelInventorySnapshot extends AuditTimestamps {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "sold_qty", nullable = false)
    private int soldQty;

    @Column(name = "source_outbox_event_id", nullable = false)
    private String sourceOutboxEventId;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected ChannelInventorySnapshot() {
    }

    public ChannelInventorySnapshot(
            String id,
            SalesChannel channel,
            String sku,
            int availableQty,
            int reservedQty,
            int soldQty,
            String sourceOutboxEventId,
            Instant syncedAt
    ) {
        this.id = id;
        this.channel = channel;
        this.sku = sku;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.soldQty = soldQty;
        this.sourceOutboxEventId = sourceOutboxEventId;
        this.syncedAt = syncedAt;
    }

    public void apply(ChannelSyncCommand command, Instant syncedAt) {
        this.availableQty = command.availableQty();
        this.reservedQty = command.reservedQty();
        this.soldQty = command.soldQty();
        this.sourceOutboxEventId = command.outboxEventId();
        this.syncedAt = syncedAt;
    }

    public ChannelInventorySnapshotView toView() {
        return new ChannelInventorySnapshotView(channel, sku, availableQty, reservedQty, soldQty, syncedAt, sourceOutboxEventId);
    }

    public static String snapshotId(SalesChannel channel, String sku) {
        return channel.name() + ":" + sku;
    }
}
