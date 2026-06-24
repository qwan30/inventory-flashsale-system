package com.codex.flashsale.inventory;

import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Represents the physical or logical inventory for a specific SKU.
 * Invariants:
 * - availableQty >= 0
 * - reservedQty >= 0
 * - soldQty >= 0
 * 
 * Uses optimistic locking via the {@code version} field to handle concurrent reservation
 * and release operations safely without requiring pessimistic database locks.
 */
@Entity
@Table(name = "inventory_item")
public class InventoryItem extends AuditTimestamps {

    @Id
    private String sku;

    /** Quantity available for new reservations or immediate purchase. */
    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    /** Quantity currently reserved by active campaigns or checkouts. */
    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    /** Total quantity successfully sold/confirmed. */
    @Column(name = "sold_qty", nullable = false)
    private int soldQty;

    /** Version field for JPA optimistic locking to prevent double-reservations under concurrency. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, int availableQty, int reservedQty, int soldQty) {
        this.sku = sku;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.soldQty = soldQty;
    }

    /**
     * Reserves a specific quantity of this SKU.
     * Decreases availableQty and increases reservedQty.
     * 
     * @param quantity the amount to reserve
     * @throws ConflictException if there is insufficient available stock
     */
    public void reserve(int quantity) {
        if (availableQty < quantity) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Insufficient stock for SKU " + sku);
        }
        availableQty -= quantity;
        reservedQty += quantity;
    }

    /**
     * Releases a previously reserved quantity back to available stock.
     * Decreases reservedQty and increases availableQty.
     * 
     * @param quantity the amount to release
     * @throws ConflictException if the reserved quantity is less than the release amount
     */
    public void release(int quantity) {
        if (reservedQty < quantity) {
            throw new ConflictException("RESERVATION_STOCK_CONFLICT", "Reserved quantity is insufficient for release");
        }
        reservedQty -= quantity;
        availableQty += quantity;
    }

    /**
     * Confirms the sale of a reserved quantity, converting it from reserved to sold.
     * Decreases reservedQty and increases soldQty.
     * 
     * @param quantity the amount to confirm
     * @throws ConflictException if the reserved quantity is less than the confirmation amount
     */
    public void confirm(int quantity) {
        if (reservedQty < quantity) {
            throw new ConflictException("RESERVATION_CONFIRM_CONFLICT", "Reserved quantity is insufficient to confirm");
        }
        reservedQty -= quantity;
        soldQty += quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public int getSoldQty() {
        return soldQty;
    }

    public long getVersion() {
        return version;
    }
}

