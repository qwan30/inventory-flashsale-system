package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OutboxRetryResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.application.ChannelHealthStatus;
import com.codex.flashsale.application.ChannelHealthSummary;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.ingress.TikTokIngressReceipt;
import com.codex.flashsale.channel.reconciliation.ChannelReconciliationService;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshot;
import com.codex.flashsale.channel.sync.ChannelSyncAttempt;
import com.codex.flashsale.channel.sync.ChannelSyncFailureType;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.config.ApplicationProperties;
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

    @Autowired
    private ApplicationProperties applicationProperties;

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

        assertThat(publishedCount).isEqualTo(4);
        assertThat(channelSyncAttemptRepository.count()).isEqualTo(4);
        assertThat(channelSyncAttemptRepository.findAll())
                .allSatisfy(attempt -> assertThat(attempt.getStatus().name()).isEqualTo("SYNCED"));
        assertThat(channelInventorySnapshotRepository.count()).isEqualTo(4);
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

        assertThat(failedFirstPass).isEqualTo(4);
        assertThat(retriedCount).isEqualTo(4);
        assertThat(publishedSecondPass).isEqualTo(4);
        assertThat(channelInventorySnapshotRepository.count()).isEqualTo(4);
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
        assertThat(run.openDriftCount()).isEqualTo(4);
        assertThat(run.scannedSnapshotCount()).isEqualTo(4);

        ReconciliationDriftResponse drift = opsApplicationService.listOpenReconciliationDrifts().getFirst();
        ReconciliationDriftResponse resolved = opsApplicationService.resolveReconciliationDrift(
                drift.driftId(),
                "Snapshot stale after new reservation"
        );

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(opsApplicationService.listOpenReconciliationDrifts()).hasSize(3);
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

        assertThat(channelReconciliationService.listOpenDrifts()).hasSize(4);
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

    @Test
    void shouldReturnHealthyChannelHealthSummariesWhenSignalsAreClean() {
        Map<SalesChannel, ChannelHealthSummary> summaries = opsApplicationService.listChannelHealthSummaries().stream()
                .collect(java.util.stream.Collectors.toMap(ChannelHealthSummary::channel, summary -> summary));

        assertThat(summaries.keySet()).containsExactlyInAnyOrder(SalesChannel.SHOPEE, SalesChannel.TIKTOK_SHOP);
        assertThat(summaries.values()).allSatisfy(summary -> {
            assertThat(summary.status()).isEqualTo(ChannelHealthStatus.HEALTHY);
            assertThat(summary.configValid()).isTrue();
            assertThat(summary.syncBacklogCount()).isZero();
            assertThat(summary.staleSnapshotCount()).isZero();
            assertThat(summary.openDriftCount()).isZero();
            assertThat(summary.latestReplay()).isNull();
            assertThat(summary.lastReconciliationAt()).isNull();
        });
        assertThat(summaries.get(SalesChannel.TIKTOK_SHOP).latestIngressReceipt()).isNull();
    }

    @Test
    void shouldReturnDegradedSummaryWithBacklogStalenessDriftAndIngressReceipt() {
        OutboxEvent failedOutbox = new OutboxEvent(
                "evt-tiktok-health-1",
                "reservation",
                "res-tiktok-health-1",
                "inventory.reservation.created",
                "{\"ok\":true}"
        );
        failedOutbox.markFailed("connector down", Instant.now(), Duration.ofSeconds(5), 5);
        outboxService.save(failedOutbox);

        ChannelSyncAttempt pendingAttempt = new ChannelSyncAttempt(
                "sync-tiktok-pending",
                failedOutbox.getId(),
                SalesChannel.TIKTOK_SHOP,
                BASE_SKU,
                "inventory.reservation.created",
                failedOutbox.getPayload(),
                10,
                0,
                0
        );
        channelSyncAttemptRepository.saveAndFlush(pendingAttempt);

        ChannelSyncAttempt failedAttempt = new ChannelSyncAttempt(
                "sync-tiktok-failed",
                failedOutbox.getId(),
                SalesChannel.TIKTOK_SHOP,
                BASE_SKU,
                "inventory.reservation.created",
                failedOutbox.getPayload(),
                10,
                0,
                0
        );
        failedAttempt.markFailed(ChannelSyncFailureType.PERMANENT, "partner failure", Instant.now(), Duration.ofSeconds(1), 3);
        channelSyncAttemptRepository.saveAndFlush(failedAttempt);

        ChannelInventorySnapshot staleSnapshot = new ChannelInventorySnapshot(
                ChannelInventorySnapshot.snapshotId(SalesChannel.TIKTOK_SHOP, BASE_SKU),
                SalesChannel.TIKTOK_SHOP,
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
                SalesChannel.TIKTOK_SHOP,
                BASE_SKU,
                9,
                1,
                0,
                10,
                0,
                0
        );

        tikTokIngressReceiptRepository.saveAndFlush(new TikTokIngressReceipt(
                "TIKTOK_SHOP:INVENTORY:receipt-channel-health",
                SalesChannel.TIKTOK_SHOP,
                "INVENTORY",
                "receipt-channel-health",
                "hash",
                "PROCESSED",
                Instant.now().minus(Duration.ofMinutes(1))
        ));

        Map<SalesChannel, ChannelHealthSummary> summaries = opsApplicationService.listChannelHealthSummaries().stream()
                .collect(java.util.stream.Collectors.toMap(ChannelHealthSummary::channel, summary -> summary));

        ChannelHealthSummary tikTokSummary = summaries.get(SalesChannel.TIKTOK_SHOP);
        assertThat(tikTokSummary.status()).isEqualTo(ChannelHealthStatus.DEGRADED);
        assertThat(tikTokSummary.syncBacklogCount()).isEqualTo(2);
        assertThat(tikTokSummary.staleSnapshotCount()).isEqualTo(1);
        assertThat(tikTokSummary.openDriftCount()).isEqualTo(1);
        assertThat(tikTokSummary.lastReconciliationAt()).isNotNull();
        assertThat(tikTokSummary.latestIngressReceipt()).isNotNull();
        assertThat(tikTokSummary.latestIngressReceipt().externalReceiptId()).isEqualTo("receipt-channel-health");
        assertThat(tikTokSummary.latestReplay()).isNull();

        ChannelHealthSummary shopeeSummary = summaries.get(SalesChannel.SHOPEE);
        assertThat(shopeeSummary.status()).isEqualTo(ChannelHealthStatus.HEALTHY);
    }

    @Test
    void shouldReturnUnavailableWhenConnectorModeIsRealButConfigIsInvalid() {
        String originalShopeeMode = applicationProperties.getChannel().getShopee().getMode();
        Long originalShopeePartnerId = applicationProperties.getChannel().getShopee().getPartnerId();
        String originalTikTokMode = applicationProperties.getChannel().getTikTok().getMode();
        String originalTikTokAppKey = applicationProperties.getChannel().getTikTok().getAppKey();
        try {
            applicationProperties.getChannel().getShopee().setMode("real");
            applicationProperties.getChannel().getShopee().setPartnerId(null);
            applicationProperties.getChannel().getTikTok().setMode("real");
            applicationProperties.getChannel().getTikTok().setAppKey(null);

            Map<SalesChannel, ChannelHealthSummary> summaries = opsApplicationService.listChannelHealthSummaries().stream()
                    .collect(java.util.stream.Collectors.toMap(ChannelHealthSummary::channel, summary -> summary));

            assertThat(summaries.get(SalesChannel.SHOPEE).status()).isEqualTo(ChannelHealthStatus.UNAVAILABLE);
            assertThat(summaries.get(SalesChannel.SHOPEE).configValid()).isFalse();
            assertThat(summaries.get(SalesChannel.TIKTOK_SHOP).status()).isEqualTo(ChannelHealthStatus.UNAVAILABLE);
            assertThat(summaries.get(SalesChannel.TIKTOK_SHOP).configValid()).isFalse();
        } finally {
            applicationProperties.getChannel().getShopee().setMode(originalShopeeMode);
            applicationProperties.getChannel().getShopee().setPartnerId(originalShopeePartnerId);
            applicationProperties.getChannel().getTikTok().setMode(originalTikTokMode);
            applicationProperties.getChannel().getTikTok().setAppKey(originalTikTokAppKey);
        }
    }
}
