package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.BenchmarkEvidenceDetailResponse;
import com.codex.flashsale.api.BenchmarkEvidenceEntryResponse;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.scheduler.channel-sync-delay=1h",
        "app.scheduler.alert-delivery-delay=1h"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminBenchmarkEvidenceIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void benchmarkProperties(DynamicPropertyRegistry registry) {
        registry.add("app.benchmark.evidence-root", () ->
                Path.of("..", "..", "testing", "k6", "evidence").toAbsolutePath().normalize().toString());
    }

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
    }

    @Test
    void shouldListLatestAndFetchBenchmarkEvidenceDetails() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");

        MvcResult listResult = mockMvc.perform(get("/api/v1/admin/ops/benchmarks/evidence")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<BenchmarkEvidenceEntryResponse> entries = objectMapper.readValue(
                listResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        assertThat(entries).isNotEmpty();
        BenchmarkEvidenceEntryResponse latest = entries.getFirst();

        MvcResult detailResult = mockMvc.perform(get("/api/v1/admin/ops/benchmarks/evidence/{runId}", latest.runId())
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        BenchmarkEvidenceDetailResponse detail = objectMapper.readValue(
                detailResult.getResponse().getContentAsByteArray(),
                BenchmarkEvidenceDetailResponse.class
        );

        MvcResult latestResult = mockMvc.perform(get("/api/v1/admin/ops/benchmarks/evidence/latest")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        BenchmarkEvidenceDetailResponse latestDetail = objectMapper.readValue(
                latestResult.getResponse().getContentAsByteArray(),
                BenchmarkEvidenceDetailResponse.class
        );

        assertThat(detail.entry().runId()).isEqualTo(latest.runId());
        assertThat(detail.report().path("suiteStatus").asText()).isNotBlank();
        assertThat(detail.summaryMarkdown() == null || detail.summaryMarkdown().contains("Suite Status:")).isTrue();
        assertThat(detail.suiteSummary().suiteStatus()).isEqualTo(detail.report().path("suiteStatus").asText());
        assertThat(detail.suiteSummary().baselineAvailable()).isFalse();
        assertThat(detail.suiteSummary().baselineNote()).isNotBlank();
        assertThat(detail.scenarioSummaries()).isNotEmpty();
        assertThat(detail.scenarioComparisons()).isEmpty();
        assertThat(latestDetail.entry().runId()).isEqualTo(latest.runId());
    }

    private AdminAuthResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminLoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), AdminAuthResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
