package com.codex.flashsale.inventory;

import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_item")
public class InventoryItem extends AuditTimestamps {

    @Id
    private String sku;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "sold_qty", nullable = false)
    private int soldQty;

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

    public void reserve(int quantity) {
        if (availableQty < quantity) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Insufficient stock for SKU " + sku);
        }
        availableQty -= quantity;
        reservedQty += quantity;
    }

    public void release(int quantity) {
        if (reservedQty < quantity) {
            throw new ConflictException("RESERVATION_STOCK_CONFLICT", "Reserved quantity is insufficient for release");
        }
        reservedQty -= quantity;
        availableQty += quantity;
    }

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

