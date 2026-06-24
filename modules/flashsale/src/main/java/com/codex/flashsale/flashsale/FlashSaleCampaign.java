package com.codex.flashsale.flashsale;

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
 * Represents a specific flash sale campaign for a SKU.
 * Flash sale campaigns run within a defined time window and have
 * a dedicated inventory quota to prevent overloading general warehouse inventory.
 * 
 * Lifecycles:
 * - DRAFT: Created, configurable, and inactive.
 * - ACTIVE: Active and accepting orders if the current time falls inside the window.
 * - ENDED: Manually or automatically concluded campaign. No further orders accepted.
 */
@Entity
@Table(name = "flash_sale_campaign")
public class FlashSaleCampaign extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "sku", nullable = false)
    private String sku;

    /** Timestamp when the campaign starts. */
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    /** Timestamp when the campaign ends. */
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Maximum allowed quantity to be reserved or sold under this campaign. */
    @Column(name = "quota", nullable = false)
    private int quota;

    /** Current quantity reserved under this campaign. */
    @Column(name = "reserved_quota", nullable = false)
    private int reservedQuota;

    /** Current quantity confirmed sold under this campaign. */
    @Column(name = "sold_quota", nullable = false)
    private int soldQuota;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CampaignStatus status;

    protected FlashSaleCampaign() {
    }

    public FlashSaleCampaign(
            String id,
            String sku,
            Instant startsAt,
            Instant endsAt,
            int quota,
            int reservedQuota,
            int soldQuota,
            CampaignStatus status
    ) {
        this.id = id;
        this.sku = sku;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.quota = quota;
        this.reservedQuota = reservedQuota;
        this.soldQuota = soldQuota;
        this.status = status;
    }

    /**
     * Creates a new campaign in DRAFT status.
     */
    public static FlashSaleCampaign draft(String id, String sku, Instant startsAt, Instant endsAt, int quota) {
        validateWindow(startsAt, endsAt);
        validateQuota(quota);
        return new FlashSaleCampaign(id, sku, startsAt, endsAt, quota, 0, 0, CampaignStatus.DRAFT);
    }

    /**
     * Asserts that the campaign is currently ACTIVE and within its start/end window.
     * 
     * @param now current time
     * @throws ConflictException if not active or outside the timeframe window
     */
    public void ensureActive(Instant now) {
        if (status != CampaignStatus.ACTIVE) {
            throw new ConflictException("FLASH_SALE_INACTIVE", "Flash sale campaign is not active");
        }
        if (now.isBefore(startsAt) || now.isAfter(endsAt)) {
            throw new ConflictException("FLASH_SALE_OUT_OF_WINDOW", "Flash sale campaign is outside the active window");
        }
    }

    /**
     * Reserves campaign quota for a given quantity.
     * 
     * @param quantity amount to reserve
     * @throws ConflictException if reservation exceeds remaining quota
     */
    public void reserveQuota(int quantity) {
        int remaining = quota - reservedQuota - soldQuota;
        if (remaining < quantity) {
            throw new ConflictException("FLASH_SALE_QUOTA_EXCEEDED", "Flash sale quota is exhausted");
        }
        reservedQuota += quantity;
    }

    /**
     * Releases a previously reserved campaign quota.
     */
    public void releaseQuota(int quantity) {
        reservedQuota = Math.max(0, reservedQuota - quantity);
    }

    /**
     * Confirms campaign quota, moving it from reserved to sold.
     * 
     * @param quantity amount to confirm
     * @throws ConflictException if reserved quota is insufficient
     */
    public void confirmQuota(int quantity) {
        if (reservedQuota < quantity) {
            throw new ConflictException("FLASH_SALE_QUOTA_CONFLICT", "Reserved quota is insufficient to confirm");
        }
        reservedQuota -= quantity;
        soldQuota += quantity;
    }

    public void updateDraft(Instant startsAt, Instant endsAt, int quota) {
        ensureStatus(CampaignStatus.DRAFT, "FLASH_SALE_CAMPAIGN_NOT_DRAFT", "Only draft campaigns can be updated");
        validateWindow(startsAt, endsAt);
        validateQuota(quota);
        if (soldQuota > 0 || reservedQuota > 0) {
            throw new ConflictException(
                    "FLASH_SALE_CAMPAIGN_ALREADY_USED",
                    "Campaign with existing reservations or sales cannot be edited as draft"
            );
        }
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.quota = quota;
    }

    public void activate() {
        ensureStatus(CampaignStatus.DRAFT, "FLASH_SALE_CAMPAIGN_NOT_DRAFT", "Only draft campaigns can be activated");
        validateWindow(startsAt, endsAt);
        this.status = CampaignStatus.ACTIVE;
    }

    public void end() {
        if (status == CampaignStatus.ENDED) {
            throw new ConflictException("FLASH_SALE_CAMPAIGN_ALREADY_ENDED", "Campaign is already ended");
        }
        this.status = CampaignStatus.ENDED;
    }

    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getQuota() {
        return quota;
    }

    public int getReservedQuota() {
        return reservedQuota;
    }

    public int getSoldQuota() {
        return soldQuota;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    private static void validateWindow(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new ConflictException("FLASH_SALE_INVALID_WINDOW", "Campaign start time must be before end time");
        }
    }

    private static void validateQuota(int quota) {
        if (quota <= 0) {
            throw new ConflictException("FLASH_SALE_INVALID_QUOTA", "Campaign quota must be greater than zero");
        }
    }

    private void ensureStatus(CampaignStatus requiredStatus, String code, String message) {
        if (status != requiredStatus) {
            throw new ConflictException(code, message);
        }
    }
}
