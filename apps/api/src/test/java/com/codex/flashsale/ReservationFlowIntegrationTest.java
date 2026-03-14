package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.api.ConfirmReservationResponse;
import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.OrderResponse;
import com.codex.flashsale.api.ReservationResponse;
import com.codex.flashsale.application.OrderApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.exception.ConflictException;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.inventory.InventoryItem;
import com.codex.flashsale.order.OrderStatus;
import com.codex.flashsale.outbox.OutboxEvent;
import com.codex.flashsale.outbox.OutboxService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.lock.wait-timeout=30s",
        "app.lock.lease-timeout=30s"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReservationApplicationService reservationApplicationService;

    @Autowired
    private OrderApplicationService orderApplicationService;

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
    void shouldReserveAndConfirmSuccessfully() {
        ReservationResponse reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 2),
                "reserve-key-1"
        );

        ConfirmReservationResponse confirmed = reservationApplicationService.confirm(
                reservation.reservationId(),
                "confirm-key-1"
        );

        InventoryItem inventoryItem = inventoryItemRepository.findById(BASE_SKU).orElseThrow();

        assertThat(reservation.status().name()).isEqualTo("ACTIVE");
        assertThat(confirmed.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(inventoryItem.getAvailableQty()).isEqualTo(18);
        assertThat(inventoryItem.getReservedQty()).isZero();
        assertThat(inventoryItem.getSoldQty()).isEqualTo(2);
    }

    @Test
    void shouldReturnSameReservationForDuplicateIdempotencyKey() {
        ReservationResponse first = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.APP, 1),
                "duplicate-key"
        );
        ReservationResponse second = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.APP, 1),
                "duplicate-key"
        );

        assertThat(first.reservationId()).isEqualTo(second.reservationId());
        assertThat(stockReservationRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectReservationOutsideCampaignWindow() {
        resetDatabase(
                20,
                20,
                Instant.now().minus(Duration.ofHours(2)),
                Instant.now().minus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );

        assertThatThrownBy(() -> reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "reserve-outside-window"
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("outside the active window");
    }

    @Test
    void shouldHandleConfirmIdempotencyAndRejectDifferentSecondConfirmKey() {
        ReservationResponse reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.SHOPEE, 1),
                "reserve-confirm-idempotent"
        );

        ConfirmReservationResponse firstConfirm = reservationApplicationService.confirm(
                reservation.reservationId(),
                "confirm-same-key"
        );
        ConfirmReservationResponse secondConfirm = reservationApplicationService.confirm(
                reservation.reservationId(),
                "confirm-same-key"
        );

        assertThat(firstConfirm.orderId()).isEqualTo(secondConfirm.orderId());
        assertThatThrownBy(() -> reservationApplicationService.confirm(
                reservation.reservationId(),
                "confirm-other-key"
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldPreventOversellUnderConcurrency() throws Exception {
        resetDatabase(
                10,
                10,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );

        int requestCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requestCount);
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> successes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < requestCount; i++) {
            int requestIndex = i;
            executorService.submit(() -> {
                ready.countDown();
                try {
                    start.await(10, TimeUnit.SECONDS);
                    ReservationResponse response = reservationApplicationService.reserve(
                            BASE_CAMPAIGN_ID,
                            new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                            "concurrency-key-" + requestIndex
                    );
                    successes.add(response.reservationId());
                } catch (Throwable throwable) {
                    failures.add(throwable);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executorService.shutdownNow();

        InventoryItem inventoryItem = inventoryItemRepository.findById(BASE_SKU).orElseThrow();

        assertThat(successes).hasSize(10);
        assertThat(Set.copyOf(successes)).hasSize(10);
        assertThat(inventoryItem.getAvailableQty()).isZero();
        assertThat(inventoryItem.getReservedQty()).isEqualTo(10);
        assertThat(inventoryItem.getSoldQty()).isZero();
        assertThat(failures).allSatisfy(throwable ->
                assertThat(throwable).isInstanceOf(ConflictException.class)
        );
    }

    @Test
    void shouldPublishOutboxEventsExactlyOncePerStateChange() {
        ReservationResponse reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 2),
                "outbox-reserve"
        );
        ConfirmReservationResponse confirmed = reservationApplicationService.confirm(reservation.reservationId(), "outbox-confirm");
        OrderResponse paid = orderApplicationService.updateStatus(confirmed.orderId(), OrderStatus.PAID);
        OrderResponse shipped = orderApplicationService.updateStatus(paid.orderId(), OrderStatus.SHIPPED);

        Consumer<String, String> consumer = createConsumer();
        consumer.subscribe(List.of("inventory-flashsale.events"));

        int publishedFirstPass = outboxService.publishPendingEvents();
        int publishedSecondPass = outboxService.publishPendingEvents();
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        List<String> eventTypes = outboxEventRepository.findAll().stream()
                .map(OutboxEvent::getEventType)
                .toList();

        assertThat(publishedFirstPass).isEqualTo(4);
        assertThat(publishedSecondPass).isZero();
        assertThat(records.count()).isEqualTo(4);
        assertThat(eventTypes).containsExactlyInAnyOrder(
                "inventory.reservation.created",
                "order.created",
                "order.paid",
                "order.shipped"
        );
        assertThat(outboxEventRepository.findAll()).allSatisfy(event ->
                assertThat(event.getStatus().name()).isEqualTo("PUBLISHED")
        );
        consumer.close();
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "integration-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );
        return new KafkaConsumer<>(props);
    }
}
