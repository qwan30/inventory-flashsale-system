package com.codex.flashsale.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OpsAlertSeverity;
import com.codex.flashsale.api.OpsAlertStatus;
import com.codex.flashsale.config.ApplicationProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebhookAlertDeliveryPublisherTest {

    private final LinkedBlockingQueue<String> requestBodies = new LinkedBlockingQueue<>();
    private final AtomicInteger responseStatus = new AtomicInteger(204);

    private HttpServer server;
    private String webhookUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/alerts", this::handleRequest);
        server.start();
        webhookUrl = "http://localhost:" + server.getAddress().getPort() + "/alerts";
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPostAlertPayloadToWebhook() throws Exception {
        WebhookAlertDeliveryPublisher publisher = new WebhookAlertDeliveryPublisher(properties());

        publisher.publish(alert(), AlertDispatchType.TRANSITION, Instant.parse("2026-03-15T09:05:00Z"));

        String body = requestBodies.poll(5, TimeUnit.SECONDS);
        assertThat(body).contains("\"source\":\"inventory-flashsale-api\"");
        assertThat(body).contains("\"code\":\"OUTBOX_FAILED_BACKLOG\"");
        assertThat(body).contains("\"dispatchType\":\"TRANSITION\"");
    }

    @Test
    void shouldThrowWhenWebhookRejectsRequest() {
        responseStatus.set(500);
        WebhookAlertDeliveryPublisher publisher = new WebhookAlertDeliveryPublisher(properties());

        assertThatThrownBy(() -> publisher.publish(
                alert(),
                AlertDispatchType.REMINDER,
                Instant.parse("2026-03-15T09:05:00Z")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rejected");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        requestBodies.offer(new String(requestBody, StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(responseStatus.get(), response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private ApplicationProperties properties() {
        ApplicationProperties properties = new ApplicationProperties();
        ApplicationProperties.AlertDelivery delivery = properties.getAlerts().getDelivery();
        delivery.setEnabled(true);
        delivery.setWebhookUrl(webhookUrl);
        delivery.setConnectTimeout(Duration.ofSeconds(1));
        delivery.setReadTimeout(Duration.ofSeconds(1));
        delivery.setReminderInterval(Duration.ofMinutes(15));
        return properties;
    }

    private OpsAlertResponse alert() {
        return new OpsAlertResponse(
                "OUTBOX_FAILED_BACKLOG",
                OpsAlertSeverity.WARN,
                OpsAlertStatus.ACTIVE,
                "Failed backlog breached threshold",
                "2",
                "1",
                Instant.parse("2026-03-15T09:00:00Z")
        );
    }
}
