package com.codex.flashsale.channel.ingress;

import com.codex.flashsale.api.AdminTikTokIngressReplayRequest;
import com.codex.flashsale.api.TikTokIngressReceiptResponse;
import com.codex.flashsale.api.TikTokInventoryIngressRequest;
import com.codex.flashsale.api.TikTokOrderStatusIngressRequest;
import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshot;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshotRepository;
import com.codex.flashsale.common.exception.BadRequestException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.application.OrderApplicationService;
import com.codex.flashsale.order.OrderStatus;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TikTokIngressService {

    private static final String INVENTORY_TYPE = "INVENTORY";
    private static final String ORDER_STATUS_TYPE = "ORDER_STATUS";

    private final ChannelInventorySnapshotRepository snapshotRepository;
    private final TikTokIngressReceiptRepository receiptRepository;
    private final OrderApplicationService orderApplicationService;
    private final OutboxService outboxService;
    private final TimeProvider timeProvider;

    public TikTokIngressService(
            ChannelInventorySnapshotRepository snapshotRepository,
            TikTokIngressReceiptRepository receiptRepository,
            OrderApplicationService orderApplicationService,
            OutboxService outboxService,
            TimeProvider timeProvider
    ) {
        this.snapshotRepository = snapshotRepository;
        this.receiptRepository = receiptRepository;
        this.orderApplicationService = orderApplicationService;
        this.outboxService = outboxService;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public TikTokIngressReceiptResponse ingestInventory(TikTokInventoryIngressRequest request, String rawBody) {
        String receiptKey = receiptKey(INVENTORY_TYPE, request.receiptId());
        if (receiptRepository.existsById(receiptKey)) {
            return new TikTokIngressReceiptResponse(request.receiptId(), "DUPLICATE", "Inventory ingress receipt already processed");
        }
        Instant observedAt = request.observedAt() != null ? request.observedAt() : timeProvider.now();
        OutboxEvent sourceEvent = outboxService.record(
                "channel-ingress",
                request.receiptId(),
                "channel.inventory.ingested",
                request
        );
        String snapshotId = ChannelInventorySnapshot.snapshotId(SalesChannel.TIKTOK_SHOP, request.sku());
        snapshotRepository.saveAndFlush(new ChannelInventorySnapshot(
                snapshotId,
                SalesChannel.TIKTOK_SHOP,
                request.sku(),
                request.availableQty(),
                request.reservedQty(),
                request.soldQty(),
                sourceEvent.getId(),
                observedAt
        ));
        receiptRepository.saveAndFlush(new TikTokIngressReceipt(
                receiptKey,
                SalesChannel.TIKTOK_SHOP,
                INVENTORY_TYPE,
                request.receiptId(),
                sha256(rawBody),
                "PROCESSED",
                timeProvider.now()
        ));
        return new TikTokIngressReceiptResponse(request.receiptId(), "PROCESSED", "Inventory snapshot updated");
    }

    @Transactional
    public TikTokIngressReceiptResponse ingestOrderStatus(TikTokOrderStatusIngressRequest request, String rawBody) {
        String receiptKey = receiptKey(ORDER_STATUS_TYPE, request.receiptId());
        if (receiptRepository.existsById(receiptKey)) {
            return new TikTokIngressReceiptResponse(request.receiptId(), "DUPLICATE", "Order status ingress receipt already processed");
        }
        OrderStatus orderStatus = parseOrderStatus(request.status());
        orderApplicationService.updateStatus(request.orderId(), orderStatus, "tiktok-ingress:" + request.receiptId());
        receiptRepository.saveAndFlush(new TikTokIngressReceipt(
                receiptKey,
                SalesChannel.TIKTOK_SHOP,
                ORDER_STATUS_TYPE,
                request.receiptId(),
                sha256(rawBody),
                "PROCESSED",
                timeProvider.now()
        ));
        return new TikTokIngressReceiptResponse(request.receiptId(), "PROCESSED", "Order status applied");
    }

    public TikTokIngressReceiptResponse replay(AdminTikTokIngressReplayRequest request) {
        if (request.kind() == null || request.kind().isBlank()) {
            throw new BadRequestException("TIKTOK_REPLAY_KIND_REQUIRED", "Replay request kind is required");
        }
        return switch (request.kind().trim().toUpperCase()) {
            case INVENTORY_TYPE -> ingestInventory(new TikTokInventoryIngressRequest(
                    requireText(request.receiptId(), "receiptId"),
                    requireText(request.sku(), "sku"),
                    requireNumber(request.availableQty(), "availableQty"),
                    requireNumber(request.reservedQty(), "reservedQty"),
                    requireNumber(request.soldQty(), "soldQty"),
                    request.observedAt()
            ), replayRawBody(request));
            case ORDER_STATUS_TYPE -> ingestOrderStatus(new TikTokOrderStatusIngressRequest(
                    requireText(request.receiptId(), "receiptId"),
                    requireText(request.orderId(), "orderId"),
                    requireText(request.status(), "status"),
                    request.observedAt()
            ), replayRawBody(request));
            default -> throw new BadRequestException("TIKTOK_REPLAY_KIND_INVALID", "Unsupported TikTok replay kind: " + request.kind());
        };
    }

    private String receiptKey(String type, String receiptId) {
        return SalesChannel.TIKTOK_SHOP.name() + ":" + type + ":" + receiptId;
    }

    private OrderStatus parseOrderStatus(String rawStatus) {
        try {
            return OrderStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("TIKTOK_ORDER_STATUS_INVALID", "Unsupported TikTok order status: " + rawStatus);
        }
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("TIKTOK_REPLAY_FIELD_REQUIRED", "Replay request field is required: " + field);
        }
        return value;
    }

    private Integer requireNumber(Integer value, String field) {
        if (value == null || value < 0) {
            throw new BadRequestException("TIKTOK_REPLAY_FIELD_REQUIRED", "Replay request field is invalid: " + field);
        }
        return value;
    }

    private String replayRawBody(AdminTikTokIngressReplayRequest request) {
        return "replay:" + request.kind() + ":" + request.receiptId() + ":" + timeProvider.now();
    }

    private String sha256(String rawBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(Character.forDigit((value >>> 4) & 0x0F, 16));
                builder.append(Character.forDigit(value & 0x0F, 16));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash TikTok ingress payload", exception);
        }
    }
}
