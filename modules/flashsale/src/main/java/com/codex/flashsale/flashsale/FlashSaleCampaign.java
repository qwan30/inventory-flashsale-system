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

@Entity
@Table(name = "flash_sale_campaign")
public class FlashSaleCampaign extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "quota", nullable = false)
    private int quota;

    @Column(name = "reserved_quota", nullable = false)
    private int reservedQuota;

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

    public void ensureActive(Instant now) {
        if (status != CampaignStatus.ACTIVE) {
            throw new ConflictException("FLASH_SALE_INACTIVE", "Flash sale campaign is not active");
        }
        if (now.isBefore(startsAt) || now.isAfter(endsAt)) {
            throw new ConflictException("FLASH_SALE_OUT_OF_WINDOW", "Flash sale campaign is outside the active window");
        }
    }

    public void reserveQuota(int quantity) {
        int remaining = quota - reservedQuota - soldQuota;
        if (remaining < quantity) {
            throw new ConflictException("FLASH_SALE_QUOTA_EXCEEDED", "Flash sale quota is exhausted");
        }
        reservedQuota += quantity;
    }

    public void releaseQuota(int quantity) {
        reservedQuota = Math.max(0, reservedQuota - quantity);
    }

    public void confirmQuota(int quantity) {
        if (reservedQuota < quantity) {
            throw new ConflictException("FLASH_SALE_QUOTA_CONFLICT", "Reserved quota is insufficient to confirm");
        }
        reservedQuota -= quantity;
        soldQuota += quantity;
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
}
