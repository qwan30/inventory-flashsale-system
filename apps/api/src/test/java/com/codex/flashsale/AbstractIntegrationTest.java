package com.codex.flashsale;

import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.flashsale.FlashSaleCampaignRepository;
import com.codex.flashsale.admin.AdminActivityAuditRepository;
import com.codex.flashsale.admin.AdminRefreshTokenRepository;
import com.codex.flashsale.alerts.AlertDeliveryStateRepository;
import com.codex.flashsale.channel.ingress.TikTokIngressReceiptRepository;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationDriftRepository;
import com.codex.flashsale.channel.reconciliation.InventoryReconciliationRunRepository;
import com.codex.flashsale.channel.sync.ChannelInventorySnapshotRepository;
import com.codex.flashsale.channel.sync.ChannelSyncAttemptRepository;
import com.codex.flashsale.idempotency.OperationIdempotencyRepository;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryItemRepository;
import com.codex.flashsale.inventory.StockReservationRepository;
import com.codex.flashsale.order.OrderHeaderRepository;
import com.codex.flashsale.outbox.OutboxEventRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class AbstractIntegrationTest {

    protected static final String BASE_CAMPAIGN_ID = "campaign-test-001";
    protected static final String BASE_SKU = "SKU-TEST-001";

    @Autowired
    protected InventoryItemRepository inventoryItemRepository;

    @Autowired
    protected FlashSaleCampaignRepository flashSaleCampaignRepository;

    @Autowired
    protected StockReservationRepository stockReservationRepository;

    @Autowired
    protected OrderHeaderRepository orderHeaderRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @Autowired
    protected OperationIdempotencyRepository operationIdempotencyRepository;

    @Autowired
    protected ChannelSyncAttemptRepository channelSyncAttemptRepository;

    @Autowired
    protected ChannelInventorySnapshotRepository channelInventorySnapshotRepository;

    @Autowired
    protected InventoryReconciliationRunRepository inventoryReconciliationRunRepository;

    @Autowired
    protected InventoryReconciliationDriftRepository inventoryReconciliationDriftRepository;

    @Autowired
    protected AlertDeliveryStateRepository alertDeliveryStateRepository;

    @Autowired
    protected AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Autowired
    protected AdminActivityAuditRepository adminActivityAuditRepository;

    @Autowired
    protected TikTokIngressReceiptRepository tikTokIngressReceiptRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedContainers.MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", SharedContainers.MYSQL::getUsername);
        registry.add("spring.datasource.password", SharedContainers.MYSQL::getPassword);
        registry.add("spring.data.redis.host", SharedContainers.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> SharedContainers.REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", SharedContainers.KAFKA::getBootstrapServers);
    }

    protected void resetDatabase(int availableQty, int quota, Instant startsAt, Instant endsAt, CampaignStatus status) {
        alertDeliveryStateRepository.deleteAll();
        adminActivityAuditRepository.deleteAll();
        adminRefreshTokenRepository.deleteAll();
        tikTokIngressReceiptRepository.deleteAll();
        inventoryReconciliationDriftRepository.deleteAll();
        inventoryReconciliationRunRepository.deleteAll();
        channelInventorySnapshotRepository.deleteAll();
        channelSyncAttemptRepository.deleteAll();
        operationIdempotencyRepository.deleteAll();
        orderHeaderRepository.deleteAll();
        stockReservationRepository.deleteAll();
        flashSaleCampaignRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        outboxEventRepository.deleteAll();

        inventoryItemRepository.saveAndFlush(new InventoryItem(BASE_SKU, availableQty, 0, 0));
        flashSaleCampaignRepository.saveAndFlush(new FlashSaleCampaign(
                BASE_CAMPAIGN_ID,
                BASE_SKU,
                startsAt,
                endsAt,
                quota,
                0,
                0,
                status
        ));
    }
}
