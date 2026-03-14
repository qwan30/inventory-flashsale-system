package com.codex.flashsale.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

    Optional<StockReservation> findByIdempotencyKey(String idempotencyKey);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant expiresAt);
}

