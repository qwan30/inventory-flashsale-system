package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.ReleaseReservationResponse;
import com.codex.flashsale.api.ReservationResponse;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.ReservationStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.lock.wait-timeout=30s",
        "app.lock.lease-timeout=30s",
        "app.reservation.ttl=1s"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationExpiryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReservationApplicationService reservationApplicationService;

    @BeforeEach
    void setUp() {
        resetDatabase(
                5,
                5,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );
    }

    @Test
    void shouldReleaseExpiredReservationBackToInventory() throws Exception {
        ReservationResponse reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.APP, 2),
                "expiry-test-reserve"
        );

        Thread.sleep(1_250L);
        reservationApplicationService.expireReservation(reservation.reservationId());

        InventoryItem inventoryItem = inventoryItemRepository.findById(BASE_SKU).orElseThrow();
        ReleaseReservationResponse response = reservationApplicationService.release(reservation.reservationId());

        assertThat(response.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(inventoryItem.getAvailableQty()).isEqualTo(5);
        assertThat(inventoryItem.getReservedQty()).isZero();
        assertThat(inventoryItem.getSoldQty()).isZero();
    }
}
