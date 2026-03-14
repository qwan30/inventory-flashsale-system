package com.codex.flashsale.scheduler;

import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.inventory.InventoryService;
import com.codex.flashsale.inventory.StockReservation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final InventoryService inventoryService;
    private final ReservationApplicationService reservationApplicationService;
    private final TimeProvider timeProvider;

    public ReservationExpiryScheduler(
            InventoryService inventoryService,
            ReservationApplicationService reservationApplicationService,
            TimeProvider timeProvider
    ) {
        this.inventoryService = inventoryService;
        this.reservationApplicationService = reservationApplicationService;
        this.timeProvider = timeProvider;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.expired-reservation-delay:30s}")
    public void expireReservations() {
        List<StockReservation> expiredReservations = inventoryService.findExpiredActiveReservations(timeProvider.now());
        for (StockReservation expiredReservation : expiredReservations) {
            try {
                reservationApplicationService.expireReservation(expiredReservation.getId());
            } catch (RuntimeException exception) {
                log.warn("Failed to expire reservation {}", expiredReservation.getId(), exception);
            }
        }
    }
}
