package com.codex.flashsale.api;

import com.codex.flashsale.inventory.ReservationStatus;

public record ReleaseReservationResponse(
        String reservationId,
        ReservationStatus status,
        InventoryResponse inventory
) {
}

