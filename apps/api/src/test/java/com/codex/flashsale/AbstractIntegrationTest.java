package com.codex.flashsale;

import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.flashsale.FlashSaleCampaign;
import com.codex.flashsale.flashsale.FlashSaleCampaignRepository;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.inventory.InventoryItemRepository;
import com.codex.flashsale.inventory.StockReservationRepository;
import com.codex.flashsale.order.OrderHeaderRepository;
import com.codex.flashsale.outbox.OutboxEventRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
abstract class AbstractIntegrationTest {

    protected static final String BASE_CAMPAIGN_ID = "campaign-test-001";
    protected static final String BASE_SKU = "SKU-TEST-001";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("flashsale")
            .withUsername("flashsale")
            .withPassword("flashsale");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    protected void resetDatabase(int availableQty, int quota, Instant startsAt, Instant endsAt, CampaignStatus status) {
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
