package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.ReservationResponse;
import com.codex.flashsale.api.ReconciliationDriftResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.scheduler.channel-sync-delay=1h",
        "app.scheduler.reconciliation-delay=1h",
        "app.scheduler.alert-delivery-delay=1h",
        "app.channel.retry-delay=0s",
        "app.channel.shopee.mode=real",
        "app.channel.shopee.partner-id=123456",
        "app.channel.shopee.partner-key=test-partner-key",
        "app.channel.shopee.shop-id=456789",
        "app.channel.shopee.access-token=test-token",
        "app.channel.shopee.connect-timeout=1s",
        "app.channel.shopee.read-timeout=1s"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShopeeSandboxConnectorIntegrationTest extends AbstractIntegrationTest {

    private static final ShopeeStubServer SHOPEE_STUB = new ShopeeStubServer();

    static {
        SHOPEE_STUB.start();
    }

    @DynamicPropertySource
    static void shopeeProperties(DynamicPropertyRegistry registry) {
        registry.add("app.channel.shopee.base-url", SHOPEE_STUB::baseUrl);
    }

    @Autowired
    private ReservationApplicationService reservationApplicationService;

    @Autowired
    private ChannelSyncService channelSyncService;

    @Autowired
    private OpsApplicationService opsApplicationService;

    @BeforeEach
    void setUp() {
        resetDatabase(
                20,
                20,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );
        SHOPEE_STUB.reset(BASE_SKU, 1000L, 19, 1);
    }

    @AfterAll
    static void tearDownStub() {
        SHOPEE_STUB.stop();
    }

    @Test
    void shouldSyncShopeeInventoryThroughSandboxConnectorInRealMode() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.SHOPEE, 1),
                "shopee-real-sync"
        );

        int publishedAttempts = channelSyncService.publishPendingAttempts();

        assertThat(publishedAttempts).isEqualTo(4);
        assertThat(SHOPEE_STUB.countRequests("/api/v2/product/update_stock")).isEqualTo(1);
        assertThat(SHOPEE_STUB.firstQuery("/api/v2/product/update_stock"))
                .contains("partner_id=123456")
                .contains("shop_id=456789")
                .contains("access_token=test-token")
                .contains("sign=")
                .contains("timestamp=");
        assertThat(channelInventorySnapshotRepository.findByChannelAndSku(SalesChannel.SHOPEE, BASE_SKU)).isPresent();
    }

    @Test
    void shouldUseLiveShopeeStockDuringReconciliationInRealMode() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.SHOPEE, 1),
                "shopee-reconciliation-seed"
        );
        channelSyncService.publishPendingAttempts();

        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "shopee-reconciliation-drift"
        );

        SHOPEE_STUB.setRemoteStock(19, 1);

        ReconciliationRunResponse run = opsApplicationService.runReconciliation();
        List<ReconciliationDriftResponse> openDrifts = opsApplicationService.listOpenReconciliationDrifts();

        assertThat(run.openDriftCount()).isGreaterThanOrEqualTo(1);
        assertThat(SHOPEE_STUB.countRequests("/api/v2/product/get_item_base_info")).isGreaterThan(0);
        assertThat(openDrifts)
                .extracting(ReconciliationDriftResponse::channel)
                .contains(SalesChannel.SHOPEE);

        ReconciliationDriftResponse shopeeDrift = openDrifts.stream()
                .filter(drift -> drift.channel() == SalesChannel.SHOPEE)
                .findFirst()
                .orElseThrow();
        assertThat(shopeeDrift.observedInventory().soldQty()).isEqualTo(shopeeDrift.centralInventory().soldQty());
    }

    @Test
    void shouldIgnoreShopeeSoldOnlyDifferenceDuringReconciliationInRealMode() {
        ReservationResponse reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.SHOPEE, 1),
                "shopee-sold-diff-seed"
        );
        channelSyncService.publishPendingAttempts();

        reservationApplicationService.confirm(reservation.reservationId(), "shopee-confirm");
        channelSyncService.publishPendingAttempts();

        SHOPEE_STUB.setRemoteStock(19, 0);

        opsApplicationService.runReconciliation();
        List<ReconciliationDriftResponse> openDrifts = opsApplicationService.listOpenReconciliationDrifts();

        assertThat(openDrifts)
                .extracting(ReconciliationDriftResponse::channel)
                .doesNotContain(SalesChannel.SHOPEE);
    }

    private static final class ShopeeStubServer {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final Map<String, List<RecordedRequest>> requests = new ConcurrentHashMap<>();
        private final AtomicLong itemId = new AtomicLong(1000L);
        private volatile HttpServer server;
        private volatile String sku = BASE_SKU;
        private volatile int availableStock = 19;
        private volatile int reservedStock = 1;

        void start() {
            try {
                server = HttpServer.create(new InetSocketAddress(0), 0);
                server.createContext("/api/v2/product/get_item_list", this::handleGetItemList);
                server.createContext("/api/v2/product/get_item_base_info", this::handleGetItemBaseInfo);
                server.createContext("/api/v2/product/get_model_list", this::handleGetModelList);
                server.createContext("/api/v2/product/update_stock", this::handleUpdateStock);
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to start Shopee stub server", exception);
            }
        }

        void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        void reset(String sku, long itemId, int availableStock, int reservedStock) {
            this.requests.clear();
            this.sku = sku;
            this.itemId.set(itemId);
            this.availableStock = availableStock;
            this.reservedStock = reservedStock;
        }

        void setRemoteStock(int availableStock, int reservedStock) {
            this.availableStock = availableStock;
            this.reservedStock = reservedStock;
        }

        int countRequests(String path) {
            return requests.getOrDefault(path, List.of()).size();
        }

        String firstQuery(String path) {
            return requests.getOrDefault(path, List.of()).getFirst().uri().getRawQuery();
        }

        private void handleGetItemList(HttpExchange exchange) throws IOException {
            record(exchange);
            writeJson(exchange, """
                    {
                      "error": "",
                      "message": "",
                      "warning": "",
                      "request_id": "stub-get-item-list",
                      "response": {
                        "item": [
                          {
                            "item_id": %d,
                            "item_status": "NORMAL",
                            "update_time": 1760000000
                          }
                        ],
                        "total_count": 1,
                        "has_next_page": false,
                        "next_offset": 0
                      }
                    }
                    """.formatted(itemId.get()));
        }

        private void handleGetItemBaseInfo(HttpExchange exchange) throws IOException {
            record(exchange);
            writeJson(exchange, """
                    {
                      "error": "",
                      "message": "",
                      "warning": "",
                      "request_id": "stub-get-item-base-info",
                      "response": {
                        "item_list": [
                          {
                            "item_id": %d,
                            "item_sku": "%s",
                            "item_status": "NORMAL",
                            "has_model": false,
                            "stock_info_v2": {
                              "summary_info": {
                                "total_reserved_stock": %d,
                                "total_available_stock": %d
                              },
                              "seller_stock": [
                                {
                                  "location_id": "LOC-1",
                                  "stock": %d,
                                  "if_saleable": true
                                }
                              ],
                              "shopee_stock": []
                            }
                          }
                        ]
                      }
                    }
                    """.formatted(itemId.get(), sku, reservedStock, availableStock, availableStock));
        }

        private void handleGetModelList(HttpExchange exchange) throws IOException {
            record(exchange);
            writeJson(exchange, """
                    {
                      "error": "",
                      "message": "",
                      "warning": "",
                      "request_id": "stub-get-model-list",
                      "response": {
                        "tier_variation": [],
                        "model": []
                      }
                    }
                    """);
        }

        private void handleUpdateStock(HttpExchange exchange) throws IOException {
            String body = readBody(exchange.getRequestBody());
            record(exchange, body);
            JsonNode payload = objectMapper.readTree(body);
            JsonNode sellerStock = payload.path("stock_list").path(0).path("seller_stock").path(0).path("stock");
            if (!sellerStock.isMissingNode() && sellerStock.isInt()) {
                availableStock = sellerStock.intValue();
            }
            writeJson(exchange, """
                    {
                      "error": "",
                      "message": "",
                      "warning": "",
                      "request_id": "stub-update-stock",
                      "response": {
                        "failure_list": [],
                        "success_list": [
                          {
                            "model_id": 0,
                            "location_id": "LOC-1",
                            "stock": %d
                          }
                        ]
                      }
                    }
                    """.formatted(availableStock));
        }

        private void record(HttpExchange exchange) {
            record(exchange, "");
        }

        private void record(HttpExchange exchange, String body) {
            requests.computeIfAbsent(exchange.getRequestURI().getPath(), ignored -> new ArrayList<>())
                    .add(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI(), body));
        }

        private String readBody(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        private void writeJson(HttpExchange exchange, String json) throws IOException {
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(payload);
            } finally {
                exchange.close();
            }
        }
    }

    private record RecordedRequest(String method, URI uri, String body) {
    }
}
