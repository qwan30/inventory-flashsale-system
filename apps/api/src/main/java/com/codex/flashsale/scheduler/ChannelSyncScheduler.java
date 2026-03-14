package com.codex.flashsale.scheduler;

import com.codex.flashsale.channel.sync.ChannelSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChannelSyncScheduler {

    private final ChannelSyncService channelSyncService;

    public ChannelSyncScheduler(ChannelSyncService channelSyncService) {
        this.channelSyncService = channelSyncService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.channel-sync-delay:10s}")
    public void publishPendingAttempts() {
        channelSyncService.retryFailedAttempts();
        channelSyncService.publishPendingAttempts();
    }
}
