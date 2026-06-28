package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.api.ConfirmReservationResponse;
import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.TikTokIngressReceiptResponse;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.channel.sync.ChannelSyncService;
import com.codex.flashsale.connector.tiktok.TikTokSigningSupport;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.scheduler.channel-sync-delay=1h",
        "app.scheduler.reconciliation-delay=1h",
        "app.scheduler.alert-delivery-delay=1h",
        "app.channel.retry-delay=0s",
        "app.channel.tik-tok.mode=real",
        "app.channel.tik-tok.app-key=tiktok-app-key",
        "app.channel.tik-tok.app-secret=tiktok-app-secret",
        "app.channel.tik-tok.shop-cipher=tiktok-shop-cipher",
        "app.channel.tik-tok.access-token=tiktok-access-token",
        "app.channel.tik-tok.ingress-secret=tiktok-ingress-secret",
        "app.channel.tik-tok.connect-timeout=1s",
        "app.channel.tik-tok.read-timeout=1s"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TikTokConnectorIntegrationTest extends AbstractIntegrationTest {

    private static final TikTokStubServer TIKTOK_STUB = new TikTokStubServer();

    static {
        TIKTOK_STUB.start();
    }

    @DynamicPropertySource
    static void tikTokProperties(DynamicPropertyRegistry registry) {
        registry.add("app.channel.tik-tok.base-url", TIKTOK_STUB::baseUrl);
    }

    @Autowired
    private ReservationApplicationService reservationApplicationService;

    @Autowired
    private ChannelSyncService channelSyncService;

    @Autowired
    private OpsApplicationService opsApplicationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resetDatabase(
                20,
                20,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );
        TIKTOK_STUB.reset(BASE_SKU, 19, 1);
    }

    @AfterAll
    static void tearDownStub() {
        TIKTOK_STUB.stop();
    }

    @Test
    void shouldSyncTikTokInventoryThroughRealConnectorInRealMode() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.TIKTOK_SHOP, 1),
                "tiktok-real-sync"
        );

        int publishedAttempts = channelSyncService.publishPendingAttempts();

        assertThat(publishedAttempts).isEqualTo(4);
        assertThat(TIKTOK_STUB.countRequests("POST", "/open_api/v1/inventory/sku/" + BASE_SKU)).isEqualTo(1);
        assertThat(TIKTOK_STUB.firstHeader("POST", "/open_api/v1/inventory/sku/" + BASE_SKU, "X-TTS-APP-KEY"))
                .isEqualTo("tiktok-app-key");
        assertThat(channelInventorySnapshotRepository.findByChannelAndSku(SalesChannel.TIKTOK_SHOP, BASE_SKU)).isPresent();
    }

    @Test
    void shouldUseLiveTikTokStockDuringReconciliationInRealMode() {
        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.TIKTOK_SHOP, 1),
                "tiktok-reconciliation-seed"
        );
        channelSyncService.publishPendingAttempts();

        reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "tiktok-reconciliation-drift"
        );

        TIKTOK_STUB.setRemoteStock(19, 1);

        var run = opsApplicationService.runReconciliation();
        var openDrifts = opsApplicationService.listOpenReconciliationDrifts();

        assertThat(run.openDriftCount()).isGreaterThanOrEqualTo(1);
        assertThat(openDrifts)
                .extracting(drift -> drift.channel())
                .contains(SalesChannel.TIKTOK_SHOP);
    }

    @Test
    void shouldAcceptSignedInventoryIngressAndDeduplicateReceipts() throws Exception {
        String rawBody = """
                {
                  "receiptId": "tt-inventory-1",
                  "sku": "%s",
                  "availableQty": 17,
                  "reservedQty": 2,
                  "soldQty": 1
                }
                """.formatted(BASE_SKU).trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = TikTokSigningSupport.hashRawBody("tiktok-ingress-secret", timestamp, rawBody);

        MvcResult first = mockMvc.perform(post("/api/v1/channel-ingress/tiktok/inventory")
                        .contentType(APPLICATION_JSON)
                        .header("X-TikTok-Timestamp", timestamp)
                        .header("X-TikTok-Signature", signature)
                        .content(rawBody))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/channel-ingress/tiktok/inventory")
                        .contentType(APPLICATION_JSON)
                        .header("X-TikTok-Timestamp", timestamp)
                        .header("X-TikTok-Signature", signature)
                        .content(rawBody))
                .andExpect(status().isOk())
                .andReturn();

        TikTokIngressReceiptResponse firstResponse = objectMapper.readValue(
                first.getResponse().getContentAsByteArray(),
                TikTokIngressReceiptResponse.class
        );
        TikTokIngressReceiptResponse secondResponse = objectMapper.readValue(
                second.getResponse().getContentAsByteArray(),
                TikTokIngressReceiptResponse.class
        );

        assertThat(firstResponse.outcome()).isEqualTo("PROCESSED");
        assertThat(secondResponse.outcome()).isEqualTo("DUPLICATE");
        assertThat(channelInventorySnapshotRepository.findByChannelAndSku(SalesChannel.TIKTOK_SHOP, BASE_SKU)).isPresent();
    }

    @Test
    void shouldApplySignedOrderStatusIngress() throws Exception {
        var reservation = reservationApplicationService.reserve(
                BASE_CAMPAIGN_ID,
                new CreateReservationRequest(BASE_SKU, SalesChannel.WEB, 1),
                "tiktok-order-reserve"
        );
        ConfirmReservationResponse confirmed = reservationApplicationService.confirm(
                reservation.reservationId(),
                "tiktok-order-confirm"
        );

        String rawBody = """
                {
                  "receiptId": "tt-order-1",
                  "orderId": "%s",
                  "status": "PAID"
                }
                """.formatted(confirmed.orderId()).trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = TikTokSigningSupport.hashRawBody("tiktok-ingress-secret", timestamp, rawBody);

        mockMvc.perform(post("/api/v1/channel-ingress/tiktok/orders/status")
                        .contentType(APPLICATION_JSON)
                        .header("X-TikTok-Timestamp", timestamp)
                        .header("X-TikTok-Signature", signature)
                        .content(rawBody))
                .andExpect(status().isOk());

        assertThat(orderHeaderRepository.findById(confirmed.orderId()))
                .get()
                .extracting(order -> order.getStatus())
                .isEqualTo(OrderStatus.PAID);
    }

    private static final class TikTokStubServer {

        private final Map<String, List<RecordedRequest>> requests = new ConcurrentHashMap<>();
        private volatile HttpServer server;
        private volatile String sku = BASE_SKU;
        private volatile int availableStock = 19;
        private volatile int reservedStock = 1;

        void start() {
            try {
                server = HttpServer.create(new InetSocketAddress(0), 0);
                server.createContext("/open_api/v1/inventory/sku/" + BASE_SKU, this::handleInventoryBySku);
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to start TikTok stub server", exception);
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

        void reset(String sku, int availableStock, int reservedStock) {
            this.requests.clear();
            this.sku = sku;
            this.availableStock = availableStock;
            this.reservedStock = reservedStock;
        }

        void setRemoteStock(int availableStock, int reservedStock) {
            this.availableStock = availableStock;
            this.reservedStock = reservedStock;
        }

        int countRequests(String method, String path) {
            return (int) requests.getOrDefault(method + ":" + path, List.of()).size();
        }

        String firstHeader(String method, String path, String headerName) {
            return requests.getOrDefault(method + ":" + path, List.of()).getFirst().headers().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }

        private void handleInventoryBySku(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String body = readBody(exchange.getRequestBody());
            record(exchange, body);
            if ("POST".equals(exchange.getRequestMethod()) && body.contains("\"available_qty\"")) {
                String digits = body.replaceAll(".*\"available_qty\"\\s*:\\s*(\\d+).*", "$1");
                availableStock = Integer.parseInt(digits);
            }
            writeJson(exchange, """
                    {
                      "code": 0,
                      "message": "ok",
                      "data": {
                        "sku": "%s",
                        "listing_id": "listing-001",
                        "warehouse_id": "warehouse-001",
                        "available_qty": %d,
                        "reserved_qty": %d
                      }
                    }
                    """.formatted(sku, availableStock, reservedStock));
        }

        private void record(HttpExchange exchange, String body) {
            Map<String, String> headers = new ConcurrentHashMap<>();
            exchange.getRequestHeaders().forEach((key, values) -> headers.put(key, values.getFirst()));
            requests.computeIfAbsent(exchange.getRequestMethod() + ":" + exchange.getRequestURI().getPath(), ignored -> new ArrayList<>())
                    .add(new RecordedRequest(headers, body));
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

    private record RecordedRequest(Map<String, String> headers, String body) {
    }
}
