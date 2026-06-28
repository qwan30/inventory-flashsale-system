package com.codex.flashsale.channel.sync;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.connector.tiktok.TikTokChannelClient;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(TikTokChannelClient.class)
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "real")
public class TikTokLiveChannelInboundGateway implements ChannelInboundPort {

    private static final String LIVE_TIKTOK_SOURCE = "LIVE_TIKTOK";

    private final TikTokChannelClient tikTokChannelClient;
    private final TimeProvider timeProvider;

    public TikTokLiveChannelInboundGateway(
            TikTokChannelClient tikTokChannelClient,
            TimeProvider timeProvider
    ) {
        this.tikTokChannelClient = tikTokChannelClient;
        this.timeProvider = timeProvider;
    }

    @Override
    public SalesChannel channel() {
        return SalesChannel.TIKTOK_SHOP;
    }

    @Override
    public Optional<ChannelInventorySnapshotView> fetchInventorySnapshot(String sku) {
        return tikTokChannelClient.findListingBySku(sku)
                .map(listing -> new ChannelInventorySnapshotView(
                        SalesChannel.TIKTOK_SHOP,
                        listing.stock().sku(),
                        listing.stock().availableQty(),
                        listing.stock().reservedQty(),
                        0,
                        timeProvider.now(),
                        LIVE_TIKTOK_SOURCE,
                        false
                ));
    }
}
