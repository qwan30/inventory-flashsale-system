package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
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
        "app.scheduler.channel-sync-delay=1h",
        "app.channel.retry-delay=0s"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OpsAndChannelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReservationApplicationService reservationApplicationService;

    @Autowired
    private ChannelSyncService channelSyncService;

    @Autowired
    private OpsApplicationService opsApplicationService;

    @Autowired
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        resetDatabase(
                20,
                20,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );
    }

    @Test
    void shouldPublishInventorySyncAttemptsForAllChannelsAndPersistSnapshots() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 2),
                "channel-sync-reserve"
        );

        int publishedCount = channelSyncService.publishPendingAttempts();

        assertThat(publishedCount).isEqualTo(3);
        assertThat(channelSyncAttemptRepository.count()).isEqualTo(3);
        assertThat(channelSyncAttemptRepository.findAll())
                .allSatisfy(attempt -> assertThat(attempt.getStatus().name()).isEqualTo("SYNCED"));
        assertThat(channelInventorySnapshotRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldRetryTransientChannelSyncFailureAndEventuallyPersistSnapshots() {
        String transientSku = "SKU-TRANSIENT-001";
        String transientCampaignId = "campaign-transient-001";
        inventoryItemRepository.saveAndFlush(new InventoryItem(transientSku, 10, 0, 0));
        flashSaleCampaignRepository.saveAndFlush(new FlashSaleCampaign(
                transientCampaignId,
                transientSku,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                10,
                0,
                0,
                CampaignStatus.ACTIVE
        ));

        reservationApplicationService.reserve(
                transientCampaignId,
                new CreateReservationRequest(transientSku, SalesChannel.APP, 1),
                "channel-sync-transient"
        );

        int failedFirstPass = channelSyncService.publishPendingAttempts();
        int retriedCount = channelSyncService.retryFailedAttempts();
        int publishedSecondPass = channelSyncService.publishPendingAttempts();

        assertThat(failedFirstPass).isEqualTo(3);
        assertThat(retriedCount).isEqualTo(3);
        assertThat(publishedSecondPass).isEqualTo(3);
        assertThat(channelInventorySnapshotRepository.count()).isEqualTo(3);
        assertThat(channelSyncAttemptRepository.findAll())
                .allSatisfy(attempt -> assertThat(attempt.getStatus().name()).isEqualTo("SYNCED"));
    }

    @Test
    void shouldDetectAndResolveInventoryDrift() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "drift-seed"
        );
        channelSyncService.publishPendingAttempts();

        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.APP, 1),
                "drift-introduce"
        );

        ReconciliationRunResponse run = opsApplicationService.runReconciliation();

        assertThat(run.openDriftCount()).isEqualTo(3);
        assertThat(run.scannedSnapshotCount()).isEqualTo(3);

        ReconciliationDriftResponse drift = opsApplicationService.listOpenReconciliationDrifts().getFirst();
        ReconciliationDriftResponse resolved = opsApplicationService.resolveReconciliationDrift(
                drift.driftId(),
                "Snapshot stale after new reservation"
        );

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(opsApplicationService.listOpenReconciliationDrifts()).hasSize(2);
    }

    @Test
    void shouldRetryFailedOutboxEventFromOpsSurface() {
        OutboxEvent event = new OutboxEvent("evt-failed-1", "reservation", "res-1", "inventory.reservation.created", "{\"ok\":true}");
        event.markFailed("broker down", Instant.now(), Duration.ofSeconds(5), 5);
        outboxService.save(event);

        OutboxRetryResponse response = opsApplicationService.retryOutboxEvent(event.getId());

        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(response.lastError()).isNull();
    }
}
