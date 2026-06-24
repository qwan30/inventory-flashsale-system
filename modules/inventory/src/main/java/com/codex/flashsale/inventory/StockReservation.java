package com.codex.flashsale.inventory;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Represents a lock/reservation of inventory stock for a specific SKU.
 * In a flash sale system, stock is reserved first before order placement
 * to guarantee fulfillment under heavy loads, preventing overselling.
 * 
 * Lifecycles:
 * - ACTIVE: Stock is locked and waiting for order confirmation.
 * - CONFIRMED: Order was successfully placed. The locked stock is converted to a sale.
 * - RELEASED: Reservation was canceled or expired without an order, and stock was returned.
 * - EXPIRED: Marked as expired by scheduler, returning stock to the available pool.
 */
@Entity
@Table(name = "stock_reservation")
public class StockReservation extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "sku", nullable = false)
    private String sku;

    /** The campaign associated with this reservation (optional). */
    @Column(name = "campaign_id")
    private String campaignId;

    /** The channel (Shopee, TikTok Shop, Web) from which the order request came. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    /** Number of items reserved. */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    /** Timestamp after which an ACTIVE reservation is eligible for automatic cleanup. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Unique key to ensure creating a reservation is idempotent. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    /** Unique key to ensure confirming a reservation is idempotent. */
    @Column(name = "confirm_idempotency_key")
    private String confirmIdempotencyKey;

    /** The order ID generated after the reservation is successfully confirmed. */
    @Column(name = "order_id")
    private String orderId;

    protected StockReservation() {
    }

    public static StockReservation active(
            String id,
            String sku,
            String campaignId,
            SalesChannel channel,
            int quantity,
            String idempotencyKey,
            Instant expiresAt
    ) {
        return new StockReservation(id, sku, campaignId, channel, quantity, ReservationStatus.ACTIVE, expiresAt, idempotencyKey);
    }

    private StockReservation(
            String id,
            String sku,
            String campaignId,
            SalesChannel channel,
            int quantity,
            ReservationStatus status,
            Instant expiresAt,
            String idempotencyKey
    ) {
        this.id = id;
        this.sku = sku;
        this.campaignId = campaignId;
        this.channel = channel;
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * Checks if the reservation expiry time has reached or passed.
     */
    public boolean isExpired(Instant now) {
        return expiresAt.equals(now) || expiresAt.isBefore(now);
    }

    /**
     * Confirms the reservation, linking it to a finalized order.
     * 
     * @param confirmKey unique idempotency key for confirmation
     * @param orderId the ID of the placed order
     * @param now current timestamp to validate against expiration
     * @throws ConflictException if already confirmed/not active or if it has expired
     */
    public void confirm(String confirmKey, String orderId, Instant now) {
        if (status == ReservationStatus.CONFIRMED) {
            if (confirmKey.equals(confirmIdempotencyKey)) {
                return;
            }
            throw new ConflictException("RESERVATION_ALREADY_CONFIRMED", "Reservation already confirmed");
        }
        if (status != ReservationStatus.ACTIVE) {
            throw new ConflictException("RESERVATION_NOT_ACTIVE", "Reservation is not active");
        }
        if (isExpired(now)) {
            throw new ConflictException("RESERVATION_EXPIRED", "Reservation has expired");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmIdempotencyKey = confirmKey;
        this.orderId = orderId;
    }

    /**
     * Releases this reservation, changing its status to either RELEASED or EXPIRED.
     * 
     * @param finalStatus the target final status (RELEASED or EXPIRED)
     * @throws ConflictException if the reservation is already confirmed
     */
    public void release(ReservationStatus finalStatus) {
        if (status == ReservationStatus.CONFIRMED) {
            throw new ConflictException("RESERVATION_ALREADY_CONFIRMED", "Confirmed reservation cannot be released");
        }
        if (status == ReservationStatus.RELEASED || status == ReservationStatus.EXPIRED) {
            return;
        }
        this.status = finalStatus;
    }

    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getConfirmIdempotencyKey() {
        return confirmIdempotencyKey;
    }

    public String getOrderId() {
        return orderId;
    }
}

