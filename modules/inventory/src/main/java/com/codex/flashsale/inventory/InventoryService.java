package com.codex.flashsale.inventory;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating inventory items and stock reservations.
 * Handles persistence and retrieval, serving as a boundary layer
 * above the repository interfaces.
 */
@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;

    public InventoryService(
            InventoryItemRepository inventoryItemRepository,
            StockReservationRepository stockReservationRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockReservationRepository = stockReservationRepository;
    }

    /**
     * Retrieves the inventory item for the specified SKU or throws an exception.
     */
    public InventoryItem getRequiredInventory(String sku) {
        return inventoryItemRepository.findById(sku)
                .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found for SKU " + sku));
    }

    /**
     * Retrieves the stock reservation for the specified ID or throws an exception.
     */
    public StockReservation getRequiredReservation(String reservationId) {
        return stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Reservation not found: " + reservationId));
    }

    /**
     * Finds a reservation by its idempotency key.
     * Used to prevent duplicate reservation creations.
     */
    public Optional<StockReservation> findByIdempotencyKey(String idempotencyKey) {
        return stockReservationRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * Saves or updates an inventory item and flushes changes to the database.
     */
    public InventoryItem saveInventory(InventoryItem inventoryItem) {
        return inventoryItemRepository.saveAndFlush(inventoryItem);
    }

    /**
     * Retrieves all inventory items in the system.
     */
    public List<InventoryItem> findAllInventoryItems() {
        return inventoryItemRepository.findAll();
    }

    /**
     * Creates and persists a new active stock reservation.
     */
    public StockReservation createReservation(
            String reservationId,
            String sku,
            String campaignId,
            SalesChannel channel,
            int quantity,
            String idempotencyKey,
            Instant expiresAt
    ) {
        StockReservation reservation = StockReservation.active(
                reservationId,
                sku,
                campaignId,
                channel,
                quantity,
                idempotencyKey,
                expiresAt
        );
        return stockReservationRepository.saveAndFlush(reservation);
    }

    /**
     * Saves or updates a stock reservation and flushes changes to the database.
     */
    public StockReservation saveReservation(StockReservation reservation) {
        return stockReservationRepository.saveAndFlush(reservation);
    }

    /**
     * Finds all ACTIVE reservations that have passed their expiration timestamp.
     */
    public List<StockReservation> findExpiredActiveReservations(Instant now) {
        return stockReservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, now);
    }
}
