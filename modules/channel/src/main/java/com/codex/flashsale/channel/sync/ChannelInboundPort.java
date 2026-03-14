package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import java.util.Optional;

public interface ChannelInboundPort {

    Optional<ChannelInventorySnapshotView> fetchInventorySnapshot(SalesChannel channel, String sku);
}
