package com.codex.flashsale.connector.shopee;

import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelSyncCommand;
import com.codex.flashsale.channel.sync.ChannelSyncPort;
import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.shopee.mode", havingValue = "real")
public class ShopeeRealChannelSyncPort implements ChannelSyncPort {

    private final ShopeeChannelClient shopeeChannelClient;

    public ShopeeRealChannelSyncPort(ShopeeChannelClient shopeeChannelClient) {
        this.shopeeChannelClient = shopeeChannelClient;
    }

    @Override
    public SalesChannel channel() {
        return SalesChannel.SHOPEE;
    }

    @Override
    public void publish(ChannelSyncCommand command) {
        if (command.sku() == null || command.sku().isBlank()) {
            throw new PermanentChannelSyncException("Shopee sync requires non-blank SKU");
        }
        if (command.availableQty() == null) {
            throw new PermanentChannelSyncException("Shopee sync requires availableQty payload");
        }
        ShopeeListingView listing = shopeeChannelClient.findListingBySku(command.sku())
                .orElseThrow(() -> new PermanentChannelSyncException("Shopee listing not found for SKU " + command.sku()));
        try {
            shopeeChannelClient.updateSellerStock(listing.reference(), command.availableQty());
        } catch (PermanentChannelSyncException | TransientChannelSyncException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransientChannelSyncException("Unexpected Shopee sync failure: " + exception.getMessage());
        }
    }
}
