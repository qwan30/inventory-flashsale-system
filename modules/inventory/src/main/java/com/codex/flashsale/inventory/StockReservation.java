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

@Entity
@Table(name = "stock_reservation")
public class StockReservation extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "campaign_id")
    private String campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "confirm_idempotency_key")
    private String confirmIdempotencyKey;

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

    public boolean isExpired(Instant now) {
        return expiresAt.equals(now) || expiresAt.isBefore(now);
    }

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

