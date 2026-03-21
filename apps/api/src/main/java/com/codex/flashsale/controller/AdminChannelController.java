package com.codex.flashsale.controller;

import com.codex.flashsale.api.ChannelHealthDetailResponse;
import com.codex.flashsale.api.ChannelHealthIngressReceiptResponse;
import com.codex.flashsale.api.ChannelHealthReplayResponse;
import com.codex.flashsale.api.ChannelHealthResponse;
import com.codex.flashsale.application.ChannelHealthSummary;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.BadRequestException;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/admin/channels")
public class AdminChannelController {

    private final OpsApplicationService opsApplicationService;

    public AdminChannelController(OpsApplicationService opsApplicationService) {
        this.opsApplicationService = opsApplicationService;
    }

    @GetMapping("/health")
    public List<ChannelHealthResponse> listChannelHealth() {
        return opsApplicationService.listChannelHealthSummaries().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/health/{channel}")
    public ChannelHealthDetailResponse getChannelHealthDetail(@PathVariable String channel) {
        SalesChannel salesChannel = parseChannel(channel);
        return opsApplicationService.getChannelHealthDetail(salesChannel);
    }

    private ChannelHealthResponse toResponse(ChannelHealthSummary summary) {
        return new ChannelHealthResponse(
                summary.channel().name(),
                summary.status().name(),
                summary.connectorMode(),
                summary.configValid(),
                summary.syncBacklogCount(),
                summary.staleSnapshotCount(),
                summary.openDriftCount(),
                summary.lastReconciliationAt(),
                summary.latestIngressReceipt() == null
                        ? null
                        : new ChannelHealthIngressReceiptResponse(
                                summary.latestIngressReceipt().type(),
                                summary.latestIngressReceipt().externalReceiptId(),
                                summary.latestIngressReceipt().outcome(),
                                summary.latestIngressReceipt().processedAt()
                        ),
                summary.latestReplay() == null
                        ? null
                        : new ChannelHealthReplayResponse(
                                summary.latestReplay().action(),
                                summary.latestReplay().resourceId(),
                                summary.latestReplay().outcome(),
                                summary.latestReplay().createdAt(),
                                summary.latestReplay().details()
                        )
        );
    }

    private SalesChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new BadRequestException("CHANNEL_IS_REQUIRED", "Channel path parameter is required");
        }
        try {
            return SalesChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("CHANNEL_NOT_FOUND", "Unknown channel: " + channel);
        }
    }
}
