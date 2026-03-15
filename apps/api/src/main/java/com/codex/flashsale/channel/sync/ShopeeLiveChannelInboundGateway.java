package com.codex.flashsale.channel.sync;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.connector.shopee.ShopeeChannelClient;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ShopeeChannelClient.class)
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "real")
public class ShopeeLiveChannelInboundGateway implements ChannelInboundPort {

    private static final String LIVE_SHOPEE_SOURCE = "LIVE_SHOPEE";

    private final ShopeeChannelClient shopeeChannelClient;
    private final TimeProvider timeProvider;

    public ShopeeLiveChannelInboundGateway(
            ShopeeChannelClient shopeeChannelClient,
            TimeProvider timeProvider
    ) {
        this.shopeeChannelClient = shopeeChannelClient;
        this.timeProvider = timeProvider;
    }

    @Override
    public SalesChannel channel() {
        return SalesChannel.SHOPEE;
    }

    @Override
    public Optional<ChannelInventorySnapshotView> fetchInventorySnapshot(String sku) {
        return shopeeChannelClient.findListingBySku(sku)
                .map(listing -> new ChannelInventorySnapshotView(
                        SalesChannel.SHOPEE,
                        listing.stock().sku(),
                        listing.stock().availableQty(),
                        listing.stock().reservedQty(),
                        0,
                        timeProvider.now(),
                        LIVE_SHOPEE_SOURCE,
                        false
                ));
    }
}
