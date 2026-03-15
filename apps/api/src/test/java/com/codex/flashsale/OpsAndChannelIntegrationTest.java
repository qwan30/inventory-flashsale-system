package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshot;
import com.codex.flashsale.channel.sync.ChannelSyncAttempt;
import com.codex.flashsale.channel.sync.ChannelSyncFailureType;
import com.codex.flashsale.channel.sync.ChannelSyncStatus;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import com.codex.flashsale.scheduler.ReconciliationScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.scheduler.channel-sync-delay=1h",
        "app.scheduler.reconciliation-delay=1h",
        "app.scheduler.alert-delivery-delay=1h",
        "app.channel.retry-delay=0s",
        "app.alerts.outbox-failed-threshold=1",
        "app.alerts.channel-sync-failed-threshold=1",
        "app.alerts.reconciliation-open-drift-threshold=1",
        "app.alerts.channel-snapshot-staleness=1s"
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

    @Autowired
    private ReconciliationScheduler reconciliationScheduler;

    @Autowired
    private ChannelReconciliationService channelReconciliationService;

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

        assertThat(run.triggerType()).isEqualTo("MANUAL");
        assertThat(run.status()).isEqualTo("COMPLETED");
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

    @Test
    void shouldCreateScheduledReconciliationRunWithCompletedStatus() {
        reconciliationScheduler.runScheduledReconciliation();

        var scheduledRun = inventoryReconciliationRunRepository.findAll().getFirst();

        assertThat(scheduledRun.getTriggerType().name()).isEqualTo("SCHEDULED");
        assertThat(scheduledRun.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(scheduledRun.getScannedSkuCount()).isEqualTo(1);
        assertThat(scheduledRun.getOpenDriftCount()).isZero();
    }

    @Test
    void shouldRefreshOpenDriftsInsteadOfDuplicatingAcrossScheduledRuns() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "scheduled-drift-seed"
        );
        channelSyncService.publishPendingAttempts();

        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.APP, 1),
                "scheduled-drift-introduce"
        );

        reconciliationScheduler.runScheduledReconciliation();
        reconciliationScheduler.runScheduledReconciliation();

        assertThat(channelReconciliationService.listOpenDrifts()).hasSize(3);
        assertThat(inventoryReconciliationRunRepository.findAll())
                .extracting(run -> run.getStatus().name())
                .containsOnly("COMPLETED");
    }

    @Test
    void shouldExposeInactiveAlertsForHealthyState() {
        Map<String, OpsAlertResponse> alerts = opsApplicationService.getAlerts().stream()
                .collect(java.util.stream.Collectors.toMap(OpsAlertResponse::code, alert -> alert));

        assertThat(alerts.values()).allSatisfy(alert -> assertThat(alert.status().name()).isEqualTo("INACTIVE"));
    }

    @Test
    void shouldActivateBacklogDriftAndStalenessAlerts() {
        OutboxEvent failedOutbox = new OutboxEvent("evt-failed-2", "reservation", "res-2", "inventory.reservation.created", "{\"ok\":true}");
        failedOutbox.markFailed("broker down", Instant.now(), Duration.ofSeconds(5), 5);
        outboxService.save(failedOutbox);

        ChannelSyncAttempt failedAttempt = new ChannelSyncAttempt(
                "sync-failed-1",
                failedOutbox.getId(),
                SalesChannel.WEB,
                BASE_SKU,
                "inventory.reservation.created",
                failedOutbox.getPayload(),
                10,
                0,
                0
        );
        failedAttempt.markFailed(ChannelSyncFailureType.PERMANENT, "connector down", Instant.now(), Duration.ofSeconds(1), 3);
        channelSyncAttemptRepository.saveAndFlush(failedAttempt);

        ChannelInventorySnapshot staleSnapshot = new ChannelInventorySnapshot(
                ChannelInventorySnapshot.snapshotId(SalesChannel.WEB, BASE_SKU),
                SalesChannel.WEB,
                BASE_SKU,
                10,
                0,
                0,
                failedOutbox.getId(),
                Instant.now().minus(Duration.ofMinutes(10))
        );
        channelInventorySnapshotRepository.saveAndFlush(staleSnapshot);

        var run = channelReconciliationService.startRun(
                com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType.MANUAL
        );
        channelReconciliationService.recordDrift(
                run.getId(),
                SalesChannel.WEB,
                BASE_SKU,
                9,
                1,
                0,
                10,
                0,
                0
        );

        Map<String, OpsAlertResponse> alerts = opsApplicationService.getAlerts().stream()
                .collect(java.util.stream.Collectors.toMap(OpsAlertResponse::code, alert -> alert));

        assertThat(alerts.get("OUTBOX_FAILED_BACKLOG").status().name()).isEqualTo("ACTIVE");
        assertThat(alerts.get("CHANNEL_SYNC_FAILED_BACKLOG").status().name()).isEqualTo("ACTIVE");
        assertThat(alerts.get("RECONCILIATION_OPEN_DRIFTS").status().name()).isEqualTo("ACTIVE");
        assertThat(alerts.get("STALE_CHANNEL_SNAPSHOTS").status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldExposeFailedScheduledRunThroughAlerts() {
        var run = channelReconciliationService.startRun(
                com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType.SCHEDULED
        );
        channelReconciliationService.failRun(run.getId(), "synthetic failure");

        Map<String, OpsAlertResponse> alerts = opsApplicationService.getAlerts().stream()
                .collect(java.util.stream.Collectors.toMap(OpsAlertResponse::code, alert -> alert));

        assertThat(alerts.get("RECONCILIATION_RUN_FAILURE").status().name()).isEqualTo("ACTIVE");
        assertThat(alerts.get("RECONCILIATION_RUN_FAILURE").message()).contains("synthetic failure");
    }
}
