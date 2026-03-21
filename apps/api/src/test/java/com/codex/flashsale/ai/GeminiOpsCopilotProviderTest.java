package com.codex.flashsale.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.codex.flashsale.config.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GeminiOpsCopilotProviderTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldParseJsonResponseFromGeminiEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/mock/v1beta/models/test-model:generateContent", this::handleSuccess);
        server.start();

        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getAi().setEnabled(true);
        applicationProperties.getAi().setRetryCount(0);
        applicationProperties.getAi().setMaxResponseChars(4000);
        applicationProperties.getAi().getGemini().setBaseUrl("http://localhost:%s/mock".formatted(server.getAddress().getPort()));
        applicationProperties.getAi().getGemini().setApiKey("test-key");
        applicationProperties.getAi().getGemini().setModel("test-model");

        GeminiOpsCopilotProvider provider = new GeminiOpsCopilotProvider(applicationProperties, new ObjectMapper());

        OpsCopilotProviderResult result = provider.analyze("summarize", "req-1");

        assertThat(result.requestId()).isEqualTo("req-1");
        assertThat(result.response().summary()).isEqualTo("Hotspots detected.");
        assertThat(result.response().prioritizedFindings()).hasSize(1);
        assertThat(result.response().recommendedActions()).hasSize(1);
    }

    private void handleSuccess(HttpExchange exchange) throws IOException {
        String response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\":\\"Hotspots detected.\\",\\"prioritizedFindings\\":[{\\"severity\\":\\"WARN\\",\\"title\\":\\"Backlog rising\\",\\"detail\\":\\"Retry backlog is above normal.\\",\\"sourceIds\\":[\\"alerts-current\\"]}],\\"recommendedActions\\":[{\\"label\\":\\"Open remediation\\",\\"href\\":\\"/ops/remediation\\",\\"rationale\\":\\"Review retry backlog.\\",\\"sourceIds\\":[\\"alerts-current\\"]}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
