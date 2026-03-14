package com.codex.flashsale.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHeaderRepository extends JpaRepository<OrderHeader, String> {

    Optional<OrderHeader> findByReservationId(String reservationId);
}

