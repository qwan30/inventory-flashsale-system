package com.codex.flashsale.inventory;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

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

    public InventoryItem getRequiredInventory(String sku) {
        return inventoryItemRepository.findById(sku)
                .orElseThrow(() -> new NotFoundException("INVENTORY_NOT_FOUND", "Inventory not found for SKU " + sku));
    }

    public StockReservation getRequiredReservation(String reservationId) {
        return stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("RESERVATION_NOT_FOUND", "Reservation not found: " + reservationId));
    }

    public Optional<StockReservation> findByIdempotencyKey(String idempotencyKey) {
        return stockReservationRepository.findByIdempotencyKey(idempotencyKey);
    }

    public InventoryItem saveInventory(InventoryItem inventoryItem) {
        return inventoryItemRepository.saveAndFlush(inventoryItem);
    }

    public List<InventoryItem> findAllInventoryItems() {
        return inventoryItemRepository.findAll();
    }

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

    public StockReservation saveReservation(StockReservation reservation) {
        return stockReservationRepository.saveAndFlush(reservation);
    }

    public List<StockReservation> findExpiredActiveReservations(Instant now) {
        return stockReservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, now);
    }
}
