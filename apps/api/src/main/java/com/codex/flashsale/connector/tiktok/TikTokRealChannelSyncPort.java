package com.codex.flashsale.connector.tiktok;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelSyncCommand;
import com.codex.flashsale.channel.sync.ChannelSyncPort;
import com.codex.flashsale.channel.sync.PermanentChannelSyncException;
import com.codex.flashsale.channel.sync.TransientChannelSyncException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.channel.tik-tok.mode", havingValue = "real")
public class TikTokRealChannelSyncPort implements ChannelSyncPort {

    private final TikTokChannelClient tikTokChannelClient;

    public TikTokRealChannelSyncPort(TikTokChannelClient tikTokChannelClient) {
        this.tikTokChannelClient = tikTokChannelClient;
    }

    @Override
    public SalesChannel channel() {
        return SalesChannel.TIKTOK_SHOP;
    }

    @Override
    public void publish(ChannelSyncCommand command) {
        if (command.sku() == null || command.sku().isBlank()) {
            throw new PermanentChannelSyncException("TikTok sync requires non-blank SKU");
        }
        if (command.availableQty() == null) {
            throw new PermanentChannelSyncException("TikTok sync requires availableQty payload");
        }
        TikTokListingView listing = tikTokChannelClient.findListingBySku(command.sku())
                .orElseThrow(() -> new PermanentChannelSyncException("TikTok listing not found for SKU " + command.sku()));
        try {
            tikTokChannelClient.updateAvailableStock(listing.reference(), command.availableQty());
        } catch (PermanentChannelSyncException | TransientChannelSyncException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransientChannelSyncException("Unexpected TikTok sync failure: " + exception.getMessage());
        }
    }
}
