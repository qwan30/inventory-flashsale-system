package com.codex.flashsale.application;

import com.codex.flashsale.api.ConfirmReservationResponse;
import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.InventoryResponse;
import com.codex.flashsale.api.ReleaseReservationResponse;
import com.codex.flashsale.api.ReservationResponse;
import com.codex.flashsale.channel.ChannelService;
import com.codex.flashsale.channel.ReservationValidationRequest;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import com.codex.flashsale.config.RedisLockManager;
import com.codex.flashsale.events.OrderEventPayload;
import com.codex.flashsale.events.ReservationEventPayload;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.flashsale.FlashSaleCampaignService;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryService;
import com.codex.flashsale.inventory.ReservationStatus;
import com.codex.flashsale.inventory.StockReservation;
import com.codex.flashsale.order.OrderHeader;
import com.codex.flashsale.order.OrderService;
import com.codex.flashsale.order.OrderStatus;
import com.codex.flashsale.outbox.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationApplicationService {

    private final ChannelService channelService;
    private final FlashSaleCampaignService flashSaleCampaignService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final OutboxService outboxService;
    private final RedisLockManager redisLockManager;
    private final TimeProvider timeProvider;
    private final ApplicationProperties applicationProperties;
    private final TransactionTemplate transactionTemplate;
    private final Counter reserveSuccessCounter;
    private final Counter reserveFailureCounter;
    private final Counter reserveConflictCounter;
    private final Counter confirmConflictCounter;
    private final Counter releaseConflictCounter;
    private final Counter expiryReleaseCounter;
    private final Timer reserveLatency;

    public ReservationApplicationService(
            ChannelService channelService,
            FlashSaleCampaignService flashSaleCampaignService,
            InventoryService inventoryService,
            OrderService orderService,
            OutboxService outboxService,
            RedisLockManager redisLockManager,
            TimeProvider timeProvider,
            ApplicationProperties applicationProperties,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry
    ) {
        this.channelService = channelService;
        this.flashSaleCampaignService = flashSaleCampaignService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.outboxService = outboxService;
        this.redisLockManager = redisLockManager;
        this.timeProvider = timeProvider;
        this.applicationProperties = applicationProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.reserveSuccessCounter = meterRegistry.counter("reservation.requests.success");
        this.reserveFailureCounter = meterRegistry.counter("reservation.requests.failure");
        this.reserveConflictCounter = meterRegistry.counter("reservation.requests.conflict");
        this.confirmConflictCounter = meterRegistry.counter("reservation.confirm.conflict");
        this.releaseConflictCounter = meterRegistry.counter("reservation.release.conflict");
        this.expiryReleaseCounter = meterRegistry.counter("reservation.expiry.release");
        this.reserveLatency = meterRegistry.timer("reservation.requests.latency");
    }

    public ReservationResponse reserve(String campaignId, CreateReservationRequest request, String idempotencyKey) {
        Optional<StockReservation> existingReservation = inventoryService.findByIdempotencyKey(idempotencyKey);
        if (existingReservation.isPresent()) {
            return buildReservationResponse(existingReservation.get());
        }

        Timer.Sample sample = Timer.start();
        try {
            channelService.validateReservation(new ReservationValidationRequest(request.channel(), request.sku(), request.quantity()));
            ReservationResponse response = redisLockManager.executeWithLock(
                    inventoryLockKey(request.sku()),
                    () -> transactionTemplate.execute(status -> reserveInTransaction(campaignId, request, idempotencyKey))
            );
            reserveSuccessCounter.increment();
            return response;
        } catch (ConflictException exception) {
            reserveConflictCounter.increment();
            reserveFailureCounter.increment();
            throw exception;
        } catch (RuntimeException exception) {
            reserveFailureCounter.increment();
            throw exception;
        } finally {
            sample.stop(reserveLatency);
        }
    }

    public ConfirmReservationResponse confirm(String reservationId, String confirmIdempotencyKey) {
        StockReservation reservation = inventoryService.getRequiredReservation(reservationId);
        try {
            return redisLockManager.executeWithLock(
                    inventoryLockKey(reservation.getSku()),
                    () -> transactionTemplate.execute(status -> confirmInTransaction(reservationId, confirmIdempotencyKey))
            );
        } catch (ConflictException exception) {
            confirmConflictCounter.increment();
            throw exception;
        }
    }

    public ReleaseReservationResponse release(String reservationId) {
        StockReservation reservation = inventoryService.getRequiredReservation(reservationId);
        try {
            return redisLockManager.executeWithLock(
                    inventoryLockKey(reservation.getSku()),
                    () -> transactionTemplate.execute(status -> releaseInTransaction(reservationId, ReservationStatus.RELEASED))
            );
        } catch (ConflictException exception) {
            releaseConflictCounter.increment();
            throw exception;
        }
    }

    public void expireReservation(String reservationId) {
        StockReservation reservation = inventoryService.getRequiredReservation(reservationId);
        try {
            redisLockManager.executeWithLock(
                    inventoryLockKey(reservation.getSku()),
                    () -> transactionTemplate.execute(status -> {
                        releaseInTransaction(reservationId, ReservationStatus.EXPIRED);
                        return Boolean.TRUE;
                    })
            );
        } catch (ConflictException exception) {
            releaseConflictCounter.increment();
            throw exception;
        }
    }

    private ReservationResponse reserveInTransaction(
            String campaignId,
            CreateReservationRequest request,
            String idempotencyKey
    ) {
        Optional<StockReservation> existingReservation = inventoryService.findByIdempotencyKey(idempotencyKey);
        if (existingReservation.isPresent()) {
            return buildReservationResponse(existingReservation.get());
        }

        Instant now = timeProvider.now();
        FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(campaignId);
        flashSaleCampaignService.validateCampaignSku(campaign, request.sku());

        try {
            flashSaleCampaignService.ensureReservable(campaign, request.quantity(), now);
            InventoryItem inventoryItem = inventoryService.getRequiredInventory(request.sku());
            inventoryItem.reserve(request.quantity());
            inventoryService.saveInventory(inventoryItem);
            flashSaleCampaignService.save(campaign);

            StockReservation reservation = inventoryService.createReservation(
                    UUID.randomUUID().toString(),
                    request.sku(),
                    campaignId,
                    request.channel(),
                    request.quantity(),
                    idempotencyKey,
                    now.plus(applicationProperties.getReservation().getTtl())
            );
            outboxService.record(
                    "reservation",
                    reservation.getId(),
                    "inventory.reservation.created",
                    new ReservationEventPayload(
                            reservation.getId(),
                            reservation.getCampaignId(),
                            reservation.getSku(),
                            reservation.getChannel(),
                            reservation.getQuantity(),
                            reservation.getStatus(),
                            reservation.getExpiresAt()
                    )
            );
            return toReservationResponse(reservation, inventoryItem, campaign);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConflictException("INVENTORY_VERSION_CONFLICT", "Inventory version conflict detected for SKU " + request.sku());
        }
    }

    private ConfirmReservationResponse confirmInTransaction(String reservationId, String confirmIdempotencyKey) {
        StockReservation reservation = inventoryService.getRequiredReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED
                && confirmIdempotencyKey.equals(reservation.getConfirmIdempotencyKey())) {
            OrderHeader existingOrder = orderService.getRequired(reservation.getOrderId());
            return new ConfirmReservationResponse(reservation.getId(), existingOrder.getId(), existingOrder.getStatus());
        }

        Instant now = timeProvider.now();
        if (reservation.isExpired(now)) {
            throw new ConflictException("RESERVATION_EXPIRED", "Reservation has expired");
        }

        InventoryItem inventoryItem = inventoryService.getRequiredInventory(reservation.getSku());
        inventoryItem.confirm(reservation.getQuantity());
        inventoryService.saveInventory(inventoryItem);

        if (reservation.getCampaignId() != null) {
            FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(reservation.getCampaignId());
            campaign.confirmQuota(reservation.getQuantity());
            flashSaleCampaignService.save(campaign);
        }

        String orderId = Optional.ofNullable(reservation.getOrderId()).orElse(UUID.randomUUID().toString());
        OrderHeader order = orderService.findByReservationId(reservationId)
                .orElseGet(() -> orderService.createPendingOrder(orderId, reservationId, reservation.getChannel()));

        reservation.confirm(confirmIdempotencyKey, order.getId(), now);
        inventoryService.saveReservation(reservation);
        outboxService.record(
                "order",
                order.getId(),
                "order.created",
                new OrderEventPayload(order.getId(), order.getReservationId(), order.getChannel(), order.getStatus())
        );
        return new ConfirmReservationResponse(reservation.getId(), order.getId(), order.getStatus());
    }

    private ReleaseReservationResponse releaseInTransaction(String reservationId, ReservationStatus finalStatus) {
        StockReservation reservation = inventoryService.getRequiredReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return new ReleaseReservationResponse(
                    reservation.getId(),
                    reservation.getStatus(),
                    toInventoryResponse(inventoryService.getRequiredInventory(reservation.getSku()))
            );
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new ConflictException("RESERVATION_ALREADY_CONFIRMED", "Confirmed reservation cannot be released");
        }

        InventoryItem inventoryItem = inventoryService.getRequiredInventory(reservation.getSku());
        inventoryItem.release(reservation.getQuantity());
        inventoryService.saveInventory(inventoryItem);

        if (reservation.getCampaignId() != null) {
            FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(reservation.getCampaignId());
            campaign.releaseQuota(reservation.getQuantity());
            flashSaleCampaignService.save(campaign);
        }

        reservation.release(finalStatus);
        inventoryService.saveReservation(reservation);
        if (finalStatus == ReservationStatus.EXPIRED) {
            expiryReleaseCounter.increment();
        }
        outboxService.record(
                "reservation",
                reservation.getId(),
                "inventory.reservation.released",
                new ReservationEventPayload(
                        reservation.getId(),
                        reservation.getCampaignId(),
                        reservation.getSku(),
                        reservation.getChannel(),
                        reservation.getQuantity(),
                        reservation.getStatus(),
                        reservation.getExpiresAt()
                )
        );
        return new ReleaseReservationResponse(reservation.getId(), reservation.getStatus(), toInventoryResponse(inventoryItem));
    }

    private ReservationResponse buildReservationResponse(StockReservation reservation) {
        InventoryItem inventoryItem = inventoryService.getRequiredInventory(reservation.getSku());
        FlashSaleCampaign campaign = flashSaleCampaignService.getRequired(reservation.getCampaignId());
        return toReservationResponse(reservation, inventoryItem, campaign);
    }

    private ReservationResponse toReservationResponse(
            StockReservation reservation,
            InventoryItem inventoryItem,
            FlashSaleCampaign campaign
    ) {
        int remainingCampaignQty = campaign.getQuota() - campaign.getReservedQuota() - campaign.getSoldQuota();
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCampaignId(),
                reservation.getSku(),
                reservation.getChannel(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                toInventoryResponse(inventoryItem),
                remainingCampaignQty
        );
    }

    private InventoryResponse toInventoryResponse(InventoryItem inventoryItem) {
        return new InventoryResponse(
                inventoryItem.getSku(),
                inventoryItem.getAvailableQty(),
                inventoryItem.getReservedQty(),
                inventoryItem.getSoldQty(),
                inventoryItem.getVersion()
        );
    }

    private String inventoryLockKey(String sku) {
        return "lock:inventory:" + sku;
    }
}
